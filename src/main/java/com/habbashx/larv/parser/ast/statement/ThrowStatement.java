package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a {@code throw} statement.
 *
 * <pre>
 *   throw "Something went wrong"
 *   throw errorCode
 * </pre>
 */
public record ThrowStatement(Expression value, int line) implements Statement {}
