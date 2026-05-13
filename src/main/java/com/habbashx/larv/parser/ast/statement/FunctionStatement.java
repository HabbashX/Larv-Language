package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a named function declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   func name(param1, param2, ...) {
 *       ...
 *   }
 * </pre>
 *
 * <p>When executed, the function is registered by name in the
 * {@link com.habbashx.larv.runtime.ExecutionContext} without running the body.
 * The body runs only when the function is called.</p>
 *
 * @param name   the function name
 * @param params the parameter names in declaration order
 * @param body   the body statements
 * @param line   the 1-based source line of the {@code func} keyword
 */
public record FunctionStatement(String name, List<String> params, List<Statement> body, int line) implements Statement {}
