package com.habbashx.larv.error;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single exception type used for ALL errors raised by the Larv toolchain.
 *
 * <p>Produces rich, Rust/Elm-style diagnostics with:</p>
 * <ul>
 *   <li>Coloured header — kind label, stable error code, and message</li>
 *   <li>Source pointer — file, line, column</li>
 *   <li>Source snippet — the offending line with a caret (^) underline</li>
 *   <li>Contextual hint — a "help:" note specific to the error</li>
 *   <li>Note / secondary annotation (optional)</li>
 * </ul>
 *
 * <h2>Example output</h2>
 * <pre>
 *   error[E001]: undefined variable 'x'
 *    --> main.larv:12:5
 *     |
 *  12 |   print x + 1
 *     |         ^ variable 'x' is not defined in this scope
 *     |
 *     = help: did you mean 'xi'? or declare it with: var x = ...
 * </pre>
 */
public class LarvError extends RuntimeException {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BLUE   = "\u001B[34m";
    private static final String GREEN  = "\u001B[32m";
    private static final String DIM    = "\u001B[2m";


    public enum Kind { RUNTIME, PARSE, COMPILE, FFI, LEXER }


    private final int    line;
    private final int    column;
    private final Kind   kind;

    /** Optional: the full source text, used to render the snippet. */
    private String sourceText;

    /** Optional: file name shown in the pointer line. */
    private String sourceFile;

    /** Optional: extra "help:" hint appended below the snippet. */
    private String hint;

    /** Optional: secondary annotation appended as a note. */
    private String note;

    /** Optional: span length for the caret underline (default 1). */
    private int spanLength = 1;

    /**
     * Optional: overrides the default kind-based error code (e.g. "E007").
     * Set via {@link #withCode(String)}.
     */
    private String customCode = null;

    public LarvError(String message, int line, int column, Kind kind) {
        super(message);
        this.line   = line;
        this.column = column;
        this.kind   = kind;
    }

    public LarvError(String message, int line, int column) {
        this(message, line, column, Kind.RUNTIME);
    }

    public LarvError(String message, int line, Kind kind) {
        this(message, line, -1, kind);
    }

    public LarvError(String message, int line) {
        this(message, line, -1, Kind.RUNTIME);
    }

    public LarvError(String message) {
        this(message, -1, -1, Kind.RUNTIME);
    }

    public LarvError withSource(String sourceText) {
        this.sourceText = sourceText;
        return this;
    }

    public LarvError withFile(String sourceFile) {
        this.sourceFile = sourceFile;
        return this;
    }

    public LarvError withHint(String hint) {
        this.hint = hint;
        return this;
    }

    public LarvError withNote(String note) {
        this.note = note;
        return this;
    }

    public LarvError withSpan(int length) {
        this.spanLength = Math.max(1, length);
        return this;
    }

    /**
     * Overrides the default kind-based error code with a specific one.
     * Useful for sub-categories of runtime errors (e.g. {@code "E006"} for
     * division-by-zero vs generic {@code "E001"} runtime errors).
     *
     * @param code a stable error code string (e.g. {@code "E006"})
     * @return {@code this} for chaining
     */
    public LarvError withCode(String code) {
        this.customCode = code;
        return this;
    }

    public int  getLine()   { return line; }
    public int  getColumn() { return column; }
    public Kind getKind()   { return kind; }


    @Contract(pure = true)
    private @NotNull String kindLabel() {
        return switch (kind) {
            case PARSE, LEXER -> "syntax error";
            case COMPILE      -> "compile error";
            case FFI          -> "ffi error";
            case RUNTIME      -> "error";
        };
    }

    /**
     * Returns a stable, searchable error code for this error.
     *
     * <p>Codes are grouped by kind:</p>
     * <ul>
     *   <li>{@code E001} — generic runtime error</li>
     *   <li>{@code E002} — parse / syntax error</li>
     *   <li>{@code E003} — compile error</li>
     *   <li>{@code E004} — FFI / Java interop error</li>
     *   <li>{@code E005} — lexer error</li>
     *   <li>{@code E006} — division / modulo by zero</li>
     *   <li>{@code E007} — index out of bounds</li>
     *   <li>{@code E008} — undefined variable</li>
     *   <li>{@code E009} — undefined function</li>
     *   <li>{@code E010} — wrong argument count (arity)</li>
     *   <li>{@code E011} — type mismatch</li>
     *   <li>{@code E012} — null / nil dereference</li>
     * </ul>
     *
     * <p>A specific code set via {@link #withCode(String)} always takes
     * precedence over the kind-derived default.</p>
     */
    public String errorCode() {
        if (customCode != null) return customCode;
        return switch (kind) {
            case RUNTIME -> "E001";
            case PARSE   -> "E002";
            case COMPILE -> "E003";
            case FFI     -> "E004";
            case LEXER   -> "E005";
        };
    }

    private String kindColour() {
        return switch (kind) {
            case PARSE, LEXER -> YELLOW;
            case COMPILE      -> CYAN;
            case FFI          -> BLUE;
            case RUNTIME      -> RED;
        };
    }

