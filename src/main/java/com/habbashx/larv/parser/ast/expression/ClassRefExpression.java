package com.habbashx.larv.parser.ast.expression;

/**
 * AST node representing a bare class name used as an expression value.
 *
 * <p>Allows a class to be referenced by name (e.g. to pass it as an argument
 * or store it in a variable) without immediately constructing an instance.</p>
 *
 * @param name the class name as written in source
 */
public record ClassRefExpression(String name) implements Expression {
}
