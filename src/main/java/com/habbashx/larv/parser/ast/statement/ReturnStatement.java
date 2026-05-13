package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a {@code return} statement.
 *
 * <p>Syntax: {@code return expr}</p>
 *
 * <p>At runtime the expression is evaluated and the result is wrapped in a
 * {@link com.habbashx.larv.signal.ReturnSignal} exception that unwinds the
 * call stack to the nearest {@link com.habbashx.larv.runtime.FunctionInvoker},
 * which catches it and returns the value.</p>
 *
 * @param value the expression whose result is the return value
 * @param line  the 1-based source line of the {@code return} keyword
 */
public record ReturnStatement(Expression value, int line) implements Statement {}
