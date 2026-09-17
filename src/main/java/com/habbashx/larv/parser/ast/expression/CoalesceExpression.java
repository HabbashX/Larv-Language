package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for the nil-coalescing operator: {@code left ?? right}.
 *
 * <p>Evaluates {@code left}; when it is non-{@code nil} it is the result,
 * otherwise {@code right} is evaluated and returned.  The right side is
 * lazy — it never runs when the left side is non-{@code nil} — unlike the
 * eager operands of {@link BinaryExpression}.</p>
 *
 * @param left  the preferred value expression
 * @param right the fallback expression (evaluated only when needed)
 */
public record CoalesceExpression(Expression left, Expression right) implements Expression {
}
