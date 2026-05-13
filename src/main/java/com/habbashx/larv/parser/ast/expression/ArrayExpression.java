package com.habbashx.larv.parser.ast.expression;

import java.util.List;

/**
 * AST node for an array literal expression.
 *
 * <p>Syntax: {@code [elem1, elem2, ...]}</p>
 *
 * <p>At runtime each element expression is evaluated left-to-right and the
 * results are collected into a {@code java.util.ArrayList}.</p>
 *
 * @param elements the expressions that produce the array's initial elements
 */
public record ArrayExpression(List<Expression> elements) implements Expression {
}
