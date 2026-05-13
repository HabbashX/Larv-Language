package com.habbashx.larv.parser.ast.statement;

/**
 * AST node for a {@code break} statement.
 *
 * <p>Exits the nearest enclosing {@code while} or {@code for} loop by
 * throwing a {@link com.habbashx.larv.signal.BreakSignal}, which is caught
 * by {@link com.habbashx.larv.runtime.LoopExecutor}.</p>
 *
 * @param line the 1-based source line of the {@code break} keyword
 */
public record BreakStatement(int line) implements Statement {
}
