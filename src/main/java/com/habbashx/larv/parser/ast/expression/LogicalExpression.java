package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for short-circuit logical operators {@code &&} and {@code ||}.
 *
 * <p>Unlike {@link BinaryExpression}, the right-hand side is <em>not</em>
 * evaluated eagerly — it is only evaluated when the left side doesn't
 * already determine the result:</p>
 * <ul>
 *   <li>{@code &&} — if left is falsy, return left immediately (skip right).</li>
 *   <li>{@code ||} — if left is truthy, return left immediately (skip right).</li>
 * </ul>
 *
 * @param left     left-hand operand
 * @param operator {@code "&&"} or {@code "||"}
 * @param right    right-hand operand (lazily evaluated)
 */
public record LogicalExpression(
        Expression left,
        String     operator,
        Expression right
) implements Expression {}
