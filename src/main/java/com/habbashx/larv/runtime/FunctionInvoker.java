package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.parser.ast.statement.FunctionStatement;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.signal.ReturnSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles the mechanics of calling user-defined Larv functions and methods.
 *
 * <p>{@code FunctionInvoker} encapsulates scope creation, parameter binding,
 * and return-signal catching so that call sites in
 * {@link ExpressionEvaluator} stay clean.</p>
 *
 * <h2>Scope management</h2>
 * <p>Every call pushes a fresh child scope onto the environment chain.  The
 * saved outer scope is restored in a {@code finally} block, so scope leaks
 * are impossible even if an exception propagates through the body.</p>
 *
 * <h2>Return values</h2>
 * <p>Body statements signal a return value by throwing a
 * {@link ReturnSignal}.  If no {@code return} is executed, the function
 * implicitly returns {@code null} (i.e. {@code nil}).</p>
 *
 * <h2>Circular dependency</h2>
 * <p>The {@link BlockRunner} dependency on {@link StatementExecutor} would
 * create a construction cycle.  It is therefore injected lazily via
 * {@link #setBlockRunner(BlockRunner)} after both objects are constructed.</p>
 */
public class FunctionInvoker {

    /**
     * Runs a list of statements as a scoped block.
     * Implemented by {@link StatementExecutor#runBlock}.
     */
    @FunctionalInterface
    public interface BlockRunner {
        /**
         * @param body the statements to execute in the current scope
         */
        void run(List<Statement> body);
    }

    private final ExecutionContext context;
    private BlockRunner blockRunner;

    /**
     * Constructs a {@code FunctionInvoker} for the given context.
     *
     * @param context the shared runtime state (environment, registries)
     */
    public FunctionInvoker(ExecutionContext context) {
        this.context = context;
    }

    /**
     * Injects the block runner after construction (breaks the circular
     * dependency with {@link StatementExecutor}).
     *
     * @param blockRunner the block execution strategy
     */
    public void setBlockRunner(BlockRunner blockRunner) {
        this.blockRunner = blockRunner;
    }

    /**
     * Invokes a user-defined function with the given positional arguments.
     *
     * <p>Pushes a new scope, binds parameters, executes the body, and returns
     * the result.  If fewer arguments are supplied than declared parameters,
     * the extra parameters are bound to {@code null}.</p>
     *
     * @param fn   the function declaration AST node
     * @param args the evaluated argument values
     * @return the function's return value, or {@code null} if no {@code return} was hit
     */
    public Object invokeFunction(FunctionStatement fn, List<Object> args) {
        return withScope(() -> {
            bindParams(fn.params(), args);
            return captureReturn(fn.body());
        });
    }

    /**
     * Invokes a method on {@code self} — same as {@link #invokeFunction} but
     * also binds {@code "this"} to the receiver object before binding parameters.
     *
     * @param fn   the method declaration AST node
     * @param self the receiver object
     * @param args the evaluated argument values
     * @return the method's return value, or {@code null} if no {@code return} was hit
     */
    public Object invokeMethod(FunctionStatement fn, LarvObject self, List<Object> args) {
        return withScope(() -> {
            context.getEnvironment().define("this", self);
            bindParams(fn.params(), args);
            return captureReturn(fn.body());
        });
    }

    /**
     * Pushes a fresh scope, runs {@code action}, then restores the previous scope.
     *
     * @param action the computation to run inside the new scope
     * @return the value returned by {@code action}
     */
    private Object withScope(java.util.concurrent.Callable<Object> action) {
        Environment saved = context.getEnvironment();
        context.pushScope();
        try {
            return action.call();
        } catch (LarvError e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new LarvError(e.getMessage());
        } finally {
            context.popScope(saved);
        }
    }

    /**
     * Binds each parameter name to the corresponding argument value in the
     * current environment.  Extra parameters beyond the argument list are
     * bound to {@code null}.
     *
     * @param params the formal parameter names
     * @param args   the evaluated argument values
     */
    private void bindParams(@NotNull List<String> params, List<Object> args) {
        for (int i = 0; i < params.size(); i++) {
            context.getEnvironment().define(params.get(i), i < args.size() ? args.get(i) : null);
        }
    }

    /**
     * Runs the function body and catches any {@link ReturnSignal}.
     *
     * @param body the statements to execute
     * @return the return value from a {@code return} statement, or {@code null}
     */
    private @Nullable Object captureReturn(List<Statement> body) {
        try {
            blockRunner.run(body);
            return null;
        } catch (ReturnSignal r) {
            return r.value;
        }
    }
}
