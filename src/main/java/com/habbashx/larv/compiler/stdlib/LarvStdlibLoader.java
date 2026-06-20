package com.habbashx.larv.compiler.stdlib;

import com.habbashx.larv.compiler.stdlib.libs.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Loads and indexes all standard libraries.
 *
 * <p>To add a new stdlib: create your class in {@code stdlib/libs/}, implement
 * {@link LarvStdlib}, then add one line here in {@link #allLibs()}. Nothing
 * else needs to change anywhere.</p>
 */
public final class LarvStdlibLoader {

    /** Maps function name -> [libName, jvmMethodName, internalClassName] */
    public static final Map<String, String[]> REGISTRY;

    /** Maps lib name -> set of exported function names */
    public static final Map<String, Set<String>> LIB_FUNCTIONS;

    /** All known lib names */
    public static final Set<String> KNOWN_LIBS;

    static {
        Map<String, String[]> registry = new LinkedHashMap<>();
        Map<String, Set<String>> libFunctions = new LinkedHashMap<>();
        Set<String> knownLibs = new LinkedHashSet<>();

        for (final LarvStdlib lib : allLibs()) {
            String libName = lib.name();
            knownLibs.add(libName);
            libFunctions.put(libName, new LinkedHashSet<>());

            for (final var entry : lib.functions().entrySet()) {
                String fnName  = entry.getKey();
                String jvmName = entry.getValue();

                String registryKey = libName.equals("converter")
                        ? "converter." + fnName
                        : fnName;

                registry.put(registryKey, new String[]{ libName, jvmName, lib.internalName() });
                libFunctions.get(libName).add(fnName);
            }
        }

        REGISTRY = Collections.unmodifiableMap(registry);
        LIB_FUNCTIONS = Collections.unmodifiableMap(libFunctions);
        KNOWN_LIBS = Collections.unmodifiableSet(knownLibs);
    }

    /**
     * The single place where libs are registered.
     * Add one line per new library. Order doesn't matter.
     */
    public static @NotNull @Unmodifiable List<LarvStdlib> allLibs() {
        return List.of(
                new MathLib(),
                new IoLib(),
                new StringLib(),
                new JdbcLib(),
                new ServerSocketLib(),
                new ThreadLib(),
                new PathLib(),
                new JsonLib(),
                new HttpLib(),
                new SystemLib(),
                new DateLib(),
                new Base64Lib(),
                new RegexLib(),
                new SocketLib(),
                new PropertiesLib()
        );
    }

    @Contract(pure = true)
    private LarvStdlibLoader() {}
}