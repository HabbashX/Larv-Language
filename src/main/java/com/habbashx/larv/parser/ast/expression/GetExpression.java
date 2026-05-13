package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a field-access (getter) expression.
 *
 * <p>Syntax: {@code object.field}</p>
 *
 * <p>At runtime, {@link #object()} is evaluated to a
 * {@link com.habbashx.larv.runtime.LarvObject} and {@link #field()} is used
 * to retrieve the stored value.</p>
 *
 * @param object the expression that evaluates to the target object
 * @param field  the name of the field to read
 */
public record GetExpression(Expression object, String field) implements Expression {
}
