package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing a variable reference.
 *
 * <p>At runtime the evaluator looks up {@link #name()} in the current
 * {@link com.habbashx.larv.runtime.Environment} and returns the stored value.</p>
 *
 * <p>Example: in the expression {@code x + 1}, the {@code x} portion is
 * represented as {@code VarExpression("x")}.</p>
 *
 * @param name the variable name as written in source
 */
public record VarExpression(String name) implements Expression {
}
