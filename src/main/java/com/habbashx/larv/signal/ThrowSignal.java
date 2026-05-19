package com.habbashx.larv.signal;

/**
 * Control-flow exception thrown by a {@code throw} statement to propagate
 * a user-level error up the call stack.
 *
 * <p>When {@link com.habbashx.larv.runtime.StatementExecutor} visits a
 * {@link com.habbashx.larv.parser.ast.statement.ThrowStatement}, it evaluates
 * the thrown expression and wraps the result in a {@code ThrowSignal}.</p>
 *
 * <p>The signal is caught inside a {@code try} block handler in
 * {@link com.habbashx.larv.runtime.StatementExecutor#visitTryCatch}.  If not
 * caught, it propagates to the top level where the interpreter reports it as
 * an uncaught error and exits with code 1.</p>
 *
 * <p>Stack trace generation is suppressed ({@code writableStackTrace = false})
 * for performance — this is an expected control-flow mechanism, not an
 * unexpected exception.</p>
 */
public class ThrowSignal extends RuntimeException {

    /**
     * The Larv value that was thrown — can be any runtime type:
     * string, number, boolean, {@link com.habbashx.larv.runtime.LarvObject},
     * or {@code null} ({@code nil}).
     */
    public final Object value;

    /**
     * Creates a {@code ThrowSignal} carrying the given Larv value.
     *
     * @param value the thrown value; may be {@code null} (representing {@code nil})
     */
    public ThrowSignal(Object value) {
        super(value != null ? value.toString() : "nil", null, true, false);
        this.value = value;
    }
}