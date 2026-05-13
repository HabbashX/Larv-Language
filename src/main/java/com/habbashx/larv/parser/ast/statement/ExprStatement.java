package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a statement that consists of a single expression.
 *
 * <p>Used when the value of an expression is not captured — for example, a
 * bare function call whose return value is discarded:
 * {@code doSomething()}</p>
 *
 * <p>The expression is evaluated for its side effects; the result is thrown away.</p>
 *
 * @param value the expression to evaluate
 * @param line  the 1-based source line of the statement
 */
public record ExprStatement(Expression value, int line) implements Statement {
}
