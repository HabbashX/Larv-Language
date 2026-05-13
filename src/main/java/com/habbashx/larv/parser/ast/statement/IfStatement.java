package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

import java.util.List;

/**
 * AST node for an {@code if} / {@code else} conditional statement.
 *
 * <p>Syntax:</p>
 * <pre>
 *   if condition {
 *       ...
 *   } else {
 *       ...
 *   }
 * </pre>
 *
 * <p>The {@link #elseBranch()} is an empty list when there is no {@code else}
 * clause.  Truthiness of the condition is evaluated by
 * {@link com.habbashx.larv.runtime.TruthinessEvaluator}.</p>
 *
 * @param condition  the boolean-like guard expression
 * @param thenBranch statements executed when condition is truthy
 * @param elseBranch statements executed when condition is falsy (may be empty)
 * @param line       the 1-based source line of the {@code if} keyword
 */
public record IfStatement(Expression condition , List<Statement> thenBranch , List<Statement> elseBranch, int line) implements Statement {
}
