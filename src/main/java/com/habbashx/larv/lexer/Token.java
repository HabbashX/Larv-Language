package com.habbashx.larv.lexer;

/**
 * An immutable token produced by the {@link Lexer} during source scanning.
 *
 * <p>Each token captures four pieces of information:</p>
 * <ul>
 *   <li>{@link #tokenType()} — the syntactic category (e.g. {@code NUMBER}, {@code IDENTIFIER}).</li>
 *   <li>{@link #value()}     — the raw source text of this token (e.g. {@code "42"}, {@code "myVar"}).</li>
 *   <li>{@link #line()}      — the 1-based source line where the token starts.</li>
 *   <li>{@link #column()}    — the 1-based source column where the token starts.</li>
 * </ul>
 *
 * <p>Line and column are used exclusively for error reporting so that every
 * parse or runtime error can pinpoint exactly where in the source the problem
 * occurred.  Synthetic tokens that have no real source position (e.g. the EOF
 * sentinel) use {@code -1} for both fields.</p>
 *
 * <p>As a Java record, {@code Token} is value-equal: two tokens are {@code equals}
 * if and only if all four components match.</p>
 *
 * @param tokenType the syntactic category of this token
 * @param value     the raw source text
 * @param line      1-based source line, or {@code -1} if not applicable
 * @param column    1-based source column, or {@code -1} if not applicable
 */
public record Token(TokenType tokenType, String value, int line, int column) {

    /**
     * Convenience constructor for synthetic tokens that have no source position
     * (e.g. the {@code EOF} token appended at the end of the token stream).
     *
     * <p>Sets both {@link #line()} and {@link #column()} to {@code -1}.</p>
     *
     * @param tokenType the syntactic category
     * @param value     the raw source text (often empty for synthetic tokens)
     */
    public Token(TokenType tokenType, String value) {
        this(tokenType, value, -1, -1);
    }
}
