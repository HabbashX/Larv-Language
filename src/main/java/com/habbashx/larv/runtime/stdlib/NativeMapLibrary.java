package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;

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
public class NativeMapLibrary implements NativeLibrary{

    private final ExecutionContext context;

    public NativeMapLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("mapNew",           this::mapNew);
        context.registerNative("mapSet",           this::mapSet);
        context.registerNative("mapGet",           this::mapGet);
        context.registerNative("mapHas",           this::mapHas);
        context.registerNative("mapRemove",        this::mapRemove);
        context.registerNative("mapSize",          this::mapSize);
        context.registerNative("mapKeys",          this::mapKeys);
        context.registerNative("mapValues",        this::mapValues);
        context.registerNative("mapClear",         this::mapClear);
        context.registerNative("mapIsEmpty",       this::mapIsEmpty);
        context.registerNative("mapMerge",         this::mapMerge);
        context.registerNative("mapContainsValue", this::mapContainsValue);
        context.registerNative("mapToList",        this::mapToList);
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

    private Object mapNew(List<Object> a)      { return new LinkedHashMap<String, Object>(); }
    private Object mapSize(List<Object> a)     { return (double) mapArg(a,0,"mapSize").size(); }
    private Object mapIsEmpty(List<Object> a)  { return mapArg(a,0,"mapIsEmpty").isEmpty(); }
    private Object mapClear(List<Object> a)    { mapArg(a,0,"mapClear").clear(); return null; }
    private Object mapHas(List<Object> a)      { return mapArg(a,0,"mapHas").containsKey(keyArg(a,1,"mapHas")); }
    private Object mapGet(List<Object> a)      { return mapArg(a,0,"mapGet").getOrDefault(keyArg(a,1,"mapGet"), null); }
    private Object mapRemove(List<Object> a)   { mapArg(a,0,"mapRemove").remove(keyArg(a,1,"mapRemove")); return null; }

    private Object mapSet(List<Object> a) {
        mapArg(a,0,"mapSet").put(keyArg(a,1,"mapSet"), a.size() > 2 ? a.get(2) : null);
        return null;
    }

    private Object mapKeys(List<Object> a) {
        return new ArrayList<>(mapArg(a,0,"mapKeys").keySet());
    }

    private Object mapValues(List<Object> a) {
        return new ArrayList<>(mapArg(a,0,"mapValues").values());
    }

    private Object mapContainsValue(List<Object> a) {
        return mapArg(a,0,"mapContainsValue").containsValue(a.size() > 1 ? a.get(1) : null);
    }

    private Object mapMerge(List<Object> a) {
        Map<String, Object> out = new LinkedHashMap<>(mapArg(a,0,"mapMerge"));
        out.putAll(mapArg(a,1,"mapMerge"));
        return out;
    }

    private Object mapToList(List<Object> a) {
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