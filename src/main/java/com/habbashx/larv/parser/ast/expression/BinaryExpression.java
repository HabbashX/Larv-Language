package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a binary infix operation.
 *
 * <p>Covers all two-operand operators in Larv:
 * arithmetic ({@code +}, {@code -}, {@code *}, {@code /}),
 * relational ({@code <}, {@code >}, {@code <=}, {@code >=}),
 * and equality ({@code ==}, {@code !=}).</p>
 *
 * <p>The actual computation is delegated to
 * {@link com.habbashx.larv.runtime.BinaryOperator#apply(String, Object, Object)}
 * at runtime.</p>
 *
 * @param left     the left-hand operand
 * @param operator the operator symbol as a string (e.g. {@code "+"}, {@code "=="})
 * @param right    the right-hand operand
 */
public record BinaryExpression(Expression left, String operator, Expression right) implements Expression {
}
