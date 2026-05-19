package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a try / catch / finally block.
 *
 * <pre>
 *   try {
 *       ...
 *   } catch (e) {
 *       ...
 *   } finally {
 *       ...
 *   }
 * </pre>
 *
 * {@code catchVar}      — the variable name that receives the caught value (e.g. {@code "e"}).
 * {@code catchBody}     — may be empty if there is no {@code catch} clause.
 * {@code finallyBody}   — may be empty if there is no {@code finally} clause.
 * At least one of {@code catchBody} or {@code finallyBody} must be non-empty
 * (enforced by the parser).
 */
public record TryCatchStatement(
        List<Statement> tryBody,
        String          catchVar,
        List<Statement> catchBody,
        List<Statement> finallyBody,
        int             line
) implements Statement {}
