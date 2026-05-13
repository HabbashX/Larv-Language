package com.habbashx.larv.runtime.call;

import com.habbashx.larv.runtime.Interpreter;

import java.util.List;

/**
 * Common interface for all callable objects in the Larv runtime.
 *
 * <p>Both native (Java-backed) functions and user-defined Larv functions
 * implement this interface, allowing them to be stored in the same registry
 * and invoked through a uniform API.</p>
 *
 * @see NativeFunction
 * @see UserFunction
 */
public interface LarvCallable {

    /**
     * Invokes this callable with the given arguments.
     *
     * @param interpreter the active interpreter (provides access to the runtime)
     * @param args        the evaluated argument values, in call order
     * @return the result of the call, or {@code null} for void-like callables
     */
    Object call(Interpreter interpreter, List<Object> args);
}
