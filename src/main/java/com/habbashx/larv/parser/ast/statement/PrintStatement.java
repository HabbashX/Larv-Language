package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a {@code print} statement.
 *
 * <p>Syntax: {@code print expr}</p>
 *
 * <p>Evaluates {@link #value()} and writes its string representation
 * to {@code System.out} followed by a newline.  Numeric values that are
 * whole numbers are printed without a decimal point (e.g. {@code 42}
 * rather than {@code 42.0}).</p>
 *
 * @param value the expression to evaluate and print
 * @param line  the 1-based source line of the statement
 */
public record PrintStatement(Expression value, int line) implements Statement {
}
