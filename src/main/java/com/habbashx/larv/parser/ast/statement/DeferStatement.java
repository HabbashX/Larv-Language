package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;

/**
 * AST node for a {@code defer} statement.
 *
 * <p>The deferred expression (typically a method call like {@code file.close()})
 * is evaluated when the enclosing block or function exits — in LIFO order
 * relative to other defers in the same scope.  This mirrors Go's {@code defer}
 * semantics and is the idiomatic way to auto-close resources in Larv:</p>
 *
 * <pre>
 *   var file = File.open("example.properties")
 *   defer file.close()
 *   // ... use file ...
 *   // file.close() is called automatically when the block exits
 * </pre>
 *
 * @param value the expression to defer (evaluated at scope exit, not at declaration)
 * @param line  1-based source line where the {@code defer} keyword appears
 */
public record DeferStatement(Expression value, int line) implements Statement {}
