package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

import java.util.List;

/**
 * AST node for a {@code switch} statement.
 *
 * <pre>
 *   switch expr {
 *       case "ok"    : { ... }
 *       case "error" : { ... }
 *       default      : { ... }
 *   }
 * </pre>
 *
 * Each {@link SwitchCase} holds a list of match values (supporting
 * fall-through via multiple values on one case) and a body block.
 * {@code defaultBody} is empty when no {@code default} clause is present.
 */
public record SwitchStatement(
        Expression        subject,
        List<SwitchCase>  cases,
        List<Statement>   defaultBody,
        int               line
) implements Statement {

    /**
     * One {@code case} arm.
     *
     * @param values  one or more literal/expression values that trigger this arm
     * @param body    statements to execute when matched
     */
    public record SwitchCase(List<Expression> values, List<Statement> body) {}
}
