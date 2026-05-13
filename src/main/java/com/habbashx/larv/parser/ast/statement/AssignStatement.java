package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a standalone variable assignment statement.
 *
 * <p>Syntax: {@code name = expr}</p>
 *
 * <p>The variable must already exist in scope (declared with {@code var} or
 * {@code const}).  If it does not, a runtime
 * {@link com.habbashx.larv.error.LarvError} is thrown.  Assigning to a
 * {@code const} also throws.</p>
 *
 * @param name  the name of the variable to update
 * @param value the expression whose result replaces the current value
 * @param line  the 1-based source line of the assignment
 */
public record AssignStatement(String name , Expression value, int line) implements Statement {
}
