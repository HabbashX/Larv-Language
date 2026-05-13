package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing a boolean literal — either {@code true} or {@code false}.
 *
 * @param value {@code true} or {@code false} as parsed from source
 */
public record BooleanExpression(boolean value) implements Expression {
}
