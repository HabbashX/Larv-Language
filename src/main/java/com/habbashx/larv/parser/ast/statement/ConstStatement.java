package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for an immutable constant declaration.
 *
 * <p>Syntax: {@code const NAME = expr} or, inside a class body,
 * {@code const NAME = expr : get}</p>
 *
 * <p>Constants can only declare a getter (never a setter, since they are immutable).</p>
 *
 * @param name      the constant name
 * @param value     the initializer expression
 * @param hasGetter whether a public getter method should be generated (class fields only)
 * @param line      the 1-based source line of the declaration
 */
public record ConstStatement(String name,String type, Expression value, boolean hasGetter, int line) implements Statement {
    /** Convenience constructor for declarations without accessor annotations. */
    public ConstStatement(String name,String type, Expression value, int line) {
        this(name, type,value, false, line);
    }
}
