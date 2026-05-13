package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing a numeric literal.
 *
 * <p>All Larv numbers are stored as {@code double} at parse time.
 * This avoids constant {@code int}↔{@code double} coercions at runtime and
 * keeps arithmetic consistent with values returned from Java FFI calls, which
 * are also normalised to {@code Double}.</p>
 *
 * <p>Examples: {@code 0}, {@code 42}, {@code 3.14}, {@code -0.001}</p>
 *
 * @param value the numeric value parsed from the source
 */
public record NumberExpression(double value) implements Expression {}
