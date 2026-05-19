package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a named module (namespace) declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   module MyMath {
 *       func add(x, y) { return x + y }
 *       func sqrt(x)   { return x * x }
 *   }
 * </pre>
 *
 * <p>A module groups functions and constants under a name.  Members are
 * accessed with dot notation after the module is in scope:</p>
 * <pre>
 *   print MyMath.add(2, 3)   // 5
 * </pre>
 *
 * @param name  the module identifier
 * @param body  the declarations inside the module block
 * @param line  the 1-based source line of the {@code module} keyword
 */
public record ModuleStatement(String name, List<Statement> body, int line) implements Statement {}
