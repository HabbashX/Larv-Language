package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a field-assignment (setter) expression.
 *
 * <p>Syntax: {@code object.field = value}</p>
 *
 * <p>At runtime, {@link #object()} is evaluated to a
 * {@link com.habbashx.larv.runtime.LarvObject}, the {@link #value()} expression
 * is evaluated, and the result is stored under {@link #field()}.
 * The expression evaluates to {@code nil}.</p>
 *
 * @param object the expression that evaluates to the target object
 * @param field  the name of the field to write
 * @param value  the expression whose result will be stored in the field
 */
public record SetExpression(Expression object, String field, Expression value) implements Expression {
}
