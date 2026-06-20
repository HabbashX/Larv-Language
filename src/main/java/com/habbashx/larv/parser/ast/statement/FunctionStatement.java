package com.habbashx.larv.parser.ast.statement;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * AST node for a named function declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   func name(param1, param2, ...) { ... }
 *   func name(param1, param2, ...) : sync { ... }   // synchronized function
 *   core func name(param1, param2, ...) { ... }     // non-inheritable method
 *   override func name(param1, param2, ...) { ... } // overrides a parent method
 * </pre>
 *
 * @param name     the function name
 * @param params   the parameter names in declaration order
 * @param body     the body statements
 * @param isSync   true if the {@code : sync} modifier was present
 * @param isCore   true if the {@code core} modifier was present (cannot be overridden)
 * @param isOverride true if the {@code override} modifier was present
 * @param line     the 1-based source line of the {@code func} keyword
 */
public record FunctionStatement(
        String name,
        List<Parameter> params,
        List<Statement> body,
        @NotNull String returnType,
        boolean isSync,
        boolean isCore,
        boolean isOverride,
        int line
) implements Statement {

    /** Convenience constructor for plain functions (no modifiers). */
    public FunctionStatement(String name, List<Parameter> params,String returnType, List<Statement> body, int line) {
        this(name, params, body,returnType, false, false, false, line);
    }

    public record Parameter(String name,String type) {}
}
