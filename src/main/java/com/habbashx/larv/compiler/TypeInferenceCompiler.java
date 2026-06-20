package com.habbashx.larv.compiler;

import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.statement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all static type-inference logic for the Larv compiler.
 *
 * <p>This includes evaluating the Larv type of any {@link Expression} given the
 * current scope, inferring the return type of a function body, and resolving
 * types using an isolated scope (used when analysing a callee method whose body
 * is not currently being compiled).</p>
 *
 * <p>None of the methods here emit bytecode; they only read shared state.</p>
 */
public abstract class TypeInferenceCompiler extends AbstractLarvCompiler {

    protected TypeInferenceCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    /**
     * Returns the Larv type string (e.g. {@code "int"}, {@code "string"},
     * {@code "User"}, {@code "any"}) for the given expression using the
     * compiler's live local-variable type maps.
     */
    @Contract("null -> !null")
    public String evaluateExpressionType(Expression expr) {
        if (expr == null) return "any";
        String result = switch (expr) {
            case NumberExpression e  -> e.value() % 1 == 0 ? "int" : "double";
            case StringExpression e  -> "string";
            case BooleanExpression e -> "bool";
            case LiteralExpression e -> {
                Object val = e.value();
                if (val instanceof Double)  yield "double";
                if (val instanceof Boolean) yield "bool";
                if (val instanceof String)  yield "string";
                yield "any";
            }
            case VarExpression e -> {
                String local = getLocalType(e.name());
                if (local != null) yield local;
                String flatType = localVarTypes.get(e.name());
                if (flatType != null && !flatType.equals("any")) yield flatType;
                if (currentClassStatement != null && hasField(currentClassStatement, e.name())) {
                    yield getFieldType(currentClassStatement, e.name());
                }
                yield "any";
            }
            case BinaryExpression e -> {
                if (List.of("==", "!=", "<", "<=", ">", ">=").contains(e.operator())) yield "bool";
                yield "double";
            }
            case UnaryExpression e   -> e.operator().equals("!") ? "bool" : "double";
            case LogicalExpression e -> "bool";
            case ArrayExpression e   -> "List";
            case NewExpression e     -> e.className();
            case CallExpression e -> {
                // Recognise built-in collection constructors: Map(...), List(...), Set(...)
                if (e.caller() instanceof VarExpression(String calleeName)) {
                    if ("Map".equals(calleeName))  yield "Map";
                    if ("List".equals(calleeName)) yield "List";
                    if ("Set".equals(calleeName))  yield "Set";
                }
                if (e.caller() instanceof GetExpression(Expression object, String field)) {
                    String receiverType = evaluateExpressionType(object);
                    if (larvClasses.containsKey(receiverType)) {
                        ClassStatement cs = larvClasses.get(receiverType);
                        FunctionStatement method = cs.body().stream()
                                .filter(s -> s instanceof FunctionStatement fs && fs.name().equals(field))
                                .map(s -> (FunctionStatement) s)
                                .findFirst().orElse(null);
                        if (method != null) {
                            String ret = method.returnType();
                            if (ret != null && !ret.equals("void") && !ret.equals("any")) yield ret;
                            if (containsReturnWithValue(method.body())) {
                                Map<String, String> paramScope = new HashMap<>();
                                for (FunctionStatement.Parameter p : method.params()) {
                                    paramScope.put(p.name(), p.type() != null ? p.type() : "any");
                                }
                                String inferred = inferReturnTypeWithScope(method.body(), paramScope);
                                if (inferred != null && !inferred.equals("any")) yield inferred;
                            }
                        }
                    }
                }
                yield "any";
            }
            default -> "any";
        };
        debugLog("    typeInfer  expr=" + expr.getClass().getSimpleName() + "  → " + result);
        return result;
    }

    /**
     * Scans a method body for the first {@code return <expr>} statement and
     * infers the type of the returned expression using an isolated local-variable
     * scope built from the method's own {@code VarStatement}s and parameters.
     *
     * <p>We MUST NOT use the compiler's live {@link #localVarTypes} here because
     * that map belongs to the currently-compiling method, not the method being
     * analysed.</p>
     */
    protected @Nullable String inferReturnType(@NotNull List<Statement> stmts) {
        return inferReturnTypeWithScope(stmts, new HashMap<>());
    }

