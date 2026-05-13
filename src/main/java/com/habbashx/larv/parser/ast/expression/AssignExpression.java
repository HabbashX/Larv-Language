package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing an in-expression variable assignment.
 *
 * <p>Unlike {@link com.habbashx.larv.parser.ast.statement.AssignStatement},
 * this node appears inside a larger expression context, allowing patterns like
 * {@code print(x = 5)}.  At runtime, the assignment is performed and the
 * assigned value is returned as the expression result.</p>
 *
 * @param name  the target variable name
 * @param value the expression whose result will be assigned to {@code name}
 */
public record AssignExpression(String name, Expression value) implements Expression {
}
