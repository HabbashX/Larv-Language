package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map standard library — import map
 *
 * Maps are represented as Java LinkedHashMap<String, Object> internally,
 * and exposed to Larv as opaque objects. Keys must be strings.
 *
 *   mapNew()               → map      create empty map
 *   mapSet(map,key,val)    → nil      put or update a key
 *   mapGet(map, key)       → any      get value by key (nil if missing)
 *   mapHas(map, key)       → boolean  true if key exists
 *   mapRemove(map, key)    → nil      remove a key
 *   mapSize(map)           → number   number of entries
 *   mapKeys(map)           → array    all keys
 *   mapValues(map)         → array    all values
 *   mapClear(map)          → nil      remove all entries
 *   mapIsEmpty(map)        → boolean  true if size == 0
 *   mapMerge(a, b)         → map      new map = a merged with b (b wins)
 *   mapContainsValue(m,v)  → boolean  true if any value equals v
 *   mapToList(map)         → array    array of [key, value] pairs
 */
@Native("Map Library")
@Deprecated(since = "1.1.0") // un used by compiler
public class NativeMapLibrary extends NativeLibrary{


    public NativeMapLibrary(ExecutionContext context) {
        super(context);

    }

    @Override
    public void registerAll() {
       getExecutionContext().registerNative("mapNew",           this::mapNew);
       getExecutionContext().registerNative("mapSet",           this::mapSet);
       getExecutionContext().registerNative("mapGet",           this::mapGet);
       getExecutionContext().registerNative("mapHas",           this::mapHas);
       getExecutionContext().registerNative("mapRemove",        this::mapRemove);
       getExecutionContext().registerNative("mapSize",          this::mapSize);
       getExecutionContext().registerNative("mapKeys",          this::mapKeys);
       getExecutionContext().registerNative("mapValues",        this::mapValues);
       getExecutionContext().registerNative("mapClear",         this::mapClear);
       getExecutionContext().registerNative("mapIsEmpty",       this::mapIsEmpty);
       getExecutionContext().registerNative("mapMerge",         this::mapMerge);
       getExecutionContext().registerNative("mapContainsValue", this::mapContainsValue);
       getExecutionContext().registerNative("mapToList",        this::mapToList);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof Map))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a map", -1, LarvError.Kind.RUNTIME);
        return (Map<String, Object>) args.get(i);
    }

    private String keyArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a string key", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull Object mapNew(List<Object> a)      { return new LinkedHashMap<String, Object>(); }
    private Object mapSize(List<Object> a)     { return (double) mapArg(a,0,"mapSize").size(); }
    private @NotNull @Unmodifiable Object mapIsEmpty(List<Object> a)  { return mapArg(a,0,"mapIsEmpty").isEmpty(); }
    private @Nullable Object mapClear(List<Object> a)    { mapArg(a,0,"mapClear").clear(); return null; }
    private @NotNull @Unmodifiable Object mapHas(List<Object> a)      { return mapArg(a,0,"mapHas").containsKey(keyArg(a,1,"mapHas")); }
    private Object mapGet(List<Object> a)      { return mapArg(a,0,"mapGet").getOrDefault(keyArg(a,1,"mapGet"), null); }
    private @Nullable Object mapRemove(List<Object> a)   { mapArg(a,0,"mapRemove").remove(keyArg(a,1,"mapRemove")); return null; }

    private @Nullable Object mapSet(List<Object> a) {
        mapArg(a,0,"mapSet").put(keyArg(a,1,"mapSet"), a.size() > 2 ? a.get(2) : null);
        return null;
    }

    @Contract("_ -> new")
    private @NotNull Object mapKeys(List<Object> a) {
        return new ArrayList<>(mapArg(a,0,"mapKeys").keySet());
    }

    @Contract("_ -> new")
    private @NotNull Object mapValues(List<Object> a) {
        return new ArrayList<>(mapArg(a,0,"mapValues").values());
    }

    private @NotNull @Unmodifiable Object mapContainsValue(List<Object> a) {
        return mapArg(a,0,"mapContainsValue").containsValue(a.size() > 1 ? a.get(1) : null);
    }

    private @NotNull Object mapMerge(List<Object> a) {
        Map<String, Object> out = new LinkedHashMap<>(mapArg(a,0,"mapMerge"));
        out.putAll(mapArg(a,1,"mapMerge"));
        return out;
    }

    private @NotNull Object mapToList(List<Object> a) {
        List<Object> out = new ArrayList<>();
        for (Map.Entry<String, Object> entry : mapArg(a,0,"mapToList").entrySet()) {
            List<Object> pair = new ArrayList<>();
            pair.add(entry.getKey());
            pair.add(entry.getValue());
            out.add(pair);
        }
        return out;
    }
}