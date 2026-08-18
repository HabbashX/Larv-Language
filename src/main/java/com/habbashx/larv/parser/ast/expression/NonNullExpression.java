package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for the postfix non-null assertion operator {@code expr!}.
 *
 * <p>Asserts that the operand is not {@code nil} (Java {@code null}).  At
 * runtime the operand value is returned unchanged if non-null, otherwise a
 * {@code LarvRuntimeException} is thrown.  Type-inference treats it as having
 * the same type as its operand, which lets callers narrow nullable values
 * before passing them to typed parameters.</p>
 *
 * @param expression the asserted non-null operand
 */
public record NonNullExpression(Expression expression) implements Expression {}