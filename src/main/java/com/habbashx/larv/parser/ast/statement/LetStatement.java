package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a mutable variable declaration.
 *
 * <p>Syntax: {@code var name = expr}</p>
 *
 * <p>If {@link #expression()} is {@code null} the variable is declared but
 * uninitialized (its runtime value will be {@code null} / {@code nil}).</p>
 *
 * @param name       the variable name
 * @param expression the initializer expression, or {@code null} for uninitialized
 * @param line       the 1-based source line of the declaration
 */
public record LetStatement(String name, Expression expression, int line) implements Statement {
}
