package com.habbashx.larv.parser.stream;

import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.lexer.TokenType;
import com.habbashx.larv.parser.exception.ParseException;

import java.util.List;

/**
 * Wraps the flat token list and owns all cursor movement.
 *
 * No other class holds a {@code current} index or calls {@code tokens.get()}.
 * Every navigation decision — peek, advance, match, consume — lives here,
 * keeping the parsers free of bookkeeping noise.
 */
public class TokenStream {

    private final List<Token> tokens;
    private int current = 0;

    public TokenStream(List<Token> tokens) {
        this.tokens = tokens;
    }


    /** Returns the current token without consuming it. */
    public Token peek() {
        return tokens.get(current);
    }

    /** Returns the most recently consumed token. */
    public Token previous() {
        return tokens.get(current - 1);
    }

    /** True if the current token is {@code EOF}. */
    public boolean isAtEnd() {
        return peek().tokenType() == TokenType.EOF;
    }

    /** True if the current token is of the given type (without consuming it). */
    public boolean check(TokenType type) {
        return !isAtEnd() && peek().tokenType() == type;
    }

    /** True if the token one position ahead is of the given type. */
    public boolean checkNext(TokenType type) {
        return current + 1 < tokens.size()
                && tokens.get(current + 1).tokenType() == type;
    }

    /**
     * Advances past the current token and returns the one just consumed.
     * Safe to call at EOF — it will not advance past the last token.
     */
    public Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Advances if the current token matches any of the given types.
     *
     * @return {@code true} if a match was found and the cursor moved.
     */
    public boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts the current token is of the expected type, advances past it,
     * and returns it. Throws with {@code errorMessage} on mismatch.
     */
    public Token consume(TokenType expected, String errorMessage) {
        if (check(expected)) return advance();
        throw new ParseException(errorMessage + " — got: " + peek().tokenType(), peek());
    }

    /** Convenience: consume and return the raw string value of the token. */
    public String consumeValue(TokenType expected, String errorMessage) {
        return consume(expected, errorMessage).value();
    }
}