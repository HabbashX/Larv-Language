package com.habbashx.larv.compiler.runtime;

import com.habbashx.larv.error.LarvError;

import java.io.Serial;

/**
 * Thrown at runtime when the Larv interpreter encounters an unrecoverable
 * condition (division by zero, index out of bounds, undefined variable, etc.).
 *
 * <p>{@code LarvRuntimeException} extends {@link LarvError} so that the
 * top-level {@link com.habbashx.larv.error.ErrorReporter} can format it with
 * the same rich, Rust-style diagnostics as every other Larv error — including
 * source snippets, caret underlining, help hints, and stable error codes.</p>
 *
 * <p>Previously this class extended {@link RuntimeException} directly, which
 * meant every runtime fault fell through to the "unexpected Java exception"
 * catch-all in {@code ErrorReporter}, losing all diagnostic quality.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   throw new LarvRuntimeException("Division by zero", line, column)
 *       .withHint("guard with:  if (divisor != 0) { ... }");
 * </pre>
 */
public class LarvRuntimeException extends LarvError {

    @Serial
    private static final long serialVersionUID = 1485592394744512985L;


    /** Full position: message + line + column. */
    public LarvRuntimeException(String message, int line, int column) {
        super(message, line, column, Kind.RUNTIME);
    }

    /** Line only (column unknown). */
    public LarvRuntimeException(String message, int line) {
        super(message, line, -1, Kind.RUNTIME);
    }

    /** No source position (e.g. errors raised from native helpers without AST context). */
    public LarvRuntimeException(String message) {
        super(message, -1, -1, Kind.RUNTIME);
    }

    /**
     * Wraps a Java cause while preserving the original message.
     * The cause is stored via {@link Throwable#initCause} but is not
     * re-thrown — it is only accessible for debugging purposes.
     */
    public LarvRuntimeException(String message, Throwable cause) {
        super(message, -1, -1, Kind.RUNTIME);
        if (cause != null) initCause(cause);
    }

    /** Full position + cause. */
    public LarvRuntimeException(String message, int line, int column, Throwable cause) {
        super(message, line, column, Kind.RUNTIME);
        if (cause != null) initCause(cause);
    }
}
