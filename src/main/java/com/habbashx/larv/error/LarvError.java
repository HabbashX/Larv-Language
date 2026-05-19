package com.habbashx.larv.error;

/**
 * The single exception type used for ALL errors raised by the Larv runtime.
 *
 * <p>Every component — lexer, parser, evaluator, FFI layer — throws
 * {@code LarvError} rather than Java's built-in exception types.  This lets
 * the top-level {@link com.habbashx.larv.Main} catch one type and format a
 * human-readable error message.</p>
 *
 * <h2>Error kinds</h2>
 * <ul>
 *   <li>{@link Kind#RUNTIME} — wrong type, undefined variable, division by zero,
 *       bad array index, etc.</li>
 *   <li>{@link Kind#PARSE}   — unexpected token, missing brace/paren, bad grammar.</li>
 *   <li>{@link Kind#FFI}     — Java class not found, method not found, bad arg type.</li>
 *   <li>{@link Kind#LEXER}   — invalid character, unterminated string.</li>
 * </ul>
 *
 * <h2>Example formatted output</h2>
 * <pre>
 *   Error Line 7, Col 12: Undefined variable 'x'
 *   Syntax Error Line 3, Col  5: Expected ')' — got: SEMICOLON
 *   FFI Error Line 9, Col  1: Java class not found: java.lang.Meth
 *   Syntax Error Line 2, Col  8: Unterminated string — opened here
 * </pre>
 */
public class LarvError extends RuntimeException {

    /** Classifies the origin of the error for display and filtering purposes. */
    public enum Kind { RUNTIME, PARSE, FFI, LEXER }

    /** 1-based source line where the error occurred, or {@code -1} if unknown. */
    private final int  line;

    /** 1-based source column where the error occurred, or {@code -1} if unknown. */
    private final int  column;

    /** The category of this error. */
    private final Kind kind;

    /**
     * Full constructor.
     *
     * @param message the human-readable error description
     * @param line    1-based source line, or {@code -1} if unknown
     * @param column  1-based source column, or {@code -1} if unknown
     * @param kind    the error category
     */
    public LarvError(String message, int line, int column, Kind kind) {
        super(message);
        this.line   = line;
        this.column = column;
        this.kind   = kind;
    }

    /**
     * Runtime error at a specific line and column.
     *
     * @param message the error description
     * @param line    1-based source line
     * @param column  1-based source column
     */
    public LarvError(String message, int line, int column) {
        this(message, line, column, Kind.RUNTIME);
    }

    /**
     * Error with a line number but no column (e.g. FFI errors).
     *
     * @param message the error description
     * @param line    1-based source line
     * @param kind    the error category
     */
    public LarvError(String message, int line, Kind kind) {
        this(message, line, -1, kind);
    }

    /**
     * Runtime error with a line number but no column.
     *
     * @param message the error description
     * @param line    1-based source line
     */
    public LarvError(String message, int line) {
        this(message, line, -1, Kind.RUNTIME);
    }

    /**
     * Runtime error with no position information (last resort).
     *
     * @param message the error description
     */
    public LarvError(String message) {
        this(message, -1, -1, Kind.RUNTIME);
    }

    /** Returns the 1-based source line, or {@code -1} if unknown. */
    public int  getLine()   { return line; }

    /** Returns the 1-based source column, or {@code -1} if unknown. */
    public int  getColumn() { return column; }

    /** Returns the error category ({@link Kind}). */
    public Kind getKind()   { return kind; }

    /**
     * Formats the error as a human-readable one-line message suitable for
     * printing to {@code stderr}.
     *
     * <p>Examples:
     * <pre>
     *   Error Line 7, Col 12: Undefined variable 'x'
     *   Syntax Error Line 3, Col  5: Expected ')' — got: SEMICOLON
     *   FFI Error: Java class not found: java.lang.Meth
     * </pre>
     *
     * @return the formatted error string
     */
    public String format() {
        String prefix = switch (kind) {
            case PARSE, LEXER -> "Syntax Error";
            case FFI          -> "FFI Error";
            case RUNTIME      -> "Error";
        };

        if (line < 0)   return prefix + " " + getMessage();
        if (column < 0) return prefix + " Line " + line + ": " + getMessage();
        return prefix + " Line " + line + ", Col " + column + ": " + getMessage();
    }
}