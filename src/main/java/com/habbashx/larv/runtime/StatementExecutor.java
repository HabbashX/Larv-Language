package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.ast.statement.visitor.StatementVisitor;
import com.habbashx.larv.runtime.importer.LarvFileImporter;
import com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader;
import com.habbashx.larv.signal.BreakSignal;
import com.habbashx.larv.signal.ContinueSignal;
import com.habbashx.larv.signal.ReturnSignal;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes every {@link Statement} node using a <b>command registry</b> pattern.
 *
 * <p>Instead of a {@code switch} that grows with every new statement type,
 * {@link #execute(Statement)} does a single {@link Map} lookup keyed on the
 * statement's runtime class. Each entry is a {@link StatementHandler} —
 * a method reference that owns one statement form's full execution logic.</p>
 *
 * <p>Adding a new statement type requires exactly two steps:
 * <ol>
 *   <li>Add a {@code visitXxx} method implementing {@link StatementVisitor}.</li>
 *   <li>Register it in {@link #buildRegistry()}.</li>
 * </ol>
 * {@link #execute(Statement)} itself never needs to change.</p>
 *
 * <p>Loop body execution and break/continue signal handling are fully delegated
 * to {@link LoopExecutor}, keeping visit methods signal-free and linear.</p>
 */
public class StatementExecutor implements StatementVisitor {

    private final ExecutionContext    context;
    private final ExpressionEvaluator evaluator;
    private final LoopExecutor        loopExecutor;

    /** Maps each concrete statement class to its dedicated handler. */
    private final Map<Class<?>, StatementHandler<Statement>> registry;

    public StatementExecutor(ExecutionContext context, ExpressionEvaluator evaluator) {
        this.context      = context;
        this.evaluator    = evaluator;
        this.loopExecutor = new LoopExecutor(this::runBlock);
        this.registry     = buildRegistry();
    }

    private @NotNull Map<Class<?>, StatementHandler<Statement>> buildRegistry() {
        Map<Class<?>, StatementHandler<Statement>> map = new HashMap<>();

        map.put(LetStatement.class,      s -> visitLet((LetStatement) s));
        map.put(ConstStatement.class,    s -> visitConst((ConstStatement) s));
        map.put(AssignStatement.class,   s -> visitAssign((AssignStatement) s));
        map.put(ExprStatement.class,     s -> visitExpr((ExprStatement) s));
        map.put(PrintStatement.class,    s -> visitPrint((PrintStatement) s));
        map.put(IfStatement.class,       s -> visitIf((IfStatement) s));
        map.put(WhileStatement.class,    s -> visitWhile((WhileStatement) s));
        map.put(ForStatement.class,      s -> visitFor((ForStatement) s));
        map.put(BreakStatement.class,    s -> visitBreak((BreakStatement) s));
        map.put(ContinueStatement.class, s -> visitContinue((ContinueStatement) s));
        map.put(ReturnStatement.class,   s -> visitReturn((ReturnStatement) s));
        map.put(FunctionStatement.class, s -> visitFunction((FunctionStatement) s));
        map.put(ClassStatement.class,    s -> visitClass((ClassStatement) s));
        map.put(JavaBindStatement.class, s -> visitJavaBind((JavaBindStatement) s));
        map.put(ImportStatement.class,   s -> visitImport((ImportStatement) s));

        return map;
    }

    public void execute(@NotNull List<Statement> statements) {
        statements.forEach(this::execute);
    }

    /**
     * Looks up the statement's class in the registry and fires its handler.
     * No {@code switch}, no {@code instanceof} — one map lookup.
     */
    public void execute(@NotNull Statement st) {
        StatementHandler<Statement> handler = registry.get(st.getClass());
        if (handler == null) {
            throw new LarvError("No handler registered for: " + st.getClass().getSimpleName(), st.line());
        }
        try {
            handler.handle(st);
        } catch (LarvError e) {
            // Re-throw with line info from the statement if the error has none
            if (e.getLine() < 0 && st.line() >= 0) {
                throw new LarvError(e.getMessage(), st.line(), e.getColumn(), e.getKind());
            }
            throw e;
        }
    }

    // ── StatementVisitor ──────────────────────────────────────────────────────

    @Override
    public void visitLet(@NotNull LetStatement st) {
        Object value = st.expression() == null ? null : evaluator.eval(st.expression());
        context.getEnvironment().define(st.name(), value);
    }

    @Override
    public void visitConst(@NotNull ConstStatement st) {
        context.getEnvironment().defineConst(st.name(), evaluator.eval(st.value()));
    }

    @Override
    public void visitAssign(@NotNull AssignStatement st) {
        context.getEnvironment().assign(st.name(), evaluator.eval(st.value()));
    }

    @Override
    public void visitExpr(@NotNull ExprStatement st) {
        evaluator.eval(st.value());
    }

    @Override
    public void visitPrint(@NotNull PrintStatement st) {
        System.out.println(evaluator.eval(st.value()));
    }

    @Override
    public void visitIf(@NotNull IfStatement st) {
        List<Statement> branch = TruthinessEvaluator.isTruthy(evaluator.eval(st.condition()))
                ? st.thenBranch()
                : st.elseBranch();
        runBlock(branch);
    }

    @Override
    public void visitWhile(@NotNull WhileStatement st) {
        loopExecutor.runWhile(
                () -> TruthinessEvaluator.isTruthy(evaluator.eval(st.condition())),
                st.body()
        );
    }

    @Override
    public void visitFor(@NotNull ForStatement st) {
        execute(st.init());
        loopExecutor.runFor(
                () -> TruthinessEvaluator.isTruthy(evaluator.eval(st.condition())),
                st.body(),
                () -> execute(st.increment())
        );
    }

    @Override
    public void visitBreak(BreakStatement st) {
        throw new BreakSignal();
    }

    @Override
    public void visitContinue(ContinueStatement st) {
        throw new ContinueSignal();
    }

    @Override
    public void visitReturn(@NotNull ReturnStatement st) {
        throw new ReturnSignal(evaluator.eval(st.value()));
    }

    @Override
    public void visitFunction(FunctionStatement st) {
        context.defineFunction(st.name(), st);
    }

    @Override
    public void visitClass(ClassStatement st) {
        context.defineClass(st.name(), st);
    }

    @Override
    public void visitJavaBind(@NotNull JavaBindStatement st) {
        if (st.hasInvolve()) {
            context.getJavaRegistry().bindInstance(st.alias(), st.className(), st.constructorArgs());
        } else {
            context.getJavaRegistry().bind(st.alias(), st.className());
        }
    }

    public void visitImport(@NotNull ImportStatement st) {
        if (st.isFileImport()) {
            new LarvFileImporter(context, context.getProjectRoot()).importFile(st.path());
        } else {
            new NativeLibraryLoader(context).load(st.library());
        }
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