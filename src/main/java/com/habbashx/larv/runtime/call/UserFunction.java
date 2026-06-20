package com.habbashx.larv.runtime.call;


import com.habbashx.larv.runtime.Environment;
import com.habbashx.larv.runtime.Interpreter;
import com.habbashx.larv.signal.ReturnSignal;

import java.util.List;


import com.habbashx.larv.parser.ast.statement.FunctionStatement;


/**
 * A {@link LarvCallable} representing a user-defined Larv function with
 * lexical closure semantics.
 *
 * <p>When a function is declared, a {@code UserFunction} is created that
 * captures the {@link Environment} at the declaration site as its
 * {@link #closure}.  When the function is called, a new child environment
 * is created from the closure, parameters are bound, and the body is executed.</p>
 *
 * <p><strong>Note:</strong> the body execution is currently a stub — see
 * {@link com.habbashx.larv.runtime.FunctionInvoker} for the active
 * implementation used by the main evaluator pipeline.</p>
 *
 * @see NativeFunction
 */
@Deprecated(since = "1.1.0")
public class UserFunction implements LarvCallable {

    /** The AST node that describes this function's parameters and body. */
    private final FunctionStatement fn;

    /**
     * The lexical environment captured at the point of declaration.
     * Used as the parent of each call's fresh scope.
     */
    private final Environment closure;

    /**
     * Creates a user function that closes over {@code closure}.
     *
     * @param fn      the function's AST declaration
     * @param closure the enclosing environment at declaration time
     */
    public UserFunction(FunctionStatement fn, Environment closure) {
        this.fn = fn;
        this.closure = closure;
    }

    /**
     * Calls the function by binding parameters and executing the body.
     *
     * @param interpreter the active interpreter
     * @param args        the evaluated argument values
     * @return the return value, or {@code null} if no {@code return} is hit
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> args) {

        Environment env = new Environment(closure);

        for (int i = 0; i < fn.params().size(); i++) {
            env.define(fn.params().get(i).name(), args.get(i));
        }

        try {
        } catch (ReturnSignal r) {
            return r.value;
        }

        return null;
    }
}
