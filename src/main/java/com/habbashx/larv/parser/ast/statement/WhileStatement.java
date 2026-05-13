package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

import java.util.List;

/**
 * AST node for a {@code while} loop.
 *
 * <p>Syntax:</p>
 * <pre>
 *   while condition {
 *       ...
 *   }
 * </pre>
 *
 * <p>The loop runs as long as {@link #condition()} is truthy.  {@code break}
 * and {@code continue} are handled via
 * {@link com.habbashx.larv.signal.BreakSignal} /
 * {@link com.habbashx.larv.signal.ContinueSignal} exceptions caught by
 * {@link com.habbashx.larv.runtime.LoopExecutor}.</p>
 *
 * @param condition the loop guard expression
 * @param body      statements to execute each iteration
 * @param line      the 1-based source line of the {@code while} keyword
 */
public record WhileStatement(Expression condition, List<Statement> body, int line) implements Statement {}
