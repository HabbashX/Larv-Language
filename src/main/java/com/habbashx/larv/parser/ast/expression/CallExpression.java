package com.habbashx.larv.parser.ast.expression;

import java.util.List;

/**
 * AST node for a function or method call.
 *
 * <p>The {@link #caller()} can be:</p>
 * <ul>
 *   <li>A {@link VarExpression} — plain function call, e.g. {@code foo(1, 2)}</li>
 *   <li>A {@link GetExpression} — method call, e.g. {@code obj.method(arg)}</li>
 * </ul>
 *
 * <p>The evaluator inspects the caller type to decide whether to look up the
 * function in the global registry or dispatch to an object method.</p>
 *
 * @param caller    the expression that resolves to the callable target
 * @param arguments the evaluated argument list (in source order)
 */
public record CallExpression(Expression caller, List<Expression> arguments) implements Expression {
}
