package com.habbashx.larv.runtime.registry;


import com.habbashx.larv.runtime.ExecutionContext;
import com.habbashx.larv.runtime.BinaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Registers the core built-in functions that are always available in every
 * Larv program, regardless of any {@code import} statements.
 *
 * <p>Adding a new always-available built-in requires exactly one step:
 * add a private method and register it in {@link #registerAll()}.
 * Nothing else needs to change.</p>
 *
 * <h2>Currently registered functions</h2>
 * <ul>
 *   <li>{@code print(value)} — prints a value to {@code stdout} with a newline.
 *       Whole-number doubles are printed without the decimal point.</li>
 *   <li>{@code input()}      — reads one line from {@code stdin} and returns it
 *       as a string.</li>
 *   <li>{@code len(array)}   — returns the size of an array as a number.</li>
 * </ul>
 *
 * <p>Standard library functions (math, io, string, …) are loaded on demand by
 * {@link com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader}.</p>
 */
public class NativeRegistry {

    private final ExecutionContext context;

    /**
     * Shared reader for {@code input()} — reused across calls to avoid
     * closing and reopening {@code System.in}.
     */
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Constructs a {@code NativeRegistry} for the given execution context.
     *
     * @param context the runtime context in which built-ins will be registered
     */
    public NativeRegistry(ExecutionContext context) {
        this.context = context;
    }

    /**
     * Registers all core built-in functions into the execution context.
     *
     * <p>Called once by {@link com.habbashx.larv.runtime.Interpreter} during
     * construction.</p>
     */
    public void registerAll() {
        context.registerNative("print",   this::nativePrint);
        context.registerNative("input", this::nativeInput);
        context.registerNative("len",    this::nativeLen);
    }

    /**
     * {@code print(value)} — prints the string representation of the first
     * argument to {@code stdout}, followed by a newline.
     *
     * @param args a single-element list containing the value to print
     * @return {@code null}
     */
    private @Nullable Object nativePrint(@NotNull List<Object> args) {
        System.out.println(BinaryOperator.stringify(args.getFirst()));
        return null;
    }

    /**
     * {@code input()} — reads one line from {@code stdin}.
     *
     * @param args ignored
     * @return the line read, or {@code null} on EOF
     */
    private @Unmodifiable Object nativeInput(List<Object> args) {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("input failed: " + e.getMessage(), e);
        }
    }

    /**
     * {@code len(array)} — returns the number of elements in an array.
     *
     * @param args a single-element list where the first element must be a {@code List}
     * @return the size as a {@code Double}
     * @throws RuntimeException if the argument is not a list
     */
    private @NotNull @Unmodifiable Object nativeLen(@NotNull List<Object> args) {
        Object value = args.getFirst();
        if (value instanceof List<?> list) return list.size();
        throw new RuntimeException("len() expects an array, got: " + value.getClass().getSimpleName());
    }
}
