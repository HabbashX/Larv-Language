package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a class declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   class ClassName {
 *       func init(params) { ... }
 *       func methodName(params) { ... }
 *       ...
 *   }
 * </pre>
 *
 * <p>When executed, the class is registered in the
 * {@link com.habbashx.larv.runtime.ExecutionContext} by name.  The body
 * statements are stored as-is; they are inspected when {@code new ClassName()}
 * is evaluated to collect method declarations.</p>
 *
 * <p>The special method {@code init} acts as the constructor: it is called
 * automatically during {@code new} with the provided arguments.</p>
 *
 * @param name the class name
 * @param body the statements inside the class body (typically function declarations)
 * @param line the 1-based source line of the {@code class} keyword
 */
public record ClassStatement(String name, List<Statement> body, int line) implements Statement {
}
