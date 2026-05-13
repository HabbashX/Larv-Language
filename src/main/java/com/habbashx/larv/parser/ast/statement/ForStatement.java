package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

import java.util.List;

/**
 * AST node for a traditional C-style {@code for} loop.
 *
 * <p>Syntax:</p>
 * <pre>
 *   for init; condition; increment {
 *       ...
 *   }
 * </pre>
 *
 * <p>Execution order: {@link #init()} once, then repeat:
 * evaluate {@link #condition()}, if truthy execute {@link #body()},
 * then execute {@link #increment()}.  Break/continue are propagated via
 * {@link com.habbashx.larv.runtime.LoopExecutor}.</p>
 *
 * @param init      the initialization statement (executed once before the loop)
 * @param condition the loop guard expression
 * @param increment the post-iteration statement (e.g. {@code i++})
 * @param body      the loop body
 * @param line      the 1-based source line of the {@code for} keyword
 */
public record ForStatement(Statement init, Expression condition, Statement increment, List<Statement> body, int line) implements Statement {}
