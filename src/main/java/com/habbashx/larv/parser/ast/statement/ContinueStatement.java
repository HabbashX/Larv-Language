package com.habbashx.larv.parser.ast.statement;

/**
 * AST node for a {@code continue} statement.
 *
 * <p>Skips the remainder of the current loop iteration by throwing a
 * {@link com.habbashx.larv.signal.ContinueSignal}, which is caught by
 * {@link com.habbashx.larv.runtime.LoopExecutor}.  The loop's post-step
 * (for {@code for} loops) and condition check still run normally.</p>
 *
 * @param line the 1-based source line of the {@code continue} keyword
 */
public record ContinueStatement(int line) implements Statement {
}
