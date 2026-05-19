package com.habbashx.larv.runtime.stdlib;

/**
 * Contract implemented by every Larv standard library module.
 *
 * <p>Each implementation registers one cohesive set of native functions into
 * the {@link com.habbashx.larv.runtime.ExecutionContext} by calling
 * {@link com.habbashx.larv.runtime.ExecutionContext#registerNative} once per
 * function inside {@link #registerAll()}.</p>
 *
 * <p>Implementations are loaded on demand by
 * {@link com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader} when
 * a Larv program executes {@code import "libname"}.  Each library is
 * instantiated at most once per interpreter run.</p>
 *
 * <h2>Implementing a new library</h2>
 * <pre>
 * public class NativeFooLibrary implements NativeLibrary {
 *     private final ExecutionContext context;
 *
 *     public NativeFooLibrary(ExecutionContext context) {
 *         this.context = context;
 *     }
 *
 *     {@literal @}Override
 *     public void registerAll() {
 *         context.registerNative("fooBar", this::fooBar);
 *     }
 *
 *     private Object fooBar(List{@literal <}Object{@literal >} args) { ... }
 * }
 * </pre>
 *
 * Then add one line to
 * {@link com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader}'s
 * constructor registry.
 *
 * @see Native
 */
public interface NativeLibrary {

    /**
     * Registers all functions provided by this library into the runtime context.
     *
     * <p>Called exactly once per interpreter run by
     * {@link com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader#load(String)}.
     * Subsequent {@code import} statements for the same library are no-ops.</p>
     */
    void registerAll();
}