package com.habbashx.larv.runtime.stdlib.loader;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import com.habbashx.larv.runtime.stdlib.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves an import name to the correct standard library and registers it.
 *
 * Design:
 *  - Each library is instantiated at most ONCE (lazy singleton per loader instance).
 *  - Calling `import math` twice is a no-op the second time.
 *  - The HttpLibrary instance (which holds an HttpClient) is reused across calls.
 *  - Adding a new library only requires one line in the registry map.
 *
 * Available libraries:
 *   math    — sqrt, pow, sin, cos, random, pi, ...
 *   io      — readFile, writeFile, readLines, readBytes, ...
 *   string  — strLen, strUpper, strSplit, strJoin, ...
 *   list    — listNew, listAdd, listRemove, listSort, ...
 *   map     — mapNew, mapSet, mapGet, mapKeys, ...
 *   json    — jsonParse, jsonStringify, jsonPretty
 *   http    — httpGet, httpPost, httpPostJson, httpPut, httpDelete, httpRequest
 *   system  — exit, getEnv, clock, sleep, exec, osName, ...
 */
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeLibraryLoader {

    private static final Set<String> KNOWN = Set.of(
            "math", "io", "string", "list", "map", "json", "http", "system","date","regex","converter","base64","properties"
    );

    private final ExecutionContext context;

    /** Tracks which libraries have already been loaded — no re-registration. */
    private final Set<String> loaded = new java.util.HashSet<>();

    /**
     * Lazy singleton factories — each supplier is called at most once.
     * The created instance is cached in {@link #instances}.
     */
    private final Map<String, Supplier<NativeLibrary>> registry;

    /** Cached library instances — built on first import, reused on subsequent ones. */
    private final Map<String, NativeLibrary> instances = new HashMap<>();

    public NativeLibraryLoader(ExecutionContext context) {
        this.context = context;
        registry = new HashMap<>();
        registry.put("math",   () -> new NativeMathLibrary(context));
        registry.put("io",     () -> new NativeIoLibrary(context));
        registry.put("string", () -> new NativeStringLibrary(context));
        registry.put("list",   () -> new NativeListLibrary(context));
        registry.put("map",    () -> new NativeMapLibrary(context));
        registry.put("http",   () -> new NativeHttpLibrary(context));
        registry.put("system", () -> new NativeSystemLibrary(context));
        registry.put("date", () -> new NativeDateLibrary(context));
        registry.put("base64",() -> new NativeEncodeLibrary(context));
        registry.put("regex",() -> new NativeRegexLibrary(context));
        registry.put("converter",() -> new NativeConvertLibrary(context));
        registry.put("properties",() -> new NativePropertiesLibrary(context));
        registry.put("jdbc",() -> new NativeJdbcLibrary(context));
        registry.put("json",() -> new NativeJsonLibrary(context));
    }

    /**
     * Load and register a standard library by name.
     * Safe to call multiple times — subsequent calls for the same library are ignored.
     *
     * @param name the library name exactly as written after the {@code import} keyword
     */
    public void load(@NotNull String name) {
        if (loaded.contains(name)) return;

        Supplier<NativeLibrary> factory = registry.get(name);
        if (factory == null) {
            throw new LarvError(
                    "Unknown library '" + name + "'. Available: " +
                            String.join(", ", KNOWN.stream().sorted().toList()),
                    -1, LarvError.Kind.RUNTIME
            );
        }

        NativeLibrary lib = instances.computeIfAbsent(name, k -> factory.get());
        lib.registerAll();
        loaded.add(name);
    }


    public ExecutionContext getContext() {
        return context;
    }
}