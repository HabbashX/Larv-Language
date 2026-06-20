package com.habbashx.larv.runtime.call;

import com.habbashx.larv.runtime.Interpreter;

import java.util.List;
import java.util.function.Function;

/**
 * A {@link LarvCallable} backed by a Java lambda or method reference.
 *
 * <p>Native functions are registered in the execution context via
 * {@link com.habbashx.larv.runtime.ExecutionContext#registerNative} and
 * are invoked when a Larv program calls a built-in function by name.</p>
 *
 * <p>The underlying {@link Function} receives the already-evaluated argument
 * list and returns the result directly, bypassing the AST entirely.</p>
 *
 * @see com.habbashx.larv.runtime.registry.NativeRegistry
 * @see com.habbashx.larv.runtime.stdlib.NativeLibrary
 */
@Deprecated(since = "1.1.0")
public class NativeFunction implements LarvCallable {

    /** The Java implementation of this native function. */
    private final Function<List<Object>, Object> fn;

    /**
     * Wraps a Java function as a {@code NativeFunction}.
     *
     * @param fn the implementation that receives evaluated args and returns the result
     */
    public NativeFunction(Function<List<Object>, Object> fn) {
        this.fn = fn;
    }

    /**
     * Delegates directly to the underlying Java function.
     *
     * @param interpreter unused (native functions have direct Java access)
     * @param args        the evaluated argument list
     * @return the result of the Java function
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        return fn.apply(args);
    }
}
