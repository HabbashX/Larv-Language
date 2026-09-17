package com.habbashx.larv.parser.ast.expression;

/**
 * AST node for null-safe field access: {@code obj?.field}.
 *
 * <p>Evaluates {@code object}; when it is {@code nil} the whole expression
 * is {@code nil} without touching the field, otherwise it behaves exactly
 * like {@link GetExpression}.  Also serves as the receiver of safe method
 * calls: {@code obj?.method(args)} parses as a {@link CallExpression} whose
 * caller is this node.</p>
 *
 * @param object the receiver expression
 * @param field  the field name
 */
public record SafeGetExpression(Expression object, String field) implements Expression {
}
