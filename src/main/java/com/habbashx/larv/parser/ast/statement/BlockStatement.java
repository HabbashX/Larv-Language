package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a grouped block of statements.
 *
 * <p>A {@code BlockStatement} introduces a new lexical scope: variables
 * declared inside are not visible outside the block.  Scope management is
 * handled by {@link com.habbashx.larv.runtime.StatementExecutor#runBlock}.</p>
 *
 * @param statements the statements that make up the block
 * @param line       the 1-based source line of the opening {@code {}
 */
public record BlockStatement(List<Statement> statements, int line) implements Statement {}
