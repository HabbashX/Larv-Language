package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for a class declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   class ClassName { ... }
 *   class ChildClass : ParentClass { ... }                    // inheritance
 *   class ClassName implements I1, I2 { ... }                 // interfaces
 *   class ChildClass : ParentClass implements I1, I2 { ... }
 * </pre>
 *
 * <p>When executed, the class is registered in the
 * {@link com.habbashx.larv.runtime.ExecutionContext} by name.  The body
 * statements are stored as-is; they are inspected when {@code new ClassName()}
 * is evaluated to collect method declarations.</p>
 *
 * <p>The special method {@code constructor} acts as the constructor: it is called
 * automatically during {@code new} with the provided arguments.</p>
 *
 * @param name           the class name
 * @param superclassName the name of the parent class, or {@code null} if none
 * @param interfaces     the interface names this class declares it implements
 * @param body           the statements inside the class body
 * @param line           the 1-based source line of the {@code class} keyword
 */
public record ClassStatement(String name, String superclassName, List<String> interfaces, List<Statement> body, int line) implements Statement {

    /** Convenience constructor for a class with no superclass. */
    public ClassStatement(String name, List<Statement> body, int line) {
        this(name, null, List.of(), body, line);
    }
}
