package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for an interface declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   interface Name { ... }
 *   interface Name : ParentInterface { ... }   // interface inheritance
 * </pre>
 *
 * <p>An interface declares abstract method signatures — {@code FunctionStatement}
 * nodes with an empty body.  Classes satisfy an interface by defining a method
 * with the same name and parameter count, either in their own body or inherited
 * from a superclass.</p>
 *
 * @param name                  the interface name
 * @param superinterfaceName    the parent interface name, or {@code null} if none
 * @param methods               the abstract method signatures declared in the body
 * @param line                  the 1-based source line of the {@code interface} keyword
 */
public record InterfaceStatement(String name, String superinterfaceName, List<FunctionStatement> methods, int line) implements Statement {

    /** Convenience constructor for a standalone interface. */
    public InterfaceStatement(String name, List<FunctionStatement> methods, int line) {
        this(name, null, methods, line);
    }
}
