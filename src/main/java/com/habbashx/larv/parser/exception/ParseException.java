package com.habbashx.larv.parser.exception;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Token;

/**
 * Thrown by the parser when the token stream does not match the expected grammar.
 *
 * <p>{@code ParseException} is a specialised {@link LarvError} of kind
 * {@link LarvError.Kind#PARSE}.  It carries the offending {@link Token} so
 * that both the error message and exact source position (line + column) can
 * be reported to the user.</p>
 *
 * <h2>Example</h2>
 * <pre>
 *   Syntax Error Line 5, Col 8: Expected ')' after arguments — got: SEMICOLON
 * </pre>
 */
public class ParseException extends LarvError {

    /**
     * The token that was present when a different token was expected.
     * Stored for programmatic access (e.g. error recovery or IDE integration).
     */
    private final Token offendingToken;

    /**
     * Creates a {@code ParseException} rooted at the given token.
     *
     * @param message        the human-readable parse error description
     * @param offendingToken the token that triggered the error; its
     *                       {@link Token#line()} and {@link Token#column()} are
     *                       used for source position reporting
     */
    public ParseException(String message, Token offendingToken) {
        super(message, offendingToken.line(), offendingToken.column(), Kind.PARSE);
        this.offendingToken = offendingToken;
    }

    /**
     * Returns the token that was present when the parser expected something else.
     *
     * @return the offending {@link Token}
     */
    public Token getOffendingToken() {
        return offendingToken;
    }
}