    /**
     * Extracts the requested line (1-based) from {@code sourceText}.
     * Returns {@code null} if the source is absent or the line is out of range.
     */
    @Contract(pure = true)
    private @Nullable String sourceLine(int lineNumber) {
        if (sourceText == null || lineNumber < 1) return null;
        String[] lines = sourceText.split("\n", -1);
        if (lineNumber > lines.length) return null;
        return lines[lineNumber - 1];
    }

    /**
     * Builds a Rust-style snippet block:
     * <pre>
     *     |
     *  12 |   print x + 1
     *     |         ^^^^^ message
     *     |
     * </pre>
     *
     * @param lineContent the source line text
     * @param col         1-based column of the caret start; -1 = skip caret
     * @param caretMsg    short message placed after the caret (may be null)
     * @param lineNum     the line number shown in the gutter
     */
    private @NotNull String buildSnippet(String lineContent, int col, String caretMsg, int lineNum) {
        String kc   = kindColour();
        int    gw   = String.valueOf(lineNum).length();         // gutter width
        String pipe = BLUE + " ".repeat(gw + 1) + "|" + RESET;

        StringBuilder sb = new StringBuilder();
        sb.append(pipe).append("\n");

        sb.append(BLUE).append(String.format(" %d", lineNum)).append(" | ").append(RESET);
        sb.append(lineContent).append("\n");

        if (col > 0) {
            int caretOffset = Math.min(col - 1, lineContent.length());
            int caretLen    = Math.min(spanLength, lineContent.length() - caretOffset);
            if (caretLen < 1) caretLen = 1;

            sb.append(pipe).append(" ")
                    .append(" ".repeat(caretOffset))
                    .append(kc).append(BOLD)
                    .append("^".repeat(caretLen))
                    .append(RESET);

            if (caretMsg != null && !caretMsg.isBlank()) {
                sb.append(" ").append(kc).append(caretMsg).append(RESET);
            }
            sb.append("\n");
        }

        sb.append(pipe).append("\n");
        return sb.toString();
    }


    /**
     * Formats a full multi-line diagnostic with colour, snippet, and hints.
     *
     * @param sourceText the full source text of the file being processed
     *                   (may be {@code null} — snippet section is skipped)
     * @param sourceFile the file name shown in the pointer (may be {@code null})
     * @return the formatted diagnostic string (no trailing newline)
     */
    public String format(String sourceText, String sourceFile) {
        if (sourceText != null && this.sourceText == null) this.sourceText = sourceText;
        if (sourceFile != null && this.sourceFile == null) this.sourceFile = sourceFile;
        return buildDiagnostic();
    }

    /** Format using already-attached source/file (or without snippet). */
    public String format(String sourceFile) {
        if (sourceFile != null && this.sourceFile == null) this.sourceFile = sourceFile;
        return buildDiagnostic();
    }

    public String format() { return buildDiagnostic(); }


    private @NotNull String buildDiagnostic() {
        String kc   = kindColour();
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(BOLD).append(kc)
                .append(kindLabel()).append("[").append(errorCode()).append("]")
                .append(RESET).append(BOLD).append(": ")
                .append(getMessage())
                .append(RESET)
                .append("\n");

        if (line >= 0) {
            sb.append(BLUE).append(" --> ").append(RESET);
            if (this.sourceFile != null && !this.sourceFile.isBlank()) {
                sb.append(this.sourceFile);
            } else {
                sb.append("<source>");
            }
            sb.append(":").append(line);
            if (column >= 0) sb.append(":").append(column);
            sb.append("\n");
        }

        String lineContent = sourceLine(line);
        if (lineContent != null) {
            String prevLine = sourceLine(line - 1);
            if (prevLine != null) {
                int gw = String.valueOf(line).length();
                sb.append(BLUE).append(" ".repeat(gw + 1)).append("|").append(RESET).append("\n");
                sb.append(DIM)
                        .append(BLUE).append(String.format(" %d", line - 1)).append(" | ").append(RESET)
                        .append(DIM).append(prevLine).append(RESET).append("\n");
            }

            String caretMsg = buildInlineCaretMessage();
            sb.append(buildSnippet(lineContent, column, caretMsg, line));
        } else if (line >= 0) {
            String gutter = BLUE + " " + "|" + RESET;
            sb.append(gutter).append("\n");
        }
        if (hint != null && !hint.isBlank()) {
            sb.append(GREEN).append(BOLD).append("     = help: ").append(RESET)
                    .append(hint).append("\n");
        }

        if (note != null && !note.isBlank()) {
            sb.append(DIM).append("     = note: ").append(RESET)
                    .append(DIM).append(note).append(RESET).append("\n");
        }

        return sb.toString();
    }

