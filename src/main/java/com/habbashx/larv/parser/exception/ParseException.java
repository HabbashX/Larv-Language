package com.habbashx.larv.parser.exception;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Token;

/**
 * Thrown by the parser when the token stream doesn't match the grammar.
 * Pulls line + column directly from the offending token.
 */
public class ParseException extends LarvError {

    private final Token offendingToken;

    public ParseException(String message, Token offendingToken) {
        super(message, offendingToken.line(), offendingToken.column(), Kind.PARSE);
        this.offendingToken = offendingToken;
    }

    public Token getOffendingToken() {
        return offendingToken;
    }
}
