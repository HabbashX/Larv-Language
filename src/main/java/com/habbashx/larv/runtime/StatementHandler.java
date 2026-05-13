package com.habbashx.larv.runtime;

import com.habbashx.larv.parser.ast.statement.Statement;

/**
 * Executes a single {@link Statement} node.
 *
 * <p>Each implementation owns exactly one statement form's runtime behaviour
 * (e.g. {@code let}, {@code if}, {@code while}).  Being a
 * {@link FunctionalInterface} means every handler can be expressed as a
 * concise method reference with no boilerplate wrapper class.</p>
 *
 * <p>Handlers are stored in the {@link StatementExecutor}'s class-keyed
 * registry and invoked by {@link StatementExecutor#execute(Statement)}.
 * The handler receives the raw (erased) {@link Statement} type {@code T} and
 * is responsible for a single downcast inside its implementation.</p>
 *
 * @param <T> the concrete {@link Statement} subtype handled by this instance
 */
@FunctionalInterface
public interface StatementHandler<T extends Statement> {
    /**
     * Executes the statement, producing side effects on the runtime state.
     *
     * @param statement the concrete statement node to execute
     */
    void handle(T statement);
}