    /**
     * Derives a concise inline caret message from the full error message,
     * e.g. "undefined variable 'x'" → "not defined here".
     * Falls back to an empty string if nothing shorter can be said.
     */
    private @NotNull String buildInlineCaretMessage() {
        String msg = getMessage();
        if (msg == null) return "";
        if (msg.length() <= 35) return msg;
        int stop = msg.indexOf('.');
        if (stop < 0) stop = msg.indexOf(',');
        if (stop > 0 && stop <= 50) return msg.substring(0, stop);
        return msg.substring(0, Math.min(msg.length(), 40)) + "…";
    }


    @Override
    public String toString() { return format(); }


    /**
     * Creates a RUNTIME error with a built-in hint for undefined variables.
     *
     * @param name    the variable name that was not found
     * @param line    source line
     * @param column  source column
     * @param similar a similar name found in scope (may be null)
     */
    public static LarvError undefinedVariable(String name, int line, int column, String similar) {
        String hint = similar != null
                ? "did you mean '" + similar + "'? Or declare it with:  var " + name + " = ..."
                : "declare it with:  var " + name + " = ...  or  const " + name + " = ...";
        return new LarvError("undefined variable '" + name + "'", line, column, Kind.RUNTIME)
                .withCode("E008")
                .withHint(hint);
    }

    /**
     * Creates a RUNTIME error for type mismatches with a descriptive hint.
     */
    public static LarvError typeMismatch(String op, String expected, String got,
                                         int line, int column) {
        return new LarvError(
                "type mismatch in '" + op + "': expected " + expected + ", got " + got,
                line, column, Kind.RUNTIME)
                .withCode("E011")
                .withHint("you can convert with:  strToNumber(x)  or  toString(x)");
    }

    /**
     * Creates a PARSE error for a missing/unexpected token.
     *
     * @param expected human-readable description of the expected token
     * @param got      the token that appeared instead
     * @param line     source line
     * @param column   source column
     */
    public static LarvError unexpectedToken(String expected, String got,
                                            int line, int column) {
        return new LarvError(
                "expected " + expected + ", got: " + got,
                line, column, Kind.PARSE)
                .withNote("the parser stopped here because the token stream no longer matched the grammar");
    }

    /**
     * Creates a COMPILE error for an unsupported or missing construct.
     */
    @Contract("_, _ -> new")
    public static @NotNull LarvError compileError(String message, int line) {
        return new LarvError(message, line, -1, Kind.COMPILE);
    }

    /**
     * Creates an FFI error for a missing Java class.
     */
    public static LarvError classNotFound(String fqcn) {
        return new LarvError("Java class not found: '" + fqcn + "'", -1, Kind.FFI)
                .withHint("make sure the class is on the classpath and the fully-qualified name is correct");
    }

    /**
     * Creates a RUNTIME error for division by zero.
     */
    public static LarvError divisionByZero(int line, int column) {
        return new LarvError("division by zero", line, column, Kind.RUNTIME)
                .withCode("E006")
                .withHint("guard with:  if (divisor != 0) { ... }")
                .withNote("dividing by zero is undefined in mathematics and causes a runtime fault");
    }

    /**
     * Creates a RUNTIME error for calling an undefined function.
     */
    public static LarvError undefinedFunction(String name, int line, int column) {
        return new LarvError("undefined function '" + name + "'", line, column, Kind.RUNTIME)
                .withCode("E009")
                .withHint("define it above the call site with:  func " + name + "(...) { ... }");
    }

    /**
     * Creates a RUNTIME error for wrong arity.
     */
    public static LarvError wrongArity(String name, int expected, int got,
                                       int line, int column) {
        String s = expected == 1 ? "" : "s";
        return new LarvError(
                "'" + name + "' expects " + expected + " argument" + s + " but got " + got,
                line, column, Kind.RUNTIME)
                .withCode("E010")
                .withHint(got < expected
                        ? "add the " + (expected - got) + " missing argument(s)"
                        : "remove the " + (got - expected) + " extra argument(s)");
    }

    /**
     * Creates a RUNTIME error for an index out of bounds.
     */
    public static LarvError indexOutOfBounds(int index, int size, int line, int column) {
        return new LarvError(
                "index " + index + " is out of bounds for a list of size " + size,
                line, column, Kind.RUNTIME)
                .withCode("E007")
                .withHint("valid indices are 0 to " + (size - 1) + " (or " + (-size) + " to -1 from the end)");
    }

    /**
     * Creates a RUNTIME error for accessing a field or calling a method on nil.
     */
    public static LarvError nullDereference(String operation, int line, int column) {
        return new LarvError(
                "cannot " + operation + " on nil",
                line, column, Kind.RUNTIME)
                .withCode("E012")
                .withHint("check that the value is not nil before using it:  if (value != nil) { ... }");
    }

    /**
     * Creates a RUNTIME error for modulo by zero.
     */
    public static LarvError moduloByZero(int line, int column) {
        return new LarvError("modulo by zero", line, column, Kind.RUNTIME)
                .withCode("E006")
                .withHint("guard with:  if (divisor != 0) { ... }")
                .withNote("the remainder of division by zero is undefined");
    }
}
