package com.habbashx.larv.error;

/**
 * The single exception type for ALL Larv errors.
 *
 * Carries line + column so every error message pinpoints the exact position.
 *
 * Example output:
 *   [Larv Error]       Line 7, Col 12: Undefined variable 'x'
 *   [Larv Parse Error] Line 3, Col  5: Expected ')' — got: SEMICOLON
 *   [Larv FFI Error]   Line 9, Col  1: Class not found: java.lang.Meth
 *   [Larv Lexer Error] Line 2, Col  8: Unterminated string — opened here
 */
public class LarvError extends RuntimeException {

    public enum Kind { RUNTIME, PARSE, FFI, LEXER }

    private final int  line;
    private final int  column;
    private final Kind kind;


    public LarvError(String message, int line, int column, Kind kind) {
        super(message);
        this.line   = line;
        this.column = column;
        this.kind   = kind;
    }

    // ── convenience constructors ──────────────────────────────────────────────

    /** Runtime error at a specific line and column. */
    public LarvError(String message, int line, int column) {
        this(message, line, column, Kind.RUNTIME);
    }

    /** Error with line only (column unknown, e.g. FFI errors). */
    public LarvError(String message, int line, Kind kind) {
        this(message, line, -1, kind);
    }

    /** Error with line only. */
    public LarvError(String message, int line) {
        this(message, line, -1, Kind.RUNTIME);
    }

    /** Error with no position info (last resort). */
    public LarvError(String message) {
        this(message, -1, -1, Kind.RUNTIME);
    }


    public int  getLine()   { return line; }
    public int  getColumn() { return column; }
    public Kind getKind()   { return kind; }



    public String format() {
        String prefix = switch (kind) {
            case PARSE, LEXER -> "Syntax Error";
            case FFI     -> "FFI Error";
            case RUNTIME -> "Error";
        };

        if (line < 0)            return prefix + " " + getMessage();
        if (column < 0)          return prefix + " Line " + line + ": " + getMessage();
        return prefix + " Line " + line + ", Col " + column + ": " + getMessage();
    }
}
