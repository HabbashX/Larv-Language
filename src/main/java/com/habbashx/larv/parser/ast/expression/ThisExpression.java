package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing the {@code this} keyword.
 *
 * <p>Inside a method body, {@code this} evaluates to the
 * {@link com.habbashx.larv.runtime.LarvObject} that the method was called on.
 * It is looked up in the current environment under the reserved key
 * {@code "this"}.</p>
 */
public record ThisExpression() implements Expression {
}
