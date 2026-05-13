package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for an array index access.
 *
 * <p>Syntax: {@code array[index]}</p>
 *
 * <p>At runtime {@link #array()} must evaluate to a {@code List} and
 * {@link #index()} must evaluate to a {@code Double} (Larv's number type).
 * The index is truncated to {@code int} before use.  Out-of-bounds access
 * throws a {@link com.habbashx.larv.error.LarvError}.</p>
 *
 * @param array the expression that evaluates to the target list
 * @param index the expression that evaluates to the numeric index
 */
public record IndexExpression(Expression array, Expression index) implements Expression {
}
