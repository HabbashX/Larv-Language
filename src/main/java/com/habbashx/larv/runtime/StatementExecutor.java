package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.ast.statement.visitor.StatementVisitor;
import com.habbashx.larv.runtime.importer.LarvFileImporter;
import com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader;
import com.habbashx.larv.signal.BreakSignal;
import com.habbashx.larv.signal.ContinueSignal;
import com.habbashx.larv.signal.ReturnSignal;
import com.habbashx.larv.signal.ThrowSignal;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes every {@link Statement} node.
 *
 * <h2>New handlers</h2>
 * <ul>
 *   <li>{@link #visitTryCatch}  — try / catch / finally</li>
 *   <li>{@link #visitThrow}     — throw expr</li>
 *   <li>{@link #visitSwitch}    — switch / case / default</li>
 *   <li>{@link #visitEnum}      — enum Name { A, B, C }</li>
 * </ul>
 */
public class StatementExecutor implements StatementVisitor {

    private final ExecutionContext     context;
    private final ExpressionEvaluator  evaluator;
    private final LoopExecutor         loopExecutor;

    private final Map<Class<?>, StatementHandler<Statement>> registry;

    public StatementExecutor(ExecutionContext context, ExpressionEvaluator evaluator) {
        this.context      = context;
        this.evaluator    = evaluator;
        this.loopExecutor = new LoopExecutor(this::runBlock);
        this.registry     = buildRegistry();
    }

    private @NotNull Map<Class<?>, StatementHandler<Statement>> buildRegistry() {
        Map<Class<?>, StatementHandler<Statement>> map = new HashMap<>();

        map.put(LetStatement.class,              s -> visitLet((LetStatement) s));
        map.put(ConstStatement.class,            s -> visitConst((ConstStatement) s));
        map.put(AssignStatement.class,           s -> visitAssign((AssignStatement) s));
        map.put(CompoundAssignStatement.class,   s -> visitCompoundAssign((CompoundAssignStatement) s));
        map.put(ModuleStatement.class,           s -> visitModule((ModuleStatement) s));
        map.put(ExprStatement.class,             s -> visitExpr((ExprStatement) s));
        map.put(PrintStatement.class,            s -> visitPrint((PrintStatement) s));
        map.put(IfStatement.class,               s -> visitIf((IfStatement) s));
        map.put(WhileStatement.class,            s -> visitWhile((WhileStatement) s));
        map.put(ForStatement.class,              s -> visitFor((ForStatement) s));
        map.put(ForeachStatement.class,          s -> visitForeach((ForeachStatement) s));
        map.put(BreakStatement.class,            s -> visitBreak((BreakStatement) s));
        map.put(ContinueStatement.class,         s -> visitContinue((ContinueStatement) s));
        map.put(IncrementStatement.class,        s -> visitIncrement((IncrementStatement) s));
        map.put(DecrementStatement.class,        s -> visitDecrement((DecrementStatement) s));
        map.put(ReturnStatement.class,           s -> visitReturn((ReturnStatement) s));
        map.put(FunctionStatement.class,         s -> visitFunction((FunctionStatement) s));
        map.put(ClassStatement.class,            s -> visitClass((ClassStatement) s));
        map.put(JavaBindStatement.class,         s -> visitJavaBind((JavaBindStatement) s));
        map.put(ImportStatement.class,           s -> visitImport((ImportStatement) s));
        // ── new ───────────────────────────────────────────────────────────────
        map.put(TryCatchStatement.class,         s -> visitTryCatch((TryCatchStatement) s));
        map.put(ThrowStatement.class,            s -> visitThrow((ThrowStatement) s));
        map.put(SwitchStatement.class,           s -> visitSwitch((SwitchStatement) s));
        map.put(EnumStatement.class,             s -> visitEnum((EnumStatement) s));

        return map;
    }

    public void execute(@NotNull List<Statement> statements) {
        statements.forEach(this::execute);
    }

    public void execute(@NotNull Statement st) {
        StatementHandler<Statement> handler = registry.get(st.getClass());
        if (handler == null) {
            throw new LarvError("No handler registered for: " + st.getClass().getSimpleName(), st.line());
        }
        try {
            handler.handle(st);
        } catch (LarvError e) {
            if (e.getLine() < 0 && st.line() >= 0) {
                throw new LarvError(e.getMessage(), st.line(), e.getColumn(), e.getKind());
            }
            throw e;
        }
    }

    // ── Existing visitors (unchanged) ─────────────────────────────────────────

    @Override public void visitLet(@NotNull LetStatement st) {
        Object value = st.expression() == null ? null : evaluator.eval(st.expression());
        context.getEnvironment().define(st.name(), value);
    }

    @Override public void visitConst(@NotNull ConstStatement st) {
        context.getEnvironment().defineConst(st.name(), evaluator.eval(st.value()));
    }

    @Override public void visitAssign(@NotNull AssignStatement st) {
        context.getEnvironment().assign(st.name(), evaluator.eval(st.value()));
    }

    @Override public void visitCompoundAssign(@NotNull CompoundAssignStatement st) {
        Object current = context.getEnvironment().get(st.name());
        Object rhs     = evaluator.eval(st.value());
        Object result  = BinaryOperator.apply(st.operator(), current, rhs);
        context.getEnvironment().assign(st.name(), result);
    }

    /**
     * Executes a {@code for … in} loop over a Larv list.
     *
     * <p>Each element is bound to {@link ForeachStatement#variable()} in a fresh
     * inner scope.  {@link ContinueSignal} skips to the next element;
     * {@link BreakSignal} exits the loop entirely.</p>
     *
     * @param st the foreach statement node
     * @throws LarvError if the iterable expression does not evaluate to a list
     */
    @Override public void visitForeach(@NotNull ForeachStatement st) {
        Object iterable = evaluator.eval(st.iterable());
        if (!(iterable instanceof java.util.List<?> rawList))
            throw new LarvError("'for in' expects a list, got: " + iterable, st.line());
        @SuppressWarnings("unchecked")
        java.util.List<Object> list = (java.util.List<Object>) rawList;
        for (Object element : list) {
            Environment saved = context.getEnvironment();
            context.pushScope();
            try {
                context.getEnvironment().define(st.variable(), element);
                try {
                    st.body().forEach(this::execute);
                } catch (ContinueSignal ignored) {
                } catch (BreakSignal ignored) { break; }
            } finally {
                context.popScope(saved);
            }
        }
    }

    /**
     * Registers a module by collecting its function and constant declarations
     * into a {@link LarvObject} namespace and storing it in the environment.
     *
     * <p>Functions are registered globally under the qualified name
     * {@code "ModuleName.funcName"} so they survive scope changes.  The module
     * object itself is stored in the current scope under the module name.  A
     * special {@code "__module__"} field on the object holds the module name,
     * enabling method dispatch in {@link ExpressionEvaluator#callMethod}.</p>
     *
     * @param st the module statement node
     */
    @Override
    public void visitModule(@NotNull ModuleStatement st) {
        LarvObject moduleObj = new LarvObject();
        for (Statement member : st.body()) {
            if (member instanceof FunctionStatement fn) {
                context.defineFunction(st.name() + "." + fn.name(), fn);
                moduleObj.set(fn.name(), st.name() + "." + fn.name());
            } else if (member instanceof ConstStatement cs) {
                moduleObj.set(cs.name(), evaluator.eval(cs.value()));
            } else if (member instanceof LetStatement ls) {
                Object val = ls.expression() == null ? null : evaluator.eval(ls.expression());
                moduleObj.set(ls.name(), val);
            }
        }
        moduleObj.set("__module__", st.name());
        context.getEnvironment().define(st.name(), moduleObj);
    }

    @Override public void visitExpr(@NotNull ExprStatement st)  { evaluator.eval(st.value()); }
    @Override public void visitPrint(@NotNull PrintStatement st) { System.out.println(evaluator.eval(st.value())); }

    @Override public void visitIf(@NotNull IfStatement st) {
        List<Statement> branch = TruthinessEvaluator.isTruthy(evaluator.eval(st.condition()))
                ? st.thenBranch() : st.elseBranch();
        runBlock(branch);
    }

    @Override public void visitWhile(@NotNull WhileStatement st) {
        loopExecutor.runWhile(
                () -> TruthinessEvaluator.isTruthy(evaluator.eval(st.condition())),
                st.body());
    }

    @Override public void visitFor(@NotNull ForStatement st) {
        execute(st.init());
        loopExecutor.runFor(
                () -> TruthinessEvaluator.isTruthy(evaluator.eval(st.condition())),
                st.body(),
                () -> execute(st.increment()));
    }

    @Override public void visitBreak(BreakStatement st)         { throw new BreakSignal(); }
    @Override public void visitContinue(ContinueStatement st)   { throw new ContinueSignal(); }

    @Override public void visitIncrement(@NotNull IncrementStatement st) {
        Object current = context.getEnvironment().get(st.name());
        if (!(current instanceof Double d))
            throw new LarvError("'++' requires a number variable, got: " + current, st.line());
        context.getEnvironment().assign(st.name(), d + 1);
    }

    @Override public void visitDecrement(@NotNull DecrementStatement st) {
        Object current = context.getEnvironment().get(st.name());
        if (!(current instanceof Double d))
            throw new LarvError("'--' requires a number variable, got: " + current, st.line());
        context.getEnvironment().assign(st.name(), d - 1);
    }

    @Override public void visitReturn(@NotNull ReturnStatement st) { throw new ReturnSignal(evaluator.eval(st.value())); }
    @Override public void visitFunction(FunctionStatement st)      { context.defineFunction(st.name(), st); }
    @Override public void visitClass(ClassStatement st)            { context.defineClass(st.name(), st); }

    @Override public void visitJavaBind(@NotNull JavaBindStatement st) {
        if (st.hasInvolve()) context.getJavaRegistry().bindInstance(st.alias(), st.className(), st.constructorArgs());
        else                 context.getJavaRegistry().bind(st.alias(), st.className());
    }

    public void visitImport(@NotNull ImportStatement st) {
        if (st.isFileImport()) new LarvFileImporter(context, context.getProjectRoot()).importFile(st.path());
        else                   new NativeLibraryLoader(context).load(st.library());
    }


    /**
     * Executes a try/catch/finally block.
     *
     * <p>Catches both {@link ThrowSignal} (user {@code throw} statements) and
     * {@link LarvError} (runtime errors), so every error is catchable from
     * Larv code.  The {@code finally} block always runs, even if an uncaught
     * error is re-thrown.</p>
     */
    @Override
    public void visitTryCatch(@NotNull TryCatchStatement st) {
        Throwable caughtException = null;

        try {
            runBlock(st.tryBody());
        } catch (ThrowSignal signal) {
            caughtException = signal;
            if (!st.catchBody().isEmpty() && st.catchVar() != null) {
                runCatchBlock(st.catchVar(), signal.value, st.catchBody());
                caughtException = null;
            }
        } catch (LarvError error) {
            caughtException = error;
            if (!st.catchBody().isEmpty() && st.catchVar() != null) {
                runCatchBlock(st.catchVar(), error.getMessage(), st.catchBody());
                caughtException = null;
            }
        } finally {
            if (!st.finallyBody().isEmpty()) {
                runBlock(st.finallyBody());
            }
        }

        if (caughtException instanceof ThrowSignal ts) throw ts;
        if (caughtException instanceof LarvError   le) throw le;
    }

    private void runCatchBlock(String varName, Object value, @NotNull List<Statement> body) {
        Environment saved = context.getEnvironment();
        context.pushScope();
        try {
            context.getEnvironment().define(varName, value);
            body.forEach(this::execute);
        } finally {
            context.popScope(saved);
        }
    }


    @Override
    public void visitThrow(@NotNull ThrowStatement st) {
        Object value = evaluator.eval(st.value());
        throw new ThrowSignal(value);
    }

    /**
     * Evaluates the subject expression once, then walks the case list.
     * Uses {@link Objects#equals} for equality so strings, numbers, and booleans
     * all compare correctly.  The first matching arm wins (no fall-through).
     * {@code break} inside a case exits the switch just like a loop.
     */
    @Override
    public void visitSwitch(@NotNull SwitchStatement st) {
        Object subject = evaluator.eval(st.subject());
        boolean matched = false;

        for (SwitchStatement.SwitchCase arm : st.cases()) {
            for (var valueExpr : arm.values()) {
                Object caseVal = evaluator.eval(valueExpr);
                if (Objects.equals(subject, caseVal)) {
                    try {
                        runBlock(arm.body());
                    } catch (BreakSignal ignored) {}
                    matched = true;
                    break;
                }
            }
            if (matched) break;
        }

        if (!matched && !st.defaultBody().isEmpty()) {
            try {
                runBlock(st.defaultBody());
            } catch (BreakSignal ignored) {}
        }
    }

    /**
     * Registers an enum as a {@link LarvObject} whose fields are the ordinal
     * values (0, 1, 2, …) of each variant, plus a {@code "__enum__"} marker.
     *
     * <pre>
     *   enum Color { RED, GREEN, BLUE }
     *   // Color.RED == 0, Color.GREEN == 1, Color.BLUE == 2
     * </pre>
     */
    @Override
    public void visitEnum(@NotNull EnumStatement st) {
        LarvObject enumObj = new LarvObject();
        enumObj.set("__enum__", st.name());
        int ordinal = 0;
        for (String variant : st.variants()) {
            enumObj.set(variant, (double) ordinal++);
        }
        context.getEnvironment().define(st.name(), enumObj);
    }

    public void runBlock(@NotNull List<Statement> stmts) {
        Environment saved = context.getEnvironment();
        context.pushScope();
        try {
            stmts.forEach(this::execute);
        } finally {
            context.popScope(saved);
        }
    }
}
