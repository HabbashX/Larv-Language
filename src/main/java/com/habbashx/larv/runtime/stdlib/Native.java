package com.habbashx.larv.runtime.stdlib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker annotation for standard library implementation classes.
 *
 * <p>Applied at the type level to identify a class as a native (Java-backed)
 * Larv standard library module.  The {@link #value()} attribute holds the
 * import name that Larv programs use to activate the library
 * (e.g. {@code "math"}, {@code "io"}, {@code "string"}).</p>
 *
 * <p>This annotation is informational — it does not affect runtime behavior.
 * The actual registration is performed by each class's {@code registerAll()}
 * method, invoked by
 * {@link com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader}.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 * {@literal @}Native("math")
 * public class NativeMathLibrary implements NativeLibrary {
 *     ...
 * }
 * </pre>
 *
 * @see NativeLibrary
 */
@Target(ElementType.TYPE)
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public @interface Native {
    /**
     * The import name as written in Larv source, e.g. {@code "math"} for
     * {@code import "math"}.
     *
     * @return the stdlib module name
     */
    String value();
}