package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a field-assignment statement.
 *
 * <p>Syntax: {@code object.field = value}</p>
 *
 * <p>Similar to {@link com.habbashx.larv.parser.ast.expression.SetExpression}
 * but as a standalone statement where the returned value is discarded.</p>
 *
 * @param object the expression that evaluates to the target object
 * @param field  the name of the field to assign
 * @param value  the expression whose result is stored in the field
 * @param line   the 1-based source line of the assignment
 */
public record SetFieldStatement(Expression object ,String field , Expression value, int line) implements Statement {
}
