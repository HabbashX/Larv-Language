package com.habbashx.larv.parser.ast.expression;

import java.util.List;

/**
 * AST node representing a Java Foreign Function Interface (FFI) call.
 *
 * <p>Syntax: {@code Alias.methodName(arg1, arg2, ...)}</p>
 *
 * <p>The {@link #alias()} must have been previously bound via an
 * {@link com.habbashx.larv.parser.ast.statement.JavaBindStatement}.
 * At runtime the interpreter resolves the alias to a real Java {@link Class}
 * (or instance) via
 * {@link com.habbashx.larv.runtime.ffi.JavaClassRegistry} and invokes the
 * named method using reflection + {@code MethodHandle} caching.</p>
 *
 * <h3>Examples</h3>
 * <pre>
 *   include Math from "java.lang.Math"
 *   print Math.sqrt(9)          // → 3.0
 *
 *   include sc from "java.util.Scanner" involve { "java.lang.System.in" }
 *   var line = sc.nextLine()
 * </pre>
 *
 * @param alias      the short alias bound via {@code include … from "…"}
 * @param methodName the Java method to invoke on the bound class/instance
 * @param arguments  the argument expressions (evaluated left-to-right)
 */
public record JavaCallExpression(
        String alias,
        String methodName,
        List<Expression> arguments
) implements Expression {}
