package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.util.*;

public final class PropertiesLib implements LarvStdlib {

    private static final ThreadLocal<Properties> LOADED =
            ThreadLocal.withInitial(Properties::new);

    @Contract(pure = true)
    @Override public @NotNull String name() { return "properties"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("loadProp",     "properties_loadProp"),
                Map.entry("getProp",      "properties_getProp"),
                Map.entry("getPropOr",    "properties_getPropOr"),
                Map.entry("setProp",      "properties_setProp"),
                Map.entry("hasProp",      "properties_hasProp"),
                Map.entry("removeProp",   "properties_removeProp"),
                Map.entry("getAllProps",     "properties_allProps"),
                Map.entry("saveProps",    "properties_saveProps"),
                Map.entry("loadPropsMap", "properties_loadPropsMap")
        );
    }

    public static @Nullable Object properties_loadProp(Object file) {
        try (FileInputStream fis = new FileInputStream(str(file, "loadProp"))) {
            Properties p = new Properties();
            p.load(fis);
            LOADED.set(p);
            return null;
        } catch (IOException e) {
            throw new LarvRuntimeException("loadProp(): " + e.getMessage());
        }
    }

    public static @NotNull Object properties_getProp(Object key) {
        String k = str(key, "getProp");
        String v = LOADED.get().getProperty(k);
        if (v == null) throw new LarvRuntimeException("getProp(): key not found: " + k);
        return v;
    }

    public static Object properties_getPropOr(Object key, Object fallback) {
        String v = LOADED.get().getProperty(str(key, "getPropOr"));
        return v != null ? v : fallback;
    }

    public static @Nullable Object properties_setProp(Object key, Object value) {
        LOADED.get().setProperty(str(key, "setProp"), str(value, "setProp"));
        return null;
    }

    public static @NotNull @Unmodifiable Object properties_hasProp(Object key) {
        return LOADED.get().containsKey(str(key, "hasProp"));
    }

    public static @NotNull Object properties_removeProp(Object key) {
        return LOADED.get().remove(str(key, "removeProp")) != null;
    }

    public static @NotNull Object properties_allProps() {
        Map<String, Object> result = new LinkedHashMap<>();
        LOADED.get().forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
        return result;
    }

    public static @Nullable Object properties_saveProps(Object file) {
        try (FileOutputStream fos = new FileOutputStream(str(file, "saveProps"))) {
            LOADED.get().store(fos, null);
            return null;
        } catch (IOException e) {
            throw new LarvRuntimeException("saveProps(): " + e.getMessage());
        }
    }

    public static @NotNull Object properties_loadPropsMap(Object file) {
        try (FileInputStream fis = new FileInputStream(str(file, "loadPropsMap"))) {
            Properties p = new Properties();
            p.load(fis);
            Map<String, Object> result = new LinkedHashMap<>();
            p.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
            return result;
        } catch (IOException e) {
            throw new LarvRuntimeException("loadPropsMap(): " + e.getMessage());
        }
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s))
            throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}