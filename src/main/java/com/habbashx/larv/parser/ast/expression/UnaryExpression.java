package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a unary prefix operation.
 *
 * <p>Currently the only supported unary operator is {@code -} (arithmetic
 * negation).  Example: {@code -x} produces
 * {@code UnaryExpression("-", VarExpression("x"))}.</p>
 *
 * @param operator the operator symbol (currently only {@code "-"})
 * @param right    the operand expression
 */
public record UnaryExpression(String operator, Expression right) implements Expression {}
