package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a mutable variable declaration.
 *
 * <p>Syntax: {@code var name = expr} or, inside a class body,
 * {@code var name = expr : get,set} / {@code var name = expr : get}</p>
 *
 * @param name       the variable name
 * @param expression the initializer expression, or {@code null} for uninitialized
 * @param hasGetter  whether a public getter method should be generated (class fields only)
 * @param hasSetter  whether a public setter method should be generated (class fields only)
 * @param line       the 1-based source line of the declaration
 */
public record VarStatement(String name,String type, Expression expression, boolean hasGetter, boolean hasSetter,boolean isVolatile, int line) implements Statement {
}
