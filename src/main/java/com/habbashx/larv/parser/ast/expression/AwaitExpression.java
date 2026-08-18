package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for the {@code await} prefix expression.
 *
 * <p>Syntax: {@code await expr}.  If {@code expr} evaluates to an async
 * future (the result of calling an {@code async} function), evaluation
 * blocks until the future completes and yields its value.  Any other
 * value passes through unchanged.</p>
 *
 * @param expression the awaited expression (typically an async function call)
 */
public record AwaitExpression(Expression expression) implements Expression {}