package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for compound assignment operators: {@code +=}, {@code -=},
 * {@code *=}, {@code /=}.
 *
 * <p>Syntax:</p>
 * <pre>
 *   x += 5    // x = x + 5
 *   x -= 2    // x = x - 2
 *   x *= 3    // x = x * 3
 *   x /= 4    // x = x / 4
 * </pre>
 *
 * @param name     the variable being updated
 * @param operator the operator string: {@code "+"}, {@code "-"}, {@code "*"}, {@code "/"}
 * @param value    the right-hand side expression
 * @param line     the 1-based source line
 */
public record CompoundAssignStatement(String name, String operator, Expression value, int line)
        implements Statement {}
