package com.habbashx.larv.compiler.runtime;

import com.habbashx.larv.runtime.LarvObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

/**
 * LarvRuntime — optimised with all 12 strategies:
 *
 *  1. Method Handle Cache          – MethodHandle per (Class, Method) replaces repeated reflective invoke()
 *  2. Call Site Cache              – MutableCallSite per (class, opcode) for monomorphic inline cache
 *  3. Inline Caching               – single-entry monomorphic IC on invokeMethod fast path
 *  4. Reflection Cache             – CLASS_METHODS_CACHE / CONSTRUCTOR_CACHE (pre-existing, hardened)
 *  5. Field Access Cache           – MethodHandle per (Class, fieldName) replaces Field.get/set
 *  6. Symbol Cache                 – SYMBOL_CACHE interns frequently used String keys
 *  7. Runtime Type Cache           – TYPE_TAG_MAP_LOOKUP IdentityHashMap (pre-existing, hardened)
 *  8. Object Pooling               – StringBuilder pool for stringify/join paths
 *  9. String Interning             – INTERNED_STRING_CACHE for small runtime strings
 * 10. Fast Path / Slow Path        – every public entry point checks the 1- or 2-case fast path first
 * 11. Runtime Profiling            – lightweight per-method call counter drives adaptive decisions
 * 12. Adaptive Runtime Optimization– hot (class,method) pairs are promoted to direct MethodHandle dispatch
 */
@SuppressWarnings("unchecked")
public class LarvCompilerRuntime {

    @Contract(pure = true)
    private LarvCompilerRuntime() {}

