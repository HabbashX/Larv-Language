package com.habbashx.larv.compiler.stdlib.libs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class JsonLib implements LarvStdlib {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectMapper PRETTY =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Contract(pure = true)
    @Override public @NotNull String name() { return "json"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("jsonStringify", "jsonStringify"),
                Map.entry("jsonPretty",    "jsonPretty"),
                Map.entry("jsonParse",     "jsonParse"),
                Map.entry("jsonGet",       "jsonGet"),
                Map.entry("jsonHas",       "jsonHas"),
                Map.entry("jsonIsValid",   "jsonIsValid")
        );
    }

    public static @Unmodifiable Object json_jsonStringify(Object value) {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception e) { throw new LarvRuntimeException("jsonStringify(): " + e.getMessage()); }
    }

    public static @Unmodifiable Object json_jsonPretty(Object value) {
        try { return PRETTY.writeValueAsString(value); }
        catch (Exception e) { throw new LarvRuntimeException("jsonPretty(): " + e.getMessage()); }
    }

    public static Object json_jsonParse(Object json) {
        try {
            Object parsed = MAPPER.readValue(str(json, "jsonParse"), Object.class);
            return normalize(parsed);
        } catch (Exception e) { throw new LarvRuntimeException("jsonParse(): " + e.getMessage()); }
    }

    @Contract("null, _ -> fail")
    @SuppressWarnings("unchecked")
    public static Object json_jsonGet(Object map, Object key) {
        if (!(map instanceof Map<?,?> m))
            throw new LarvRuntimeException("jsonGet(): first argument must be a map");
        return ((Map<String, Object>) m).get(str(key, "jsonGet"));
    }

    @Contract("null, _ -> fail")
    @SuppressWarnings("unchecked")
    public static @NotNull @Unmodifiable Object json_jsonHas(Object map, Object key) {
        if (!(map instanceof Map<?,?> m))
            throw new LarvRuntimeException("jsonHas(): first argument must be a map");
        return ((Map<String, Object>) m).containsKey(str(key, "jsonHas"));
    }

    public static @NotNull Object json_jsonIsValid(Object json) {
        try { MAPPER.readTree(str(json, "jsonIsValid")); return true; }
        catch (Exception e) { return false; }
    }

    @Contract("null -> null")
    @SuppressWarnings("unchecked")
    private static Object normalize(Object v) {
        if (v == null)              return null;
        if (v instanceof Integer i) return (double) i;
        if (v instanceof Long l)    return (double) l;
        if (v instanceof Float f)   return (double) f;
        if (v instanceof Double)    return v;
        if (v instanceof Boolean)   return v;
        if (v instanceof String)    return v;
        if (v instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object o : list) result.add(normalize(o));
            return result;
        }
        if (v instanceof Map<?,?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (var e : ((Map<String, Object>) map).entrySet())
                result.put(e.getKey(), normalize(e.getValue()));
            return result;
        }
        return v.toString();
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s))
            throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}