    protected @Nullable String inferReturnTypeWithScope(
            @NotNull List<Statement> stmts,
            @NotNull Map<String, String> scope) {
        for (Statement s : stmts) {
            if (s instanceof VarStatement vs && vs.expression() != null) {
                String t = vs.type() != null && !vs.type().equals("any")
                        ? vs.type()
                        : resolveTypeInScope(vs.expression(), scope);
                scope.put(vs.name(), t);
            }
            if (s instanceof ReturnStatement rs && rs.value() != null) {
                return resolveTypeInScope(rs.value(), scope);
            }
            if (s instanceof BlockStatement bs) {
                String t = inferReturnTypeWithScope(bs.statements(), new HashMap<>(scope));
                if (t != null) return t;
            }
            if (s instanceof IfStatement is) {
                String t = inferReturnTypeWithScope(is.thenBranch(), new HashMap<>(scope));
                if (t != null) return t;
                if (is.elseBranch() != null) {
                    t = inferReturnTypeWithScope(is.elseBranch(), new HashMap<>(scope));
                    if (t != null) return t;
                }
            }
        }
        return null;
    }

    /**
     * Resolves the type of {@code expr} using only {@code scope} (a local-variable
     * type map for the method being analysed) plus the global {@link #larvClasses}.
     * Does NOT touch the live {@link #localVarTypes}.
     */
    protected String resolveTypeInScope(@NotNull Expression expr, @NotNull Map<String, String> scope) {
        return switch (expr) {
            case NumberExpression e  -> e.value() % 1 == 0 ? "int" : "double";
            case StringExpression e  -> "string";
            case BooleanExpression e -> "bool";
            case NewExpression e     -> e.className();
            case ArrayExpression e   -> "List";
            case VarExpression e     -> scope.getOrDefault(e.name(), "any");
            case CallExpression e -> {
                // Recognise built-in collection constructors: Map(...), List(...), Set(...)
                if (e.caller() instanceof VarExpression(String calleeName)) {
                    if ("Map".equals(calleeName))  yield "Map";
                    if ("List".equals(calleeName)) yield "List";
                    if ("Set".equals(calleeName))  yield "Set";
                }
                if (e.caller() instanceof GetExpression(Expression object, String field)) {
                    String recv = resolveTypeInScope(object, scope);
                    if (larvClasses.containsKey(recv)) {
                        FunctionStatement m = larvClasses.get(recv).body().stream()
                                .filter(st -> st instanceof FunctionStatement fs && fs.name().equals(field))
                                .map(st -> (FunctionStatement) st)
                                .findFirst().orElse(null);
                        if (m != null) {
                            String ret = m.returnType();
                            if (ret != null && !ret.equals("void") && !ret.equals("any")) yield ret;
                        }
                    }
                }
                yield "any";
            }
            default -> "any";
        };
    }

    // -----------------------------------------------------------------------
    // Return analysis helpers
    // -----------------------------------------------------------------------

    protected boolean containsReturn(@NotNull List<Statement> stmts) {
        for (Statement st : stmts) {
            if (st instanceof ReturnStatement) return true;
            if (st instanceof BlockStatement bs   && containsReturn(bs.statements()))  return true;
            if (st instanceof IfStatement is) {
                if (containsReturn(is.thenBranch())) return true;
                if (containsReturn(is.elseBranch())) return true;
            }
            if (st instanceof WhileStatement ws   && containsReturn(ws.body()))        return true;
            if (st instanceof ForStatement fs     && containsReturn(fs.body()))        return true;
            if (st instanceof ForeachStatement fs && containsReturn(fs.body()))        return true;
            if (st instanceof SwitchStatement ss) {
                for (var c : ss.cases()) if (containsReturn(c.body())) return true;
                if (containsReturn(ss.defaultBody())) return true;
            }
        }
        return false;
    }

    protected boolean containsReturnWithValue(@NotNull List<Statement> stmts) {
        for (Statement st : stmts) {
            if (st instanceof ReturnStatement r && r.value() != null) return true;
            if (st instanceof BlockStatement b    && containsReturnWithValue(b.statements()))  return true;
            if (st instanceof IfStatement i) {
                if (containsReturnWithValue(i.thenBranch())) return true;
                if (containsReturnWithValue(i.elseBranch())) return true;
            }
            if (st instanceof WhileStatement w    && containsReturnWithValue(w.body()))        return true;
            if (st instanceof ForStatement f      && containsReturnWithValue(f.body()))        return true;
            if (st instanceof ForeachStatement f  && containsReturnWithValue(f.body()))        return true;
            if (st instanceof SwitchStatement s) {
                for (var c : s.cases()) if (containsReturnWithValue(c.body())) return true;
                if (containsReturnWithValue(s.defaultBody())) return true;
            }
        }
        return false;
    }
}