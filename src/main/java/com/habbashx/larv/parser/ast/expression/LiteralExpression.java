package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a generic literal value.
 *
 * <p>Currently used exclusively for the {@code nil} keyword, which wraps a
 * Java {@code null}.  Other literal types ({@link NumberExpression},
 * {@link StringExpression}, {@link BooleanExpression}) have their own dedicated
 * nodes for type safety and pattern-match clarity.</p>
 *
 * @param value the literal value; {@code null} when the source token is {@code nil}
 */
public record LiteralExpression(Object value) implements Expression {
}
