package com.habbashx.larv.parser;

import com.habbashx.larv.lexer.TokenType;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.stream.TokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Parses comma-separated argument and parameter lists.
 *
 * This pattern appears in four places in the grammar — function declarations,
 * function calls, method calls, and {@code new} expressions. Centralising it
 * here eliminates the duplication and gives a single place to change if the
 * grammar ever allows trailing commas or default values.
 *
 * <pre>
 *   argList  →  '(' (expr (',' expr)*)? ')'
 *   paramList →  '(' (IDENTIFIER (',' IDENTIFIER)*)? ')'
 * </pre>
 */
public final class ArgumentParser {

    private final TokenStream stream;

    public ArgumentParser(TokenStream stream) {
        this.stream = stream;
    }

    /**
     * Parses a parenthesised, comma-separated list of expressions.
     * The opening {@code (} must already have been consumed.
     */
    public @NotNull List<Expression> parseArguments(Supplier<Expression> expressionParser) {
        return parseCommaSeparated(
                TokenType.RPAREN,
                expressionParser,
                "Expected ')' after arguments"
        );
    }

    /**
     * Parses a parenthesised, comma-separated list of parameter names.
     * The opening {@code (} must already have been consumed.
     */
    public @NotNull List<String> parseParameters() {
        List<String> params = new ArrayList<>();
        if (!stream.check(TokenType.RPAREN)) {
            do {
                params.add(stream.consumeValue(TokenType.IDENTIFIER, "Expected parameter name"));
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(TokenType.RPAREN, "Expected ')' after parameters");
        return params;
    }

    private <T> @NotNull List<T> parseCommaSeparated(
            TokenType terminator,
            Supplier<T> itemParser,
            String closeError
    ) {
        List<T> items = new ArrayList<>();
        if (!stream.check(terminator)) {
            do {
                items.add(itemParser.get());
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(terminator, closeError);
        return items;
    }
}