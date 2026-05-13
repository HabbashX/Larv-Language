package com.habbashx.larv.parser.rule;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * Extends an already-parsed left-hand {@link Expression} when a particular
 * infix or postfix token is encountered.
 *
 * <p>Examples: {@code (} turns the left side into a {@code CallExpression},
 * {@code .} into a {@code GetExpression}, {@code [} into an
 * {@code IndexExpression}, and binary operators ({@code +}, {@code ==}, etc.)
 * into a {@code BinaryExpression}.</p>
 *
 * <p>The operator token has already been consumed before {@link #parse(Expression)}
 * is called. Implementations receive the left-hand expression as an argument.</p>
 */
@FunctionalInterface
public interface InfixRule {
    Expression parse(Expression left);
}