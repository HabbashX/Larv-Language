package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

import java.util.List;

/**
 * AST node for a for-each iteration loop.
 *
 * <p>Syntax:</p>
 * <pre>
 *   for variable in iterable {
 *       ...
 *   }
 * </pre>
 *
 * <p>The {@link #iterable()} expression must evaluate to a
 * {@code java.util.List}.  On each iteration, the current element is bound
 * to {@link #variable()} in a fresh inner scope.</p>
 *
 * @param variable the loop variable name
 * @param iterable the expression that yields the collection to iterate
 * @param body     statements to execute for each element
 * @param line     the 1-based source line of the {@code for} keyword
 */
public record ForeachStatement(
        String variable,
        Expression iterable,
        List<Statement> body,
        int line
) implements Statement {}
