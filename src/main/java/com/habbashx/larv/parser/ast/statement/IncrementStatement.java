package com.habbashx.larv.parser.ast.statement;

/**
 * AST node for a post-increment shorthand statement.
 *
 * <p>Syntax: {@code name++}</p>
 *
 * <p>Equivalent to {@code name = name + 1}.  The variable must already be
 * declared and hold a numeric value.</p>
 *
 * @param name the variable to increment
 * @param line the 1-based source line of the statement
 */
public record IncrementStatement(String name, int line) implements Statement {
}
