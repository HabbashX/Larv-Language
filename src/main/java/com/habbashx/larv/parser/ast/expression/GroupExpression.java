package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for a parenthesised expression {@code (expr)}.
 *
 * <p>Grouping does not change the runtime value — it exists solely to
 * influence operator precedence during parsing.  The evaluator simply
 * unwraps the inner expression.</p>
 *
 * @param expression the expression enclosed in parentheses
 */
public record GroupExpression(Expression expression) implements Expression {
}
