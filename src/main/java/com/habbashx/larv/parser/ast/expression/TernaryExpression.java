package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for the ternary conditional expression.
 *
 * <p>Syntax: {@code condition ? thenExpr, elseExpr}</p>
 *
 * <p>Evaluates {@code condition}; if truthy returns {@code thenExpr},
 * otherwise returns {@code elseExpr}. Both branches are lazily evaluated —
 * only the chosen branch is executed.</p>
 *
 * @param condition  the boolean guard expression
 * @param thenBranch value produced when condition is truthy
 * @param elseBranch value produced when condition is falsy
 */
public record TernaryExpression(
        Expression condition,
        Expression thenBranch,
        Expression elseBranch
) implements Expression {}
