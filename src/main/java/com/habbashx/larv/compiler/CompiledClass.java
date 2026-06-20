package com.habbashx.larv.compiler;

/**
 * A single compiled JVM class produced by the {@link LarvCompiler}.
 *
 * @param name     fully-qualified class name (dot-separated), e.g. {@code "MyScript"}
 *                 or {@code "MyScript$Point"}
 * @param bytecode the raw {@code .class} file bytes
 */
public record CompiledClass(String name, byte[] bytecode) {}