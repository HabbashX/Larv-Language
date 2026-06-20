package com.habbashx.larv.compiler.stdlib;

import java.util.Map;

/**
 * Every standard library implements this interface.
 *
 * <p>The compiler discovers libs automatically — no manual registry needed.
 * Just create a new class in the {@code stdlib/libs} package, implement this
 * interface, and register it in {@link LarvStdlibLoader}. That's it.</p>
 */
public interface LarvStdlib {

    /** The import name users write: {@code import "math"}, {@code import "io"}, etc. */
    String name();

    /**
     * All functions this lib exposes.
     * Key   = function name as called from Larv (e.g. {@code "sqrt"})
     * Value = the static method name on this class (e.g. {@code "math_sqrt"})
     */
    Map<String, String> functions();

    /**
     * The fully-qualified internal JVM class name for {@code invokestatic}.
     * Default: derived from the concrete class automatically.
     */
    default String internalName() {
        return getClass().getName().replace('.', '/');
    }
}