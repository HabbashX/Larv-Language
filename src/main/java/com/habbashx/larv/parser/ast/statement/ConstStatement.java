package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for an immutable constant declaration.
 *
 * <p>Syntax: {@code const NAME = expr}</p>
 *
 * <p>The constant must be initialized — the grammar always requires the {@code =}
 * and the initializer expression.  At runtime, any attempt to reassign a constant
 * throws a {@link com.habbashx.larv.error.LarvError}.</p>
 *
 * @param name  the constant name
 * @param value the initializer expression
 * @param line  the 1-based source line of the declaration
 */
public record ConstStatement(String name , Expression value, int line) implements Statement {
}
