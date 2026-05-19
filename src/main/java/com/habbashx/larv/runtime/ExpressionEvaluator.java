package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.expression.visitor.ExpressionVisitor;
import com.habbashx.larv.parser.ast.statement.FunctionStatement;
import com.habbashx.larv.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree-walking evaluator for all {@link Expression} AST nodes.
 *
 * <p>{@code ExpressionEvaluator} implements {@link ExpressionVisitor} and is
 * the single place where expression nodes are turned into runtime values.
 * It is constructed once per interpreter run and reused for every expression
 * in the program.</p>
 *
 * <h2>Dispatch</h2>
 * <p>{@link #eval(Expression)} uses an exhaustive {@code switch} expression
 * over the sealed {@link Expression} hierarchy.  Each arm delegates to a
 * private {@code visit*} method.  No {@code instanceof} chain or external
 * visitor registration is needed.</p>
 *
 * <h2>Value representation</h2>
 * <table border="1">
 *   <tr><th>Larv type</th><th>Java representation</th></tr>
 *   <tr><td>number</td>   <td>{@link Double}</td></tr>
 *   <tr><td>string</td>   <td>{@link String}</td></tr>
 *   <tr><td>boolean</td>  <td>{@link Boolean}</td></tr>
 *   <tr><td>nil</td>      <td>{@code null}</td></tr>
 *   <tr><td>array</td>    <td>{@code java.util.ArrayList<Object>}</td></tr>
 *   <tr><td>object</td>   <td>{@link LarvObject}</td></tr>
 * </table>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>{@link ExecutionContext} — resolves variables, functions, classes, natives.</li>
 *   <li>{@link FunctionInvoker} — handles function and method call mechanics.</li>
 *   <li>{@link BinaryOperator}  — applies arithmetic and comparison operators.</li>
 *   <li>{@link TruthinessEvaluator} — converts values to boolean for conditions.</li>
 *   <li>{@link ArrayMethods}    — dispatches built-in array dot methods.</li>
 * </ul>
 */
public class ExpressionEvaluator implements ExpressionVisitor {

    private final ExecutionContext context;
    private final FunctionInvoker  invoker;

    /**
     * Creates an evaluator wired to the given context and function invoker.
     *
     * @param context the shared runtime state
     * @param invoker the helper used to invoke user-defined functions and methods
     */
    public ExpressionEvaluator(ExecutionContext context, FunctionInvoker invoker) {
        this.context = context;
        this.invoker = invoker;
    }

    /**
     * Evaluates any expression node and returns its runtime value.
     *
     * <p>This is the main entry point.  All recursive evaluation inside visit
     * methods calls back here.</p>
     *
     * @param expr the expression to evaluate (must not be {@code null})
     * @return the runtime value produced by the expression
     * @throws LarvError if evaluation fails (type error, undefined variable, etc.)
     */
    public Object eval(@NotNull Expression expr) {
        return switch (expr) {
            case NumberExpression  e -> visitNumber(e);
            case StringExpression  e -> visitString(e);
            case LiteralExpression e -> e.value();
            case BooleanExpression e -> e.value();
            case VarExpression     e -> visitVar(e);
            case UnaryExpression   e -> visitUnary(e);
            case BinaryExpression  e -> visitBinary(e);
            case LogicalExpression e -> visitLogical(e);
            case TernaryExpression e -> visitTernary(e);
            case CallExpression    e -> visitCall(e);
            case NewExpression     e -> visitNew(e);
            case ThisExpression    e -> visitThis(e);
            case GetExpression     e -> visitGet(e);
            case SetExpression     e -> visitSet(e);
            case JavaCallExpression e -> visitJavaCall(e);
            case ArrayExpression   e -> visitArray(e);
            case IndexExpression   e -> visitIndex(e);
            case AssignExpression  e -> visitAssignExpr(e);
            default -> throw new LarvError("Unknown expression type: " + expr.getClass().getSimpleName());
        };
    }

    @Override public Object visitNumber(@NotNull NumberExpression e) { return e.value(); }
    @Override public Object visitString(@NotNull StringExpression e) { return e.value(); }

    /**
     * Resolves a variable name in the current scope chain.
     *
     * @param e the variable reference node
     * @return the value bound to the variable's name
     * @throws LarvError if the variable is not declared
     */
    @Override public Object visitVar(@NotNull VarExpression e) { return context.getEnvironment().get(e.name()); }

    /**
     * Evaluates both operands eagerly and applies the binary operator.
     *
     * @param e the binary expression node
     * @return the operation result
     */
    @Override
    public Object visitBinary(@NotNull BinaryExpression e) {
        Object left  = eval(e.left());
        Object right = eval(e.right());
        return BinaryOperator.apply(e.operator(), left, right);
    }

    /**
     * Evaluates a unary prefix expression ({@code -expr} or {@code !expr}).
     *
     * @param e the unary expression node
     * @return negated number for {@code -}, inverted boolean for {@code !}
     * @throws LarvError if the operand has the wrong type
     */
    private Object visitUnary(@NotNull UnaryExpression e) {
        Object operand = eval(e.right());
        return switch (e.operator()) {
            case "-" -> {
                if (operand instanceof Double d)  yield -d;
                if (operand instanceof Integer i) yield (double) -i;
                throw new LarvError("Unary '-' requires a number, got: " + typeName(operand));
            }
            case "!" -> !TruthinessEvaluator.isTruthy(operand);
            default -> throw new LarvError("Unknown unary operator: '" + e.operator() + "'");
        };
    }

    /**
     * Short-circuit logical AND / OR.
     *
     * <ul>
     *   <li>{@code ||} — returns left immediately if truthy; otherwise evaluates right.</li>
     *   <li>{@code &&} — returns left immediately if falsy; otherwise evaluates right.</li>
     * </ul>
     *
     * @param e the logical expression node
     * @return the short-circuit result
     */
    private Object visitLogical(@NotNull LogicalExpression e) {
        Object left = eval(e.left());
        if (e.operator().equals("||")) {
            if (TruthinessEvaluator.isTruthy(left)) return left;
        } else {
            if (!TruthinessEvaluator.isTruthy(left)) return left;
        }
        return eval(e.right());
    }

    /**
     * Evaluates the ternary expression {@code condition ? thenExpr, elseExpr}.
     * Only the chosen branch is evaluated.
     *
     * @param e the ternary expression node
     * @return the value of the chosen branch
     */
    private Object visitTernary(@NotNull TernaryExpression e) {
        return TruthinessEvaluator.isTruthy(eval(e.condition()))
                ? eval(e.thenBranch())
                : eval(e.elseBranch());
    }

    /**
     * Dispatches a function or method call.
     *
     * <ul>
     *   <li>{@link VarExpression} caller → plain function call via {@link #callFunction}.</li>
     *   <li>{@link GetExpression} caller → method call via {@link #callMethod}.</li>
     * </ul>
     *
     * @param e the call expression node
     * @return the call's return value
     * @throws LarvError for invalid call targets
     */
    @Override
    public Object visitCall(@NotNull CallExpression e) {
        List<Object> args = evalAll(e.arguments());
        return switch (e.caller()) {
            case VarExpression(String name)              -> callFunction(name, args);
            case GetExpression(Expression obj, String m) -> callMethod(obj, m, args);
            default -> throw new LarvError("Invalid call target — expected a function or method");
        };
    }

    /**
     * Constructs a new instance of a user-defined class.
     *
     * <p>Steps:
     * <ol>
     *   <li>Looks up the class in the registry.</li>
     *   <li>Creates a fresh {@link LarvObject}.</li>
     *   <li>Collects method declarations from the class body onto {@code __methods__}.</li>
     *   <li>Pushes a new scope, binds {@code this}, and calls {@code init} if present.</li>
     * </ol>
     *
     * @param e the new-expression node
     * @return the newly created and initialized {@link LarvObject}
     * @throws LarvError if the class is not defined
     */
    @Override
    public Object visitNew(@NotNull NewExpression e) {
        String className = e.className();
        var clazz = context.getClass(className);
        if (clazz == null) throw new LarvError("Undefined class '" + className + "'");

        LarvObject obj     = new LarvObject();
        Map<String, FunctionStatement> methods = collectMethods(clazz.body());
        obj.set("__methods__", methods);

        Environment saved = context.getEnvironment();
        context.pushScope();
        try {
            context.getEnvironment().define("this", obj);
            FunctionStatement init = methods.get("init");
            if (init != null) invoker.invokeMethod(init, obj, evalAll(e.args()));
            return obj;
        } finally {
            context.popScope(saved);
        }
    }

    @Override public Object visitThis(ThisExpression e)            { return context.getEnvironment().get("this"); }
    @Override public Object visitGet(@NotNull GetExpression e)     { return requireObject(eval(e.object()), "field access '" + e.field() + "'").get(e.field()); }
    @Override public Object visitSet(@NotNull SetExpression e)     { requireObject(eval(e.object()), "field assignment '" + e.field() + "'").set(e.field(), eval(e.value())); return null; }

    /**
     * Resolves and calls a named function.
     * Checks native registry first, then user-defined functions.
     *
     * @param name the function name
     * @param args evaluated argument list
     * @return the call's return value
     * @throws LarvError if the function is not found or arity does not match
     */
    private Object callFunction(String name, List<Object> args) {
        if (context.hasNative(name)) return context.invokeNative(name, args);
        FunctionStatement fn = context.getFunction(name);
        if (fn == null) throw new LarvError("Undefined function '" + name + "' — did you define it with 'func'?");
        if (fn.params().size() != args.size())
            throw new LarvError("Function '" + name + "' expects " + fn.params().size() +
                    " argument(s) but got " + args.size());
        return invoker.invokeFunction(fn, args);
    }

    /**
     * Dispatches a method call on an object, module, Java alias, or array.
     *
     * <p>Priority:
     * <ol>
     *   <li>Java FFI alias ({@code JMath.sqrt(4)})</li>
     *   <li>Module namespace ({@code Math.add(2,3)})</li>
     *   <li>Built-in array dot method ({@code arr.push(x)})</li>
     *   <li>User-defined object method ({@code obj.greet()})</li>
     * </ol>
     *
     * @param objExpr    the receiver expression
     * @param methodName the method name to dispatch
     * @param args       evaluated argument list
     * @return the method's return value
     */
    private Object callMethod(Expression objExpr, String methodName, List<Object> args) {
        if (objExpr instanceof VarExpression(String name) && context.getJavaRegistry().hasAlias(name))
            return context.getJavaRegistry().invoke(name, methodName, args);

        Object target = eval(objExpr);

        if (target instanceof LarvObject obj && obj.get("__module__") != null) {
            String moduleName = (String) obj.get("__module__");
            String qualifiedName = moduleName + "." + methodName;
            if (context.hasNative(qualifiedName)) return context.invokeNative(qualifiedName, args);
            FunctionStatement fn = context.getFunction(qualifiedName);
            if (fn == null) throw new LarvError("Module '" + moduleName + "' has no function '" + methodName + "'");
            if (fn.params().size() != args.size())
                throw new LarvError("'" + moduleName + "." + methodName + "' expects " + fn.params().size() + " arg(s) but got " + args.size());
            return invoker.invokeFunction(fn, args);
        }

        if (target instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) list;
            return ArrayMethods.dispatch(array, methodName, args);
        }

        LarvObject obj = requireObject(target, "method call '" + methodName + "'");
        FunctionStatement fn = getMethods(obj).get(methodName);
        if (fn == null) throw new LarvError("Undefined method '" + methodName + "' on object");
        return invoker.invokeMethod(fn, obj, args);
    }

    /**
     * Evaluates a list of expressions left-to-right and collects the results.
     *
     * @param exprs the expression list to evaluate
     * @return a new list of evaluated values, in the same order
     */
    private @NotNull List<Object> evalAll(@NotNull List<Expression> exprs) {
        List<Object> values = new ArrayList<>(exprs.size());
        for (Expression e : exprs) values.add(eval(e));
        return values;
    }

    /**
     * Scans a class body for {@link FunctionStatement} nodes and returns a
     * map from method name to its declaration.
     *
     * @param body the class body statement list
     * @return map of method name → {@link FunctionStatement}
     */
    private @NotNull Map<String, FunctionStatement> collectMethods(@NotNull List<Statement> body) {
        Map<String, FunctionStatement> methods = new HashMap<>();
        for (Statement s : body) if (s instanceof FunctionStatement fn) methods.put(fn.name(), fn);
        return methods;
    }

    /**
     * Asserts that {@code value} is a {@link LarvObject} and returns it.
     *
     * @param value     the runtime value to check
     * @param operation a description of the attempted operation (for the error message)
     * @return the value cast to {@link LarvObject}
     * @throws LarvError if the value is not a {@link LarvObject}
     */
    private LarvObject requireObject(Object value, String operation) {
        if (value instanceof LarvObject lo) return lo;
        if (value == null) throw new LarvError("Attempted " + operation + " on 'nil' — the value is not an object");
        throw new LarvError("Attempted " + operation + " on a " + typeName(value) + " — expected an object");
    }

    /** Returns the method map stored on a {@link LarvObject} under {@code __methods__}. */
    @SuppressWarnings("unchecked")
    private Map<String, FunctionStatement> getMethods(@NotNull LarvObject obj) {
        return (Map<String, FunctionStatement>) obj.get("__methods__");
    }

    /**
     * Invokes a Java FFI method via the {@link com.habbashx.larv.runtime.ffi.JavaClassRegistry}.
     *
     * @param e the Java call expression node
     * @return the return value from the Java method
     */
    private Object visitJavaCall(@NotNull JavaCallExpression e) {
        List<Object> args = evalAll(e.arguments());
        return context.getJavaRegistry().invoke(e.alias(), e.methodName(), args);
    }

    /**
     * Evaluates an array literal by evaluating each element expression in order.
     *
     * @param e the array literal expression node
     * @return a new {@code ArrayList} containing the evaluated elements
     */
    private @NotNull Object visitArray(@NotNull ArrayExpression e) {
        List<Object> list = new ArrayList<>();
        for (Expression el : e.elements()) list.add(eval(el));
        return list;
    }

    /**
     * Evaluates an array index access ({@code arr[i]}).
     *
     * @param e the index expression node
     * @return the element at the given index
     * @throws LarvError if the target is not an array, the index is not numeric,
     *                   or the index is out of bounds
     */
    private Object visitIndex(@NotNull IndexExpression e) {
        Object target = eval(e.array());
        Object idx    = eval(e.index());
        if (!(idx instanceof Double))
            throw new LarvError("Array index must be a number, got: " + typeName(idx));
        if (target instanceof List<?> list) {
            int i = ((Double) idx).intValue();
            if (i < 0 || i >= list.size())
                throw new LarvError("Index " + i + " is out of bounds — array has " + list.size() + " element(s)");
            return list.get(i);
        }
        if (target == null) throw new LarvError("Cannot index into 'nil'");
        throw new LarvError("Cannot index into " + typeName(target) + " — expected an array");
    }

    /**
     * Evaluates an assignment expression ({@code x = expr}) and returns the
     * assigned value so the expression can be used inline.
     *
     * @param e the assign expression node
     * @return the value that was assigned
     */
    private Object visitAssignExpr(@NotNull AssignExpression e) {
        Object value = eval(e.value());
        context.getEnvironment().assign(e.name(), value);
        return value;
    }

    /**
     * Returns a human-readable Larv type name for a runtime value.
     *
     * @param v the value (may be {@code null})
     * @return one of: {@code "nil"}, {@code "number"}, {@code "string"},
     *         {@code "boolean"}, {@code "array"}, {@code "object"}, or the
     *         Java class simple name as a fallback
     */
    private @NotNull String typeName(Object v) {
        if (v == null)               return "nil";
        if (v instanceof Double)     return "number";
        if (v instanceof String)     return "string";
        if (v instanceof Boolean)    return "boolean";
        if (v instanceof List)       return "array";
        if (v instanceof LarvObject) return "object";
        return v.getClass().getSimpleName();
    }
}