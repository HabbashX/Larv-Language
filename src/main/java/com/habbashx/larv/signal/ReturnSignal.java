package com.habbashx.larv.signal;

/**
 * Control-flow exception used to propagate a {@code return} value up the
 * call stack.
 *
 * <p>When the executor visits a {@code return} statement it evaluates the
 * return expression and throws this signal.
 * {@link com.habbashx.larv.runtime.FunctionInvoker} catches it and extracts
 * the {@link #value}.</p>
 *
 * <p>Using an exception avoids the need for explicit "did we return?"
 * boolean flags threading through every loop and block helper.</p>
 */
public class ReturnSignal extends RuntimeException {

    /** The value to be returned from the current function. */
    public final Object value;

    /**
     * Constructs a {@code ReturnSignal} carrying the given return value.
     *
     * @param value the return value ({@code null} for {@code return} with no expression)
     */
    public ReturnSignal(Object value) {
        this.value = value;
    }
}
