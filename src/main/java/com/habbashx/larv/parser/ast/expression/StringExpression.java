package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing a string literal.
 *
 * <p>The {@link #value()} is the already-decoded string content — escape
 * sequences (e.g. {@code \n}, {@code \"}) have been processed by the
 * {@link com.habbashx.larv.lexer.Lexer}.</p>
 *
 * <p>Example: the source text {@code "hello\nworld"} produces a
 * {@code StringExpression} whose value contains an actual newline character.</p>
 *
 * @param value the decoded string value
 */
public record StringExpression(String value) implements Expression {
}
