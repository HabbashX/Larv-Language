package com.habbashx.larv.parser.ast.expression;

import java.util.List;

/**
 * AST node for a {@code new} object-instantiation expression.
 *
 * <p>Syntax: {@code new ClassName(arg1, arg2, ...)}</p>
 *
 * <p>At runtime, the evaluator looks up {@link #className()} in the class
 * registry, creates a fresh {@link com.habbashx.larv.runtime.LarvObject},
 * copies the class body's method declarations onto the object, and invokes
 * the {@code init} method (if present) with the provided arguments.</p>
 *
 * @param className the name of the class to instantiate
 * @param args      the constructor arguments (evaluated left-to-right)
 */
public record NewExpression(String className, List<Expression> args) implements Expression {
}