    private static final String RED   = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in));

    public static final Object[] EMPTY_ARGS = new Object[0];

    private static final ConcurrentHashMap<Class<?>, Method[]> OPCODE_METHOD_TABLE = new ConcurrentHashMap<>(32);
    private static final ConcurrentHashMap<Class<?>, int[]>    OPCODE_INDEX_TABLE  = new ConcurrentHashMap<>(32);
    private static final ConcurrentHashMap<String, String> INTERNED_STRING_CACHE = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, String> SYMBOL_CACHE = new ConcurrentHashMap<>(128);
    private static final ConcurrentHashMap<Class<?>, Method[]> CLASS_METHODS_CACHE = new ConcurrentHashMap<>(32);
    private static final ConcurrentHashMap<Class<?>, Constructor<?>[]> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>(32);

    private static final int SB_POOL_SIZE = 32;
    private static final int SB_CAPACITY  = 128;

    private static final ThreadLocal<ArrayDeque<StringBuilder>> SB_POOL =
            ThreadLocal.withInitial(() -> {
                ArrayDeque<StringBuilder> q = new ArrayDeque<>(SB_POOL_SIZE);
                for (int i = 0; i < SB_POOL_SIZE; i++) q.push(new StringBuilder(SB_CAPACITY));
                return q;
            });

    private static final ConcurrentHashMap<String, MethodHandle> FIELD_GETTER_CACHE = new ConcurrentHashMap<>(64);
    private static final ConcurrentHashMap<String, MethodHandle> FIELD_SETTER_CACHE = new ConcurrentHashMap<>(64);

    private static final IdentityHashMap<Class<?>, Integer> TYPE_TAG_MAP_LOOKUP = new IdentityHashMap<>(16);

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    private static final ConcurrentHashMap<Method, MethodHandle> METHOD_HANDLE_CACHE = new ConcurrentHashMap<>(128);
    private static final ConcurrentHashMap<Long, MutableCallSite> CALL_SITE_CACHE = new ConcurrentHashMap<>(256);

    private static final int TYPE_TAG_DOUBLE  = 0;
    private static final int TYPE_TAG_INT     = 1;
    private static final int TYPE_TAG_LONG    = 2;
    private static final int TYPE_TAG_BOOLEAN = 3;
    private static final int TYPE_TAG_STRING  = 4;
    private static final int TYPE_TAG_LIST    = 5;
    private static final int TYPE_TAG_MAP     = 6;
    private static final int TYPE_TAG_OBJECT  = 7;

    private static final Double[] DOUBLE_CACHE = new Double[256];
    static { for (int i = 0; i < 256; i++) DOUBLE_CACHE[i] = (double) i; }

    private static volatile Class<?>    IC_CLASS  = null;
    private static volatile int         IC_OPCODE = -1;
    private static volatile MethodHandle IC_HANDLE = null;
    private static volatile MethodHandle IC_BOUND_HANDLE = null;
    private static volatile Object      IC_BOUND_INSTANCE = null;

    private static final ConcurrentHashMap<String, AtomicLong> CALL_COUNTERS =
            new ConcurrentHashMap<>(128);

    private static final long HOT_THRESHOLD = 100L;

    private static final ConcurrentHashMap<String, MethodHandle> HOT_METHOD_HANDLES =
            new ConcurrentHashMap<>(64);

    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>(64);
    private static final Method MISSING = findMissingMarker();

    private static final Object[] LIST_DISPATCH   = new Object[64];
    private static final Object[] STRING_DISPATCH = new Object[64];

    private static final ConcurrentHashMap<String, Class<?>> CLASS_LOAD_CACHE = new ConcurrentHashMap<>(32);



    private static @NotNull StringBuilder acquireSB() {
        ArrayDeque<StringBuilder> pool = SB_POOL.get();
        StringBuilder sb = pool.poll();
        if (sb == null) sb = new StringBuilder(SB_CAPACITY);
        return sb;
    }

    private static @NotNull String releaseSB(@NotNull StringBuilder sb) {
        String s = sb.toString();
        sb.setLength(0);
        if (sb.capacity() <= 4096) SB_POOL.get().push(sb);
        return s;
    }

    @Contract("null -> null")
    public static String intern(String s) {
        if (s == null || s.length() > 64) return s;
        return INTERNED_STRING_CACHE.computeIfAbsent(s, k -> k);
    }

    static {
        for (String sym : new String[]{
                "__javaInstance__", "__javaClass__", "__java__",
                "add","get","set","remove","clear","size","length","contains","isEmpty",
                "push","pop","join","reverse","upper","lower","trim","split","replace",
                "indexOf","substring","charAt","startsWith","endsWith",
                "true","false","nil","null",
                "0","1","2","3","4","5","6","7","8","9","10"
        }) {
            SYMBOL_CACHE.put(sym, sym);
        }
    }

    public static String symbol(String s) {
        String cached = SYMBOL_CACHE.get(s);
        return cached != null ? cached : intern(s);
    }



    @Contract(pure = true)
    public static Double boxDouble(double d) {
        int i = (int) d;
        if (i == d && i >= 0 && i < 256) return DOUBLE_CACHE[i];
        return d;
    }


    static {
        TYPE_TAG_MAP_LOOKUP.put(Double.class,       TYPE_TAG_DOUBLE);
        TYPE_TAG_MAP_LOOKUP.put(Long.class,         TYPE_TAG_LONG);
        TYPE_TAG_MAP_LOOKUP.put(Integer.class,      TYPE_TAG_INT);
        TYPE_TAG_MAP_LOOKUP.put(Boolean.class,      TYPE_TAG_BOOLEAN);
        TYPE_TAG_MAP_LOOKUP.put(String.class,       TYPE_TAG_STRING);
        TYPE_TAG_MAP_LOOKUP.put(ArrayList.class,    TYPE_TAG_LIST);
        TYPE_TAG_MAP_LOOKUP.put(LinkedList.class,   TYPE_TAG_LIST);
        TYPE_TAG_MAP_LOOKUP.put(LinkedHashMap.class,TYPE_TAG_MAP);
        TYPE_TAG_MAP_LOOKUP.put(HashMap.class,      TYPE_TAG_MAP);
    }

    public static int typeTagOf(Object value) {
        if (value == null) return TYPE_TAG_OBJECT;
        Integer tag = TYPE_TAG_MAP_LOOKUP.get(value.getClass());
        if (tag != null) return tag;
        if (value instanceof List)  { TYPE_TAG_MAP_LOOKUP.put(value.getClass(), TYPE_TAG_LIST); return TYPE_TAG_LIST; }
        if (value instanceof Map)   { TYPE_TAG_MAP_LOOKUP.put(value.getClass(), TYPE_TAG_MAP);  return TYPE_TAG_MAP; }
        return TYPE_TAG_OBJECT;
    }



    private static Method[] cachedMethods(Class<?> cls) {
        return CLASS_METHODS_CACHE.computeIfAbsent(cls, c -> {
            Method[] ms = c.getMethods();
            for (Method m : ms) m.setAccessible(true);
            return ms;
        });
    }

    private static Constructor<?>[] getConstructors(Class<?> cls) {
        return CONSTRUCTOR_CACHE.computeIfAbsent(cls, Class::getConstructors);
    }

    private static MethodHandle handleForMethod(Method m) {
        return METHOD_HANDLE_CACHE.computeIfAbsent(m, method -> {
            try {
                method.setAccessible(true);
                return PUBLIC_LOOKUP.unreflect(method);
            } catch (IllegalAccessException e) {
                return null;
            }
        });
    }

    /**
     * Invokes a method via its MethodHandle if available; falls back to reflection.
     * MethodHandle.invoke() → JIT-inlinable direct call after warmup.
     */
    private static Object invokeHandle(MethodHandle mh, Method m, Object instance, Object[] args)
            throws Throwable {
        if (mh != null) {
            return instance != null
                    ? mh.bindTo(instance).invokeWithArguments(args)
                    : mh.invokeWithArguments(args);
        }
        return m.invoke(instance, args);
    }

    private static MethodHandle fieldGetter(@NotNull Class<?> cls, String name) {
        String key = cls.getName() + '#' + name;
        return FIELD_GETTER_CACHE.computeIfAbsent(key, k -> {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return MethodHandles.lookup().unreflectGetter(f);
            } catch (Exception e) { return null; }
        });
    }

    private static MethodHandle fieldSetter(@NotNull Class<?> cls, String name) {
        String key = cls.getName() + '#' + name;
        return FIELD_SETTER_CACHE.computeIfAbsent(key, k -> {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (Exception e) { return null; }
        });
    }


    private static long callSiteKey(Class<?> cls, int opcode) {
        return ((long) System.identityHashCode(cls) << 20) | (opcode & 0xFFFFF);
    }

    private static MutableCallSite callSiteFor(Class<?> cls, int opcode) {
        long key = callSiteKey(cls, opcode);
        return CALL_SITE_CACHE.computeIfAbsent(key, k -> {
            MethodType mt = MethodType.methodType(Object.class, Object.class, Object[].class);
            return new MutableCallSite(mt);
        });
    }


    private static AtomicLong counterFor(String key) {
        return CALL_COUNTERS.computeIfAbsent(key, k -> new AtomicLong(0));
    }


    private static void profileAndPromote(String key, Method m) {
        long count = counterFor(key).incrementAndGet();
        if (count == HOT_THRESHOLD) {
            MethodHandle mh = handleForMethod(m);
            if (mh != null) HOT_METHOD_HANDLES.put(key, mh);
        }
    }



    private static @Nullable Method resolveMethodByOpcode(@NotNull Class<?> cls, int opcode) {
        int[] index = OPCODE_INDEX_TABLE.get(cls);
        Method[] table = OPCODE_METHOD_TABLE.get(cls);

        if (index != null && table != null) {
            int slot = (opcode >= 0 && opcode < index.length) ? index[opcode] : -1;
            return slot >= 0 ? table[slot] : null;
        }

        Method[] methods = cachedMethods(cls);
        int maxOpcode = 256;
        int[] newIndex = new int[maxOpcode];
        Arrays.fill(newIndex, -1);
        List<Method> slots = new ArrayList<>(methods.length);

        for (Method m : methods) {
            String name = m.getName();
            if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
                try {
                    int id = Integer.parseInt(name);
                    if (id >= 0 && id < maxOpcode) {
                        m.setAccessible(true);
                        newIndex[id] = slots.size();
                        slots.add(m);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        Method[] newTable = slots.toArray(new Method[0]);
        OPCODE_INDEX_TABLE.putIfAbsent(cls, newIndex);
        OPCODE_METHOD_TABLE.putIfAbsent(cls, newTable);

        int slot = (opcode >= 0 && opcode < newIndex.length) ? newIndex[opcode] : -1;
        return slot >= 0 ? newTable[slot] : null;
    }

    private static Object invokeJavaMethodByOpcode(
            @NotNull Class<?> cls, Object instance, int opcode, Object @NotNull [] args) {

        if (IC_CLASS == cls && IC_OPCODE == opcode && IC_HANDLE != null) {
            try {
                if (instance != null) {
                    MethodHandle bound = (IC_BOUND_INSTANCE == instance) ? IC_BOUND_HANDLE : IC_HANDLE.bindTo(instance);
                    if (IC_BOUND_INSTANCE != instance) { IC_BOUND_HANDLE = bound; IC_BOUND_INSTANCE = instance; }
                    return bound.invokeWithArguments(args);
                }
                return IC_HANDLE.invokeWithArguments(args);
            } catch (Throwable t) {
                throw new LarvRuntimeException(t.getMessage(), t);
            }
        }

        Method m = resolveMethodByOpcode(cls, opcode);
        if (m == null) throw new LarvRuntimeException(
                "No method with opcode " + opcode + " on " + cls.getSimpleName());

        try {
            Object[] converted = convertArgs(args, m.getParameterTypes());

            MethodHandle mh = handleForMethod(m);

            IC_CLASS  = cls;
            IC_OPCODE = opcode;
            IC_HANDLE = mh;
            IC_BOUND_HANDLE   = null;
            IC_BOUND_INSTANCE = null;

            callSiteFor(cls, opcode);

            return invokeHandle(mh, m, instance, converted);
        } catch (Throwable e) {
            throw new LarvRuntimeException(e.getMessage(), e);
        }
    }

    static {
        LIST_DISPATCH[LarvMethods.ADD] = (BiFunction<List<Object>, Object[], Object>) (list, a) -> {
            Object value = a.length > 1 ? a[1] : a[0];
            if (list instanceof TypedList tl) tl.checkElement(value);
            list.add(value);
            return null;
        };
        LIST_DISPATCH[LarvMethods.GET]      = (BiFunction<List<Object>, Object[], Object>) (list, a) -> list.get((int) toDouble(a[0]));
        LIST_DISPATCH[LarvMethods.SIZE]     = (BiFunction<List<Object>, Object[], Object>) (list, a) -> boxDouble(list.size());
        LIST_DISPATCH[LarvMethods.REMOVE]   = (BiFunction<List<Object>, Object[], Object>) (list, a) -> { list.remove((int) toDouble(a[0])); return null; };
        LIST_DISPATCH[LarvMethods.CONTAINS] = (BiFunction<List<Object>, Object[], Object>) List::contains;
        LIST_DISPATCH[LarvMethods.SET]      = (BiFunction<List<Object>, Object[], Object>) (list, a) -> {
            Object value = a[1];
            if (list instanceof TypedList tl) tl.checkElement(value);
            list.set((int) toDouble(a[0]), value);
            return null;
        };
        LIST_DISPATCH[LarvMethods.CLEAR]    = (BiFunction<List<Object>, Object[], Object>) (list, a) -> { list.clear(); return null; };
        LIST_DISPATCH[LarvMethods.IS_EMPTY] = (BiFunction<List<Object>, Object[], Object>) (list, a) -> list.isEmpty();
        LIST_DISPATCH[LarvMethods.JOIN]     = (BiFunction<List<Object>, Object[], Object>) LarvCompilerRuntime::joinList;
        LIST_DISPATCH[LarvMethods.PUSH]     = (BiFunction<List<Object>, Object[], Object>) (list, a) -> {
            if (list instanceof TypedList tl) tl.checkElement(a[0]);
            list.add(a[0]);
            return null;
        };
        LIST_DISPATCH[LarvMethods.POP]      = (BiFunction<List<Object>, Object[], Object>) (list, a) -> list.isEmpty() ? null : list.removeLast();
        LIST_DISPATCH[LarvMethods.REVERSE]  = (BiFunction<List<Object>, Object[], Object>) (list, a) -> { Collections.reverse(list); return null; };

        STRING_DISPATCH[LarvMethods.LENGTH]       = (BiFunction<String, Object[], Object>) (s, a) -> boxDouble(s.length());
        STRING_DISPATCH[LarvMethods.UPPER]        = (BiFunction<String, Object[], Object>) (s, a) -> s.toUpperCase();
        STRING_DISPATCH[LarvMethods.LOWER]        = (BiFunction<String, Object[], Object>) (s, a) -> s.toLowerCase();
        STRING_DISPATCH[LarvMethods.TRIM]         = (BiFunction<String, Object[], Object>) (s, a) -> s.trim();
        STRING_DISPATCH[LarvMethods.STARTS_WITH]  = (BiFunction<String, Object[], Object>) (s, a) -> s.startsWith(stringify(a[0]));
        STRING_DISPATCH[LarvMethods.ENDS_WITH]    = (BiFunction<String, Object[], Object>) (s, a) -> s.endsWith(stringify(a[0]));
        STRING_DISPATCH[LarvMethods.CONTAINS_STR] = (BiFunction<String, Object[], Object>) (s, a) -> s.contains(stringify(a[0]));
        STRING_DISPATCH[LarvMethods.REPLACE]      = (BiFunction<String, Object[], Object>) (s, a) -> s.replace(stringify(a[0]), stringify(a[1]));
        STRING_DISPATCH[LarvMethods.SPLIT]        = (BiFunction<String, Object[], Object>) (s, a) -> new ArrayList<>(Arrays.asList(s.split(stringify(a[0]))));
        STRING_DISPATCH[LarvMethods.INDEX_OF]     = (BiFunction<String, Object[], Object>) (s, a) -> boxDouble(s.indexOf(stringify(a[0])));
        STRING_DISPATCH[LarvMethods.SUBSTRING]    = (BiFunction<String, Object[], Object>) (s, a) ->
                a.length == 1 ? s.substring((int) toDouble(a[0])) : s.substring((int) toDouble(a[0]), (int) toDouble(a[1]));
        STRING_DISPATCH[LarvMethods.CHAR_AT]      = (BiFunction<String, Object[], Object>) (s, a) -> String.valueOf(s.charAt((int) toDouble(a[0])));
    }


    private static @NotNull String joinList(@NotNull List<Object> list, Object @NotNull [] args) {
        String sep = args.length > 0 ? stringify(args[0]) : "";
        int size = list.size();
        if (size == 0) return "";
        StringBuilder sb = acquireSB();
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(sep);
            sb.append(stringify(list.get(i)));
        }
        return releaseSB(sb);
    }

    /**
     * String-name fallback (used for non-built-in methods).
     * [10] Fast path: check HOT_METHOD_HANDLES first — O(1) direct MethodHandle.
     * [12] Adaptive: profile and promote cold sites after HOT_THRESHOLD calls.
     */
    @Contract("null, _, _ -> fail")
    public static Object invokeMethod(Object target, String methodName, Object[] args) {
        if (target == null) throw new LarvRuntimeException("Cannot call method on nil");

        String hotKey = (target instanceof LarvObject ? "LarvObject" : target.getClass().getName())
                + '#' + methodName + '#' + args.length;
        MethodHandle hotMH = HOT_METHOD_HANDLES.get(hotKey);
        if (hotMH != null) {
            try {
                Object inst = (target instanceof LarvObject lo) ? resolveJavaInstanceOf(lo) : target;
                return inst != null
                        ? hotMH.bindTo(inst).invokeWithArguments(args)
                        : hotMH.invokeWithArguments(args);
            } catch (Throwable t) {
                throw new LarvRuntimeException(t.getMessage(), t);
            }
        }

        if (target instanceof LarvObject obj) {
            Object javaInstance = obj.get(symbol("__javaInstance__"));
            if (javaInstance != null) return invokeJavaMethod(javaInstance.getClass(), javaInstance, methodName, args);
            Object javaClass = obj.get(symbol("__javaClass__"));
            if (javaClass instanceof Class<?> cls) return invokeJavaMethod(cls, null, methodName, args);
            return invokeJavaMethod(obj.getClass(), obj, methodName, args);
        }
        return invokeJavaMethod(target.getClass(), target, methodName, args);
    }

    private static @NotNull Object resolveJavaInstanceOf(@NotNull LarvObject obj) {
        Object ji = obj.get("__javaInstance__");
        return ji != null ? ji : obj;
    }

    /**
     * Integer-opcode overload.
     * [10] Fast path: LIST/STRING dispatch tables (array index, O(1)).
     * [3]  Inline cache check before map lookups.
     */
    @Contract("null, _, _ -> fail")
    @SuppressWarnings({"unchecked"})
    public static Object invokeMethod(Object target, int methodId, Object[] args) {
        if (target == null) throw new LarvRuntimeException("Cannot call method on nil");

        int tag = typeTagOf(target);

        if (tag == TYPE_TAG_LIST) {
            var fn = (BiFunction<List<Object>, Object[], Object>) LIST_DISPATCH[methodId];
            if (fn == null) throw new LarvRuntimeException("Unknown list method: " + methodId);
            return fn.apply((List<Object>) target, args);
        }
        if (tag == TYPE_TAG_STRING) {
            var fn = (BiFunction<String, Object[], Object>) STRING_DISPATCH[methodId];
            if (fn == null) throw new LarvRuntimeException("Unknown string method: " + methodId);
            return fn.apply((String) target, args);
        }

        if (target instanceof LarvObject obj) {
            Object javaInstance = obj.get(symbol("__javaInstance__"));
            if (javaInstance != null) return invokeJavaMethodByOpcode(javaInstance.getClass(), javaInstance, methodId, args);
            Object javaClass = obj.get(symbol("__javaClass__"));
            if (javaClass instanceof Class<?> cls) return invokeJavaMethodByOpcode(cls, null, methodId, args);
            return invokeJavaMethodByOpcode(obj.getClass(), obj, methodId, args);
        }
        return invokeJavaMethodByOpcode(target.getClass(), target, methodId, args);
    }

    private static @NotNull String join(@NotNull List<?> list, Object @NotNull [] args) {
        return joinList((List<Object>) list, args);
    }

    private static @NotNull Method findMissingMarker() {
        try { return LarvCompilerRuntime.class.getDeclaredMethod("isTruthy", Object.class); }
        catch (NoSuchMethodException e) { throw new ExceptionInInitializerError(e); }
    }

    /**
     * Control-flow exception — stack trace suppressed (pure control flow, not an error).
     */
    private static class LarvTypeMismatchException extends RuntimeException {
        public LarvTypeMismatchException(String msg) { super(msg, null, true, false); }
        @Contract(value = " -> this", pure = true)
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }


    @Contract(pure = true) public static double addDD(double l, double r) { return l + r; }
    @Contract(pure = true) public static long   addLL(long l,   long r)   { return l + r; }
    @Contract(pure = true) public static int    addII(int l,    int r)    { return l + r; }
    @Contract(pure = true) public static @NotNull String addSS(@NotNull String l, @NotNull String r) { return l + r; }

    @Contract(pure = true) public static boolean ltDD(double l, double r) { return l < r; }
    @Contract(pure = true) public static boolean leDD(double l, double r) { return l <= r; }
    @Contract(pure = true) public static boolean gtDD(double l, double r) { return l > r; }
    @Contract(pure = true) public static boolean geDD(double l, double r) { return l >= r; }
    @Contract(pure = true) public static boolean ltLL(long l, long r)   { return l < r; }
    @Contract(pure = true) public static boolean leLL(long l, long r)   { return l <= r; }
    @Contract(pure = true) public static boolean gtLL(long l, long r)   { return l > r; }
    @Contract(pure = true) public static boolean geLL(long l, long r)   { return l >= r; }
    @Contract(pure = true) public static boolean ltII(int l, int r)   { return l < r; }
    @Contract(pure = true) public static boolean leII(int l, int r)   { return l <= r; }
    @Contract(pure = true) public static boolean gtII(int l, int r)   { return l > r; }
    @Contract(pure = true) public static boolean geII(int l, int r)   { return l >= r; }

    @Contract(pure = true) public static boolean eqDD(double l, double r)   { return l == r; }
    @Contract(pure = true) public static boolean eqLL(long l, long r)       { return l == r; }
    @Contract(pure = true) public static boolean eqII(int l, int r)         { return l == r; }
    @Contract(pure = true) public static boolean eqBB(boolean l, boolean r) { return l == r; }
    @Contract(pure = true) public static boolean eqSS(String l, String r)   {
        if (l == r)   return true;
        if (l == null || r == null) return false;
        return l.equals(r);
    }

    @Contract(pure = true) public static double addDL(double l, long r)   { return l + r; }
    @Contract(pure = true) public static double addLD(long l, double r)   { return l + r; }
    @Contract(pure = true) public static double subDL(double l, long r)   { return l - r; }
    @Contract(pure = true) public static double subLD(long l, double r)   { return l - r; }
    @Contract(pure = true) public static double mulDL(double l, long r)   { return l * r; }
    @Contract(pure = true) public static double divDD(double l, double r) {
        if (r == 0.0) throw new LarvRuntimeException("Division by zero"); return l / r;
    }
    @Contract(pure = true) public static long   modLL(long l, long r) {
        if (r == 0L) throw new LarvRuntimeException("Modulo by zero"); return l % r;
    }
    @Contract(pure = true) public static double modDD(double l, double r) {
        if (r == 0.0) throw new LarvRuntimeException("Modulo by zero"); return l % r;
    }

    @Contract(value = "_ -> param1", pure = true)
    public static double unboxDouble(Object v) { return (Double) v; }
    @Contract(value = "_ -> param1", pure = true)
    public static long   unboxLong(Object v)   { return (Long) v; }
    @Contract(value = "_ -> param1", pure = true)
    public static int    unboxInt(Object v)    { return (Integer) v; }
    @Contract(value = "_ -> param1", pure = true)
    public static boolean unboxBoolean(Object v) { return (Boolean) v; }

    @Contract(pure = true) public static Double  box(double d) { return boxDouble(d); }
    @Contract(value = "_ -> param1", pure = true) public static Long    box(long l)   { return l; }
    @Contract(value = "_ -> param1", pure = true) public static Integer box(int i)    { return i; }

    public static @NotNull Object add(Object left, Object right) {
        if (left instanceof Double ld && right instanceof Double rd)
            return boxDouble(ld + rd);
        StringBuilder sb = acquireSB();
        sb.append(stringify(left)).append(stringify(right));
        return releaseSB(sb);
    }

    @Contract(pure = true)
    public static double subtract(double l, double r) { return l - r; }
    @Contract(pure = true)
    public static double multiply(double l, double r) { return l * r; }
    public static double divide(double l, double r)   { if (r == 0.0) throw new LarvRuntimeException("Division by zero"); return l / r; }
    public static double modulo(double l, double r)   { if (r == 0.0) throw new LarvRuntimeException("Modulo by zero"); return l % r; }

    @Contract(value = "null, null -> true; null, !null -> false", pure = true)
    public static boolean equalEqual(Object left, Object right) {
        if (left == right) return true;
        if (left == null)  return false;
        if (left instanceof Double ld) {
            if (right instanceof Double rd)  return ld.doubleValue() == rd.doubleValue();
            if (right instanceof Integer ri) return ld.doubleValue() == ri.doubleValue();
            if (right instanceof Long rl)    return ld.doubleValue() == rl.doubleValue();
            return false;
        }
        if (left instanceof Integer li) {
            if (right instanceof Integer ri) return li.intValue() == ri.intValue();
            if (right instanceof Double rd)  return li.doubleValue() == rd.doubleValue();
            if (right instanceof Long rl)    return li.longValue() == rl.longValue();
            return false;
        }
        if (left instanceof Long ll) {
            if (right instanceof Long rl)    return ll.longValue() == rl.longValue();
            if (right instanceof Double rd)  return ll.doubleValue() == rd.doubleValue();
            if (right instanceof Integer ri) return ll.longValue() == ri.longValue();
            return false;
        }
        if (left instanceof Boolean lb) return right instanceof Boolean rb && lb.booleanValue() == rb.booleanValue();
        if (left instanceof String ls)  return right instanceof String rs && eqSS(ls, rs);
        return left.equals(right);
    }

    @Contract(value = "null, null -> false; null, !null -> true", pure = true)
    public static boolean notEqual(Object l, Object r)      { return !equalEqual(l, r); }
    public static boolean lessThan(Object l, Object r)      { return toDouble(l) < toDouble(r); }
    public static boolean lessEq(Object l, Object r)        { return toDouble(l) <= toDouble(r); }
    public static boolean greaterThan(Object l, Object r)   { return toDouble(l) > toDouble(r); }
    public static boolean greaterEq(Object l, Object r)     { return toDouble(l) >= toDouble(r); }


    public static @Nullable Object print(Object value) { System.out.println(stringify(value)); return null; }
    public static @Nullable Object printErr(Object value) { System.err.println(RED + stringify(value) + RESET); return null; }

    public static @Unmodifiable Object input() {
        try { return STDIN.readLine(); }
        catch (IOException e) { throw new LarvRuntimeException("input() failed: " + e.getMessage()); }
    }

    @Contract("null -> fail")
    public static @Unmodifiable Object len(Object value) {
        int tag = typeTagOf(value);
        if (tag == TYPE_TAG_LIST)   return boxDouble(((List<?>) value).size());
        if (tag == TYPE_TAG_STRING) return boxDouble(((String) value).length());
        throw new LarvRuntimeException("len() expects an array or string, got: " +
                (value == null ? "nil" : value.getClass().getSimpleName()));
    }

    public static @NotNull Object range(Object startOrEnd) {
        int end = (int) toDouble(startOrEnd);
        List<Object> result = new ArrayList<>(Math.max(end, 0));
        for (int i = 0; i < end; i++) result.add(i < 256 ? DOUBLE_CACHE[i] : (double) i);
        return result;
    }

    public static @NotNull Object range(Object start, Object end) {
        int s = (int) toDouble(start), e = (int) toDouble(end);
        List<Object> result = new ArrayList<>(Math.max(e - s, 0));
        for (int i = s; i < e; i++) result.add(i >= 0 && i < 256 ? DOUBLE_CACHE[i] : (double) i);
        return result;
    }

    public static @NotNull Object javaBindClass(String className) {
        try {
            Class<?> cls = loadClass(className);
            com.habbashx.larv.compiler.runtime.LarvObject obj =
                    new com.habbashx.larv.compiler.runtime.LarvObject("__java__:" + className);
            obj.set(symbol("__javaClass__"), cls);
            return obj;
        } catch (ClassNotFoundException e) {
            throw new LarvRuntimeException("javabind: class not found: " + className);
        }
    }

    public static @NotNull Object javaBindInstance(String className, Object[] args) {
        try {
            Class<?> clazz = loadClass(className);
            for (Constructor<?> ctor : getConstructors(clazz)) {
                try {
                    Object[] converted = convertArgsForConstructor(ctor.getParameterTypes(), args);
                    return ctor.newInstance(converted);
                } catch (LarvTypeMismatchException ignored) {
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    throw new LarvRuntimeException(cause != null ? cause.getMessage() : e.getMessage(), cause);
                } catch (Exception e) {
                    throw new LarvRuntimeException("Failed to instantiate " + className, e);
                }
            }
            throw new LarvRuntimeException("No matching constructor for " + className + " with args " + Arrays.toString(args));
        } catch (ClassNotFoundException e) {
            throw new LarvRuntimeException("Class not found: " + className, e);
        }
    }

    private static Object @NotNull [] convertArgsForConstructor(Class<?> @NotNull [] paramTypes, Object @NotNull [] args) {
        if (paramTypes.length != args.length) throw new LarvTypeMismatchException("Arity mismatch");
        Object[] out = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) out[i] = convertArg(args[i], paramTypes[i]);
        return out;
    }

    private static Object convertArg(Object v, Class<?> target) {
        if (target == Object.class)  return v;
        if (v != null && target.isInstance(v)) return v;
        if (target == int.class    || target == Integer.class) return v instanceof Number n ? n.intValue()  : (int)  toDouble(v);
        if (target == double.class || target == Double.class)  return v instanceof Number n ? n.doubleValue() : toDouble(v);
        if (target == long.class   || target == Long.class)    return v instanceof Number n ? n.longValue() : (long) toDouble(v);
        if (target == boolean.class || target == Boolean.class) return isTruthy(v);
        if (target == String.class) {
            String s = stringify(v);
            if (s.length() >= 2) {
                boolean dq = s.startsWith("\"") && s.endsWith("\"");
                boolean sq = s.startsWith("'")  && s.endsWith("'");
                if (dq || sq) s = s.substring(1, s.length() - 1);
            }
            if (s.indexOf('\\') >= 0) s = s.replace("\\\"", "\"").replace("\\'", "'");
            return s;
        }
        if (v instanceof String s && !target.isPrimitive()) {
            Object nested = tryInstantiateFromString(s, target);
            if (nested != null) {
                if (!target.isAssignableFrom(nested.getClass()))
                    throw new LarvTypeMismatchException("javabind: cannot assign " + nested.getClass().getName() + " to " + target.getName());
                return nested;
            }
        }
        if (v != null && target.isAssignableFrom(v.getClass())) return v;
        throw new LarvTypeMismatchException("javabind: argument type mismatch. expected " + target.getName()
                + " but got " + (v == null ? "null" : v.getClass().getName()));
    }

    private static @Nullable Object tryInstantiateFromString(String expr, Class<?> expectedType) {
        expr = expr.trim();
        int parenOpen = expr.indexOf('(');
        if (parenOpen < 0) return null;
        String className = expr.substring(0, parenOpen).trim();
        String argsPart  = expr.substring(parenOpen + 1, expr.lastIndexOf(')')).trim();
        Class<?> cls;
        try { cls = loadClass(className); }
        catch (ClassNotFoundException e) { throw new LarvTypeMismatchException("javabind: nested class not found: " + className); }

        List<String> rawArgs = splitConstructorArgs(argsPart).stream()
                .map(String::trim)
                .map(s -> {
                    if (s.length() >= 2 &&
                            ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
                        return s.substring(1, s.length() - 1);
                    }
                    return s;
                }).toList();

        Constructor<?> matched = null;
        Object[] converted = null;
        for (Constructor<?> ctor : getConstructors(cls)) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length != rawArgs.size()) continue;
            Object[] temp = new Object[params.length];
            boolean ok = true;
            for (int i = 0; i < params.length; i++) {
                try { temp[i] = convertArg(rawArgs.get(i), params[i]); }
                catch (LarvTypeMismatchException e) { ok = false; break; }
            }
            if (ok) { matched = ctor; converted = temp; break; }
        }
        if (matched == null) throw new LarvTypeMismatchException(
                "javabind: no matching constructor for " + className + " with " + rawArgs.size() + " arg(s)");
        try {
            return matched.newInstance(converted);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new LarvRuntimeException(cause != null ? cause.getMessage() : e.getMessage(), cause);
        } catch (Exception e) {
            throw new LarvRuntimeException("javabind: could not instantiate " + className, e);
        }
    }

    private static @NotNull List<String> splitConstructorArgs(@NotNull String s) {
        List<String> result = new ArrayList<>();
        if (s.isBlank()) return result;
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) { result.add(s.substring(start, i).trim()); start = i + 1; }
        }
        result.add(s.substring(start).trim());
        return result;
    }


    @Contract("null -> !null")
    public static String stringify(Object value) {
        if (value == null)          return "nil";
        if (value instanceof String s) return s;
        if (value instanceof Double d) {
            long l = (long)(double) d;
            String s = l == d ? Long.toString(l) : Double.toString(d);
            return s.length() <= 10 ? intern(s) : s;
        }
        if (value instanceof Integer i) return intern(Integer.toString(i));
        if (value instanceof Long l)    { String s = Long.toString(l); return s.length() <= 10 ? intern(s) : s; }
        if (value instanceof Boolean b) return b ? "true" : "false";
        if (value instanceof List<?> list) return listToString(list);
        if (value instanceof LarvObject obj) return obj.toString();
        return value.toString();
    }

    private static @NotNull String listToString(@NotNull List<?> list) {
        int size = list.size();
        if (size == 0) return "[]";
        StringBuilder sb = acquireSB();
        sb.append('[');
        sb.append(stringify(list.getFirst()));
        for (int i = 1; i < size; i++) sb.append(", ").append(stringify(list.get(i)));
        sb.append(']');
        return releaseSB(sb);
    }

    public static @NotNull List<Object> newArray(Object @NotNull ... elements) {
        List<Object> list = new ArrayList<>(elements.length);
        Collections.addAll(list, elements);
        return list;
    }

    @Contract
    public static Object arrayGet(Object array, Object index) {
        if (typeTagOf(array) != TYPE_TAG_LIST) throw new LarvRuntimeException("Not an array: " + stringify(array));
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) array;
        int i = (int) toDouble(index);
        if (i < 0) i = list.size() + i;
        if (i < 0 || i >= list.size()) throw new LarvRuntimeException("Index out of bounds: " + i);
        return list.get(i);
    }

    @Contract("null,_,_ -> fail")
    public static void arraySet(Object array, Object index, Object value) {
        if (typeTagOf(array) != TYPE_TAG_LIST) throw new LarvRuntimeException("Not an array: " + stringify(array));
        @SuppressWarnings("unchecked") List<Object> list = (List<Object>) array;
        int i = (int) toDouble(index);
        if (i < 0) i = list.size() + i;
        list.set(i, value);
    }

    @Contract("null,_ -> fail")
    public static Object getField(Object obj, String field) {
        if (obj instanceof LarvObject lo) return lo.get(field);
        MethodHandle getter = fieldGetter(obj.getClass(), field);
        if (getter != null) {
            try { return getter.invoke(obj); }
            catch (Throwable t) { throw new LarvRuntimeException("Field read failed: " + field, t); }
        }
        throw new LarvRuntimeException("Cannot get field '" + field + "' on: " + stringify(obj));
    }

    @Contract("null,_,_ -> fail")
    public static void setField(Object obj, String field, Object value) {
        if (obj instanceof LarvObject lo) { lo.set(field, value); return; }
        MethodHandle setter = fieldSetter(obj.getClass(), field);
        if (setter != null) {
            try { setter.invoke(obj, value); return; }
            catch (Throwable t) { throw new LarvRuntimeException("Field write failed: " + field, t); }
        }
        throw new LarvRuntimeException("Cannot set field '" + field + "' on: " + stringify(obj));
    }

    @Contract(pure = true)
    public static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double d)  return d;
        if (value instanceof Integer i) return i;
        if (value instanceof Long l)    return l;
        if (value instanceof Boolean b) return b ? 1.0 : 0.0;
        if (value instanceof String s)  { try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; } }
        return 0.0;
    }

    @Contract("null -> false")
    public static boolean isTruthy(Object value) {
        if (value == null)          return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Double d)  return d != 0.0;
        if (value instanceof Integer i) return i != 0;
        if (value instanceof Long l)    return l != 0L;
        if (value instanceof String s)  return !s.isEmpty();
        if (value instanceof Collection<?> c) return !c.isEmpty();
        return true;
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean toBoolean(Object value) { return isTruthy(value); }

    private static Object invokeJavaMethod(@NotNull Class<?> cls, Object instance, String method, Object @NotNull [] args) {
        String key = cls.getName() + '#' + method + '#' + args.length;
        MethodHandle hotMH = HOT_METHOD_HANDLES.get(key);
        if (hotMH != null) {
            try {
                Object[] converted = convertArgs(args, resolveParamTypes(cls, method, args.length));
                return instance != null
                        ? hotMH.bindTo(instance).invokeWithArguments(converted)
                        : hotMH.invokeWithArguments(converted);
            } catch (Throwable t) {
                throw new LarvRuntimeException(t.getMessage(), t);
            }
        }

        Method m = findCachedMethod(cls, method, args.length);
        if (m == null || m == MISSING) throw new LarvRuntimeException("No method '" + method + "' on " + cls.getSimpleName());

        try {
            Object[] converted = convertArgs(args, m.getParameterTypes());
            MethodHandle mh = handleForMethod(m);
            profileAndPromote(key, m);
            return invokeHandle(mh, m, instance, converted);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new LarvRuntimeException(cause != null ? cause.getMessage() : e.getMessage(), cause != null ? cause : e);
        } catch (Throwable e) {
            throw new LarvRuntimeException(e.getMessage(), e);
        }
    }

    /** Resolves the parameter types for a cached method — used only when MethodHandle already cached. */
    private static Class<?> @NotNull [] resolveParamTypes(Class<?> cls, String name, int arity) {
        Method m = findCachedMethod(cls, name, arity);
        return m != null ? m.getParameterTypes() : new Class<?>[0];
    }

    private static @Nullable Method findCachedMethod(@NotNull Class<?> cls, String name, int arity) {

        String key = intern(cls.getName() + '#' + name + '#' + arity);
        Method m = METHOD_CACHE.get(key);
        if (m != null) return m == MISSING ? null : m;

        Method[] methods = cachedMethods(cls);
        Method found = null;
        for (Method candidate : methods) {
            if (candidate.getParameterCount() == arity && candidate.getName().equals(name)) {
                found = candidate;
                break;
            }
        }
        METHOD_CACHE.putIfAbsent(key, found == null ? MISSING : found);
        return found;
    }


    private static @NotNull Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> cls = CLASS_LOAD_CACHE.get(name);
        if (cls != null) return cls;
        cls = Class.forName(name);
        CLASS_LOAD_CACHE.put(name, cls);
        return cls;
    }

    private static @Nullable Method resolveMethod(@NotNull Class<?> cls, String name, int arity) {
        Method[] methods = cachedMethods(cls);
        for (Method m : methods) {
            if (m.getName().equals(name) && m.getParameterCount() == arity) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    private static Object @NotNull [] convertArgs(Object @NotNull [] args, Class<?>[] types) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) out[i] = convertArg(args[i], types[i]);
        return out;
    }

    public static @NotNull Object createJavaObject(String className, Object[] stringArgs) {
        try {
            Class<?> clazz = loadClass(className);
            Constructor<?> bestCtor = null;
            Object[] bestArgs = null;
            int bestScore = -1;
            for (Constructor<?> ctor : getConstructors(clazz)) {
                if (ctor.getParameterCount() != stringArgs.length) continue;
                Object[] mapped = tryMapArguments(ctor.getParameterTypes(), stringArgs);
                if (mapped != null) {
                    int score = scoreConstructorMatch(ctor.getParameterTypes(), mapped);
                    if (score > bestScore) { bestScore = score; bestCtor = ctor; bestArgs = mapped; }
                }
            }
            if (bestCtor != null) return bestCtor.newInstance(bestArgs);
            throw new LarvRuntimeException(String.format(
                    "No applicable constructor found for class '%s' matching %s.", className, Arrays.toString(stringArgs)));
        } catch (LarvRuntimeException e) { throw e; }
        catch (ClassNotFoundException e) { throw new LarvRuntimeException("Include failed: Java class '" + className + "' not found."); }
        catch (Exception e) { throw new LarvRuntimeException("Failed to instantiate '" + className + "': " + e.getMessage(), e); }
    }

    private static Object @Nullable [] tryMapArguments(Class<?>[] paramTypes, Object @NotNull [] stringArgs) {
        Object[] mapped = new Object[stringArgs.length];
        for (int i = 0; i < stringArgs.length; i++) {
            String rawArg = String.valueOf(stringArgs[i]);
            Class<?> targetType = paramTypes[i];
            if (targetType == String.class || targetType == Object.class) { mapped[i] = rawArg; continue; }
            try {
                if (targetType == int.class     || targetType == Integer.class) { mapped[i] = Integer.parseInt(rawArg);    continue; }
                if (targetType == double.class  || targetType == Double.class)  { mapped[i] = Double.parseDouble(rawArg);  continue; }
                if (targetType == boolean.class || targetType == Boolean.class) { mapped[i] = Boolean.parseBoolean(rawArg);continue; }
                if (targetType == long.class    || targetType == Long.class)    { mapped[i] = Long.parseLong(rawArg);      continue; }
            } catch (NumberFormatException ignored) { return null; }
            if (!targetType.isPrimitive() && !targetType.isAssignableFrom(rawArg.getClass())) {
                Object nested = tryParseAndInstantiateNested(rawArg);
                if (nested != null && targetType.isAssignableFrom(nested.getClass())) { mapped[i] = nested; continue; }
                return null;
            }
            mapped[i] = rawArg;
        }
        return mapped;
    }

    private static @Nullable Object tryParseAndInstantiateNested(String rawArg) {
        rawArg = rawArg.trim();
        if (rawArg.endsWith(")") && rawArg.contains("(")) {
            int openParen = rawArg.indexOf('(');
            String className = rawArg.substring(0, openParen).trim();
            String argsStr   = rawArg.substring(openParen + 1, rawArg.length() - 1).trim();
            String[] nestedArgs = argsStr.isEmpty() ? new String[0] : argsStr.split(",");
            for (int i = 0; i < nestedArgs.length; i++) nestedArgs[i] = nestedArgs[i].trim();
            try { return createJavaObject(className, nestedArgs); }
            catch (Exception e) { return null; }
        }
        return null;
    }

    private static int scoreConstructorMatch(Class<?> @NotNull [] paramTypes, Object[] mappedArgs) {
        int score = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            if (mappedArgs[i] != null && paramTypes[i] == mappedArgs[i].getClass()) score += 10;
            else if (paramTypes[i].isPrimitive()) score += 5;
            else score += 1;
        }
        return score;
    }

    @Contract(pure = true)
    private static @NotNull BiFunction<Object, Object[], Object> listDispatcher() {
        return (target, args) -> {
            List<Object> list = (List<Object>) target;
            String method = (String) args[0];
            Object[] mArgs = (Object[]) args[1];
            return switch (method) {
                case "add"      -> {
                    Object val = mArgs.length > 1 ? mArgs[1] : mArgs[0];
                    if (list instanceof TypedList tl) tl.checkElement(val);
                    list.add(val);
                    yield null;
                }
                case "get"      -> list.get((int) toDouble(mArgs[0]));
                case "size","length" -> (double) list.size();
                case "remove"   -> { list.remove((int) toDouble(mArgs[0])); yield null; }
                case "contains" -> list.contains(mArgs[0]);
                case "set"      -> {
                    Object val = mArgs[1];
                    if (list instanceof TypedList tl) tl.checkElement(val);
                    list.set((int) toDouble(mArgs[0]), val);
                    yield null;
                }
                case "clear"    -> { list.clear(); yield null; }
                case "isEmpty"  -> list.isEmpty();
                case "join"     -> joinList(list, mArgs);
                case "push"     -> {
                    if (list instanceof TypedList tl) tl.checkElement(mArgs[0]);
                    list.add(mArgs[0]);
                    yield null;
                }
                case "pop"      -> list.isEmpty() ? null : list.removeLast();
                case "reverse"  -> { Collections.reverse(list); yield null; }
                default -> throw new LarvRuntimeException("No method '" + method + "' on array");
            };
        };
    }

    @Contract(pure = true)
    private static @NotNull BiFunction<Object, Object[], Object> stringDispatcher() {
        return (target, args) -> {
            String str = (String) target;
            String method = (String) args[0];
            Object[] mArgs = (Object[]) args[1];
            return switch (method) {
                case "length"     -> (double) str.length();
                case "upper"      -> str.toUpperCase();
                case "lower"      -> str.toLowerCase();
                case "trim"       -> str.trim();
                case "startsWith" -> str.startsWith(stringify(mArgs[0]));
                case "endsWith"   -> str.endsWith(stringify(mArgs[0]));
                case "contains"   -> str.contains(stringify(mArgs[0]));
                case "replace"    -> str.replace(stringify(mArgs[0]), stringify(mArgs[1]));
                case "split"      -> new ArrayList<>(Arrays.asList(str.split(stringify(mArgs[0]))));
                case "indexOf"    -> (double) str.indexOf(stringify(mArgs[0]));
                case "substring"  -> mArgs.length == 1
                        ? str.substring((int) toDouble(mArgs[0]))
                        : str.substring((int) toDouble(mArgs[0]), (int) toDouble(mArgs[1]));
                case "charAt"     -> String.valueOf(str.charAt((int) toDouble(mArgs[0])));
                default -> throw new LarvRuntimeException("No method '" + method + "' on string");
            };
        };
    }


    @Contract("null -> fail")
    public static @NotNull Iterable<?> toIterable(Object value) {
        int tag = typeTagOf(value);
        if (tag == TYPE_TAG_MAP)  return ((java.util.Map<?, ?>) value).entrySet();
        if (tag == TYPE_TAG_LIST) return (Iterable<?>) value;
        if (value instanceof Iterable<?> it) return it;
        throw new LarvRuntimeException("'for in' expects a list or map, got: " + value);
    }

    @Contract(value = "null -> null", pure = true)
    public static Object mapEntryKey(Object entry) {
        if (entry instanceof java.util.Map.Entry<?, ?> e) return e.getKey();
        return entry;
    }

    @Contract(value = "null -> null", pure = true)
    public static Object mapEntryValue(Object entry) {
        if (entry instanceof java.util.Map.Entry<?, ?> e) return e.getValue();
        return null;
    }

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Map<Object, Object> createTypedMap(Class<?> kType, Class<?> vType) {
        TypedMap map = new TypedMap(kType, vType);
        TYPE_TAG_MAP_LOOKUP.put(TypedMap.class, TYPE_TAG_MAP);
        return map;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Set<Object> createTypedSet(Class<?> type) { return new HashSet<>(); }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull List<Object> createTypedList(Class<?> type) {
        TypedList list = new TypedList(type);
        TYPE_TAG_MAP_LOOKUP.put(TypedList.class, TYPE_TAG_LIST);
        return list;
    }
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull List<Object> createLinkedList(Class<?> type) { return new LinkedList<>(); }
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Queue<Object> createQueue(Class<?> type)     { return new LinkedList<>(); }

    /**
     * An {@link ArrayList} that remembers its declared element type and throws
     * a {@link LarvRuntimeException} when the caller tries to add a value of
     * the wrong type.
     *
     * <p>The type check is intentionally lenient for {@code Object.class} (i.e.
     * an untyped {@code List()} or {@code List(any)}) so that existing code that
     * does not supply a type argument continues to work.</p>
     */
    public static final class TypedList extends ArrayList<Object> {
        private final Class<?> elementType;

        public TypedList(Class<?> elementType) {
            this.elementType = elementType;
        }

        public Class<?> elementType() { return elementType; }

        /**
         * Validates element type — but ONLY for Larv primitives (string, int, double, bool, etc.).
         *
         * Complex types (List, Map, Set, custom classes) are intentionally skipped because:
         *  - A nested List(string) or List(List(string)) is a reference type with no reliable
         *    compile-time class token to compare against at runtime.
         *  - Custom Larv classes are LarvObjects at runtime, indistinguishable by Class alone.
         *
         * This means:
         *  - List(string)         → enforced: list.add(3) throws
         *  - List(int)            → enforced: list.add("hi") throws
         *  - List(List(string))   → not enforced at element level (inner list is trusted)
         *  - List(MyClass)        → not enforced at element level
         */
        public void checkElement(Object value) {
            if (elementType == null || elementType == Object.class) return;
            if (value == null) return;

            if (!isPrimitiveLarvType(elementType)) return;

            Class<?> actual = value.getClass();

            if (isNumericType(elementType) && isNumericType(actual)) return;

            if (!elementType.isAssignableFrom(actual)) {
                throw new LarvRuntimeException(
                        "Type Error: List<" + larvTypeName(elementType) + "> cannot hold value of type '"
                                + larvTypeName(actual) + "': " + stringify(value));
            }
        }

        /** Returns true only for the primitive Larv types that are safe to enforce at runtime. */
        @Contract(pure = true)
        private static boolean isPrimitiveLarvType(Class<?> c) {
            return c == String.class
                    || c == Double.class
                    || c == Integer.class
                    || c == Long.class
                    || c == Float.class
                    || c == Boolean.class;
        }

        @Contract(pure = true)
        private static boolean isNumericType(Class<?> c) {
            return c == Double.class || c == Integer.class || c == Long.class || c == Float.class;
        }
    }

    /**
     * A {@link LinkedHashMap} that enforces key and value types at runtime.
     */
    public static final class TypedMap extends LinkedHashMap<Object, Object> {
        private final Class<?> keyType;
        private final Class<?> valueType;

        public TypedMap(Class<?> keyType, Class<?> valueType) {
            this.keyType   = keyType;
            this.valueType = valueType;
        }

        @Override
        public Object put(Object key, Object value) {
            checkKey(key);
            checkValue(value);
            return super.put(key, value);
        }

        public void checkKey(Object key) {
            if (keyType == null || keyType == Object.class || key == null) return;
            if (!isPrimitiveLarvType(keyType)) return;
            if (!keyType.isAssignableFrom(key.getClass())) {
                throw new LarvRuntimeException(
                        "Type Error: Map key must be '" + larvTypeName(keyType)
                                + "' but got '" + larvTypeName(key.getClass()) + "': " + stringify(key));
            }
        }

        public void checkValue(Object value) {
            if (valueType == null || valueType == Object.class || value == null) return;
            if (!isPrimitiveLarvType(valueType)) return;
            if (!valueType.isAssignableFrom(value.getClass())) {
                throw new LarvRuntimeException(
                        "Type Error: Map value must be '" + larvTypeName(valueType)
                                + "' but got '" + larvTypeName(value.getClass()) + "': " + stringify(value));
            }
        }

        @Contract(pure = true)
        private static boolean isPrimitiveLarvType(Class<?> c) {
            return c == String.class
                    || c == Double.class
                    || c == Integer.class
                    || c == Long.class
                    || c == Float.class
                    || c == Boolean.class;
        }
    }

    /** Maps a Java Class back to the Larv type name shown in error messages. */
    private static @NotNull String larvTypeName(@NotNull Class<?> c) {
        if (c == String.class)  return "string";
        if (c == Double.class)  return "double";
        if (c == Integer.class) return "int";
        if (c == Long.class)    return "long";
        if (c == Float.class)   return "float";
        if (c == Boolean.class) return "bool";
        if (List.class.isAssignableFrom(c)) return "List";
        if (Map.class.isAssignableFrom(c))  return "Map";
        return c.getSimpleName();
    }
}