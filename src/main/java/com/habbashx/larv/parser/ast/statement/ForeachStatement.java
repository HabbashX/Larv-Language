package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * AST node for a for-each iteration loop.
 *
 * <p>Syntax (list iteration):</p>
 * <pre>
 *   for element in list { ... }
 * </pre>
 *
 * <p>Syntax (map iteration — key/value destructure):</p>
 * <pre>
 *   for key, value in map { ... }
 * </pre>
 *
 * <p>When iterating a {@code java.util.Map}, both {@link #variable()} (key)
 * and {@link #valueVariable()} (value) are bound in each iteration scope.
 * When iterating a {@code java.util.List}, only {@link #variable()} is bound
 * and {@link #valueVariable()} is {@code null}.</p>
 *
 * @param variable      the loop variable name (element for lists, key for maps)
 * @param valueVariable optional second variable name for map values (null for list iteration)
 * @param iterable      the expression that yields the collection to iterate
 * @param body          statements to execute for each element / entry
 * @param line          the 1-based source line of the {@code for} keyword
 */
public record ForeachStatement(
        String variable,
        @Nullable String valueVariable,
        Expression iterable,
        List<Statement> body,
        int line
) implements Statement {}
