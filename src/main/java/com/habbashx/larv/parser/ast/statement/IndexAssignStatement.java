package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for an array element assignment statement.
 *
 * <p>Syntax: {@code target[index] = value}</p>
 *
 * <p>{@link #target()} must evaluate to a {@code java.util.List},
 * {@link #index()} to a numeric index, and {@link #value()} to the new element
 * value.  Out-of-bounds indices throw a
 * {@link com.habbashx.larv.error.LarvError}.</p>
 *
 * @param target the expression that evaluates to the array
 * @param index  the expression that evaluates to the numeric index
 * @param value  the expression whose result replaces the element
 * @param line   the 1-based source line of the assignment
 */
public record IndexAssignStatement(Expression target, Expression index , Expression value, int line) implements Statement {
}
