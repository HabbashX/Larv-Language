package com.habbashx.larv.compiler.exception;

import com.habbashx.larv.error.LarvError;

/**
 * Thrown during Larv → JVM bytecode compilation when a construct cannot be
 * compiled (unsupported node, undefined variable in a strict context, etc.).
 *
 * <p>{@code CompileException} is a thin wrapper around {@link LarvError} so
 * that the compiler pipeline can throw a typed exception and the top-level
 * {@link com.habbashx.larv.error.ErrorReporter} formats it the same way as
 * every other Larv diagnostic.</p>
 */
public class CompileException extends LarvError {

    /**
     * Creates a compile error with a line number.
     *
     * @param message human-readable description of what went wrong
     * @param line    1-based source line, or {@code -1} if unknown
     */
    public CompileException(String message, int line) {
        super(message, line, -1, Kind.COMPILE);
    }

    /**
     * Creates a compile error with a line number and an inline hint.
     *
     * @param message human-readable description
     * @param line    1-based source line
     * @param hint    a "help:" note shown below the snippet
     */
    public CompileException(String message, int line, String hint) {
        super(message, line, -1, Kind.COMPILE);
        withHint(hint);
    }

    /**
     * Creates a compile error with full source position (line + column).
     */
    public CompileException(String message, int line, int column) {
        super(message, line, column, Kind.COMPILE);
    }
}