package com.habbashx.larv.runtime.ffi;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages Java class bindings and method invocations for the Larv FFI.
 *
 * Supports two binding modes:
 *
 *   Static binding  — include Math from "java.lang.Math"
 *                     Math.sqrt(4)
 *                     Only static methods work here.
 *
 *   Instance binding — include sc from "java.util.Scanner" involve {"java.lang.System.in"}
 *                      sc.nextLine()
 *                      Constructs the Java object using the resolved args and holds
 *                      the live instance so both instance and static methods work.
 *
 * Argument resolution (inside involve { ... }):
 *   - A plain string like "hello" or a number-string like "1024" is kept as-is
 *     (the constructor matcher will coerce it to the right primitive/String type).
 *   - "java.lang.System.in"  →  resolves the static field System.in
 *   - "some.Class.field"     →  resolves any public static field via reflection
 *   - "some.Class(arg1,arg2)"→  constructs that class with the given literal args
 */
public class JavaClassRegistry {

    private final Map<String, Class<?>>     classBindings    = new HashMap<>();
    private final Map<String, Object>       instanceBindings = new HashMap<>();
    private final Map<String, MethodHandle> cache            = new HashMap<>();
    private final MethodHandles.Lookup      lookup           = MethodHandles.lookup();
    private static final MethodHandles.Lookup PUBLIC_LOOKUP   = MethodHandles.publicLookup();

    /** Static binding — only static methods can be called on this alias.
     *  If the class has a no-arg constructor, an instance is created automatically. */
    public void bind(String alias, String fqcn) {
        try {
            Class<?> clazz = Class.forName(fqcn);
            classBindings.put(alias, clazz);
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                instanceBindings.put(alias, instance);


            } catch (NoSuchMethodException | IllegalAccessException ignored) {
            }
        } catch (ClassNotFoundException e) {
            throw new LarvError(
                    "Java class not found: '" + fqcn + "' — check the fully-qualified name",
                    -1, LarvError.Kind.FFI);
        } catch (LarvError e) {
            throw e;
        } catch (Exception e) {
            throw new LarvError(
                    "Failed to construct '" + fqcn + "': " + e.getMessage(), -1, LarvError.Kind.FFI);
        }
    }

    /**
     * Instance binding with raw string args from the involve { ... } clause.
     * Each arg string is resolved before being passed to the constructor.
     */
    public void bindInstance(String alias, String fqcn, List<String> rawArgs) {
        try {
            Class<?> clazz = Class.forName(fqcn);
            classBindings.put(alias, clazz);

            List<Object> resolved = resolveArgs(rawArgs);

            Object instance;
            if (resolved.isEmpty()) {
                try {
                    instance = clazz.getDeclaredConstructor().newInstance();
                    instanceBindings.put(alias,instance);
                } catch (NoSuchMethodException | IllegalAccessException ignored) {

                }
            } else {
                instance = constructWithArgs(clazz, resolved);
                instanceBindings.put(alias, instance);
            }

        } catch (ClassNotFoundException e) {
            throw new LarvError("Java class not found: '" + fqcn + "'", -1, LarvError.Kind.FFI);
        } catch (LarvError e) {
            throw e;
        } catch (Exception e) {
            throw new LarvError(
                    "Failed to construct '" + fqcn + "': " + e.getMessage(), -1, LarvError.Kind.FFI);
        }
    }

    public boolean hasAlias(String alias) {
        return classBindings.containsKey(alias) || instanceBindings.containsKey(alias);
    }

    /**
     * Invokes a Java instance method on an arbitrary object that came back from
     * an earlier FFI call (e.g. a {@code java.nio.file.Path} or a
     * {@code java.util.stream.Stream}).  This mirrors the compiled runtime's
     * raw-object dispatch so that chained calls like
     * {@code Files.list(path).toList()} behave identically in both modes.
     */
    public Object invokeOnObject(@NotNull Object target, @NotNull String methodName, @NotNull List<Object> args) {
        Class<?>   clazz    = target.getClass();
        Object[]   rawArgs  = args.toArray();
        String     alias    = clazz.getSimpleName();
        try {
            Method method = findBestMatch(clazz, methodName, rawArgs, true);
            MethodHandle handle = toHandle(method);

            Object[] converted = convertArgsFor(method, rawArgs);

            Object result = invokeMethodHandle(handle, method, target, converted);
            return normalizeResult(result);
        } catch (LarvError e) {
            throw e;
        } catch (Throwable t) {
            throw buildInvocationError(alias, methodName, t);
        }
    }

    public Object invoke(String alias, String methodName, List<Object> args) {
        Class<?> clazz = classBindings.get(alias);
        if (clazz == null) throw new LarvError(
                "Unknown Java alias '" + alias + "' — did you forget 'include " + alias + " from \"...\"'?",
                -1, LarvError.Kind.FFI);

        Object instance = instanceBindings.get(alias);
        Object[] rawArgs = args.toArray();

        try {
            Method method = findBestMatch(clazz, methodName, rawArgs, instance != null);
            boolean isStatic = Modifier.isStatic(method.getModifiers());

            String cacheKey = alias + "#" + methodName + "#" + args.size() + "#" + isStatic;
            MethodHandle handle = cache.get(cacheKey);
            if (handle == null) {
                handle = toHandle(method);
                if (handle != null) cache.putIfAbsent(cacheKey, handle);
            }

            Object[] converted = convertArgsFor(method, rawArgs);

            Object result;
            if (isStatic) {
                result = invokeMethodHandle(handle, method, null, converted);
            } else {
                if (instance == null) throw new LarvError(
                        "'" + alias + "." + methodName + "' is an instance method but '" + alias +
                                "' was bound without an instance. Use 'include " + alias +
                                " from \"...\" involve { <args> }' to construct an instance.",
                        -1, LarvError.Kind.FFI);

                result = invokeMethodHandle(handle, method, instance, converted);
            }

            return normalizeResult(result);

        } catch (LarvError e) {
            throw e;
        } catch (Throwable t) {
            throw buildInvocationError(alias, methodName, t);
        }
    }


    /**
     * Resolves each raw involve-string into a real Java object.
     *
     * Resolution order for each token string {@code s}:
     *  1. If {@code s} is a pure integer literal (e.g. "1024") → Integer
     *  2. If {@code s} is a pure double literal (e.g. "3.14")  → Double
     *  3. If {@code s} looks like "SomeClass(arg,arg,...)"      → construct that class
     *  4. If {@code s} looks like "some.pkg.Class.field"        → resolve static field
     *  5. Otherwise treat as a plain String value
     */
    private @NotNull List<Object> resolveArgs(@NotNull List<String> rawArgs) {
        List<Object> out = new ArrayList<>();
        for (String s : rawArgs) {
            out.add(resolveArg(s.trim()));
        }
        return out;
    }

    private Object resolveArg(@NotNull String s) {

        s = s.trim();

        if (s.matches("-?\\d+")) {
            return Integer.parseInt(s);
        }

        if (s.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(s);
        }

        if (s.equals("System.in")) {
            return System.in;
        }

        if (s.equals("System.out")) {
            return System.out;
        }

        if (s.equals("System.err")) {
            return System.err;
        }

        // Check for constructor spec: some.pkg.ClassName(args...)
        int parenOpen = s.indexOf('(');
        if (parenOpen > 0 && s.endsWith(")")) {
            String fqcn    = s.substring(0, parenOpen).trim();
            String argsRaw = s.substring(parenOpen + 1, s.length() - 1).trim();
            return constructFromSpec(fqcn, argsRaw);
        }

        int lastDot = s.lastIndexOf('.');
        if (lastDot > 0 && lastDot < s.length() - 1) {

            String maybeClass = s.substring(0, lastDot);
            String maybeField = s.substring(lastDot + 1);

            Object fieldValue = tryResolveStaticField(maybeClass, maybeField);
            if (fieldValue != null) {
                return fieldValue;
            }
        }
        return s;
    }

    /**
     * Constructs an object from a spec like {@code java.io.FileWriter("example.properties")}.
     * Nested constructor specs are not supported; only string and numeric literals.
     */
    private Object constructFromSpec(String fqcn, String argsRaw) {
        try {
            Class<?> clazz = Class.forName(fqcn);
            List<Object> args = new ArrayList<>();
            if (!argsRaw.isEmpty()) {
                for (String part : splitArgs(argsRaw)) {
                    String p = part.trim();
                    if (p.startsWith("\"") && p.endsWith("\"")) {
                        args.add(p.substring(1, p.length() - 1));
                    } else {
                        args.add(resolveArg(p));
                    }
                }
            }
            return args.isEmpty()
                    ? clazz.getDeclaredConstructor().newInstance()
                    : constructWithArgs(clazz, args);
        } catch (ClassNotFoundException e) {
            throw new LarvError("Java class not found in involve arg: '" + fqcn + "'",
                    -1, LarvError.Kind.FFI);
        } catch (LarvError e) {
            throw e;
        } catch (Exception e) {
            throw new LarvError("Failed to construct involve arg '" + fqcn + "': " + e.getMessage(),
                    -1, LarvError.Kind.FFI);
        }
    }

    /**
     * Split top-level comma-separated arguments (ignores commas inside quotes or parens).
     */
    private @NotNull List<String> splitArgs(@NotNull String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0; boolean inStr = false; int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inStr = !inStr;
            else if (!inStr && c == '(') depth++;
            else if (!inStr && c == ')') depth--;
            else if (!inStr && depth == 0 && c == ',') {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    /** Returns the value of a public static field, or {@code null} if not found. */
    private @Nullable Object tryResolveStaticField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            Field f = clazz.getField(fieldName);

            if (Modifier.isStatic(f.getModifiers())) {
                return f.get(null);
            }

        } catch (Exception ignored) {}

        return null;
    }

    private @NotNull Object constructWithArgs(@NotNull Class<?> clazz, @NotNull List<Object> args) throws Exception {
        Object[] rawArgs = args.toArray();
        Constructor<?> best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Constructor<?> ctor : clazz.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length != rawArgs.length) continue;
            int score = scoreParams(params, rawArgs);
            if (score > bestScore) { bestScore = score; best = ctor; }
        }

        if (best == null) {
            // Build a helpful error listing available constructors
            StringBuilder ctors = new StringBuilder();
            ctors.append("\n  available constructors for ").append(clazz.getSimpleName()).append(":");
            for (Constructor<?> ctor : clazz.getConstructors()) {
                StringBuilder sig = new StringBuilder(clazz.getSimpleName()).append("(");
                Class<?>[] p = ctor.getParameterTypes();
                for (int i = 0; i < p.length; i++) { if (i > 0) sig.append(", "); sig.append(p[i].getSimpleName()); }
                sig.append(")");
                ctors.append("\n    ").append(sig);
            }
            String argWord = args.size() == 1 ? "argument" : "arguments";
            throw new LarvError(
                    "no constructor for '" + clazz.getSimpleName() + "' takes " + args.size() + " " + argWord,
                    -1, LarvError.Kind.FFI)
                    .withHint(ctors.toString().substring(1));
        }

        Object[] converted = convertArgs(best.getParameterTypes(), rawArgs);
        try {
            return best.newInstance(converted);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            throw new LarvError(
                    "constructor '" + clazz.getSimpleName() + "' threw an exception: " + describeThrowable(cause),
                    -1, LarvError.Kind.FFI)
                    .withNote("this exception was thrown by Java, not Larv — check the constructor arguments");
        } catch (Exception ex) {
            throw new LarvError(
                    "failed to construct '" + clazz.getSimpleName() + "': " + describeThrowable(ex),
                    -1, LarvError.Kind.FFI);
        }
    }

    private @NotNull Method findBestMatch(@NotNull Class<?> clazz, String name, Object[] args, boolean hasInstance) {
        Method best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name)) continue;

            Class<?>[] params = m.getParameterTypes();
            boolean exact   = params.length == args.length;
            boolean varargs = m.isVarArgs() && args.length >= params.length - 1;
            if (!exact && !varargs) continue;

            boolean isStatic = Modifier.isStatic(m.getModifiers());
            int methodScore = m.isVarArgs() ? scoreParamsVarargs(m, args) : scoreParams(params, args);
            if (hasInstance && !isStatic) methodScore += 5;
            if (!hasInstance && isStatic)  methodScore += 5;

            // Prefer an exact-arity (non-varargs) overload over a varargs one on a tie
            if (methodScore > bestScore
                    || (methodScore == bestScore && best != null && best.isVarArgs() && !m.isVarArgs())) {
                bestScore = methodScore; best = m;
            }
        }

        if (best == null) {
            throw buildMethodNotFoundError(clazz, name, args.length);
        }

        return best;
    }

    /**
     * Builds a rich FFI error when a method cannot be found.
     *
     * <p>The error includes:</p>
     * <ul>
     *   <li>The method name that was not found and the argument count tried</li>
     *   <li>The closest spelling match found via Levenshtein distance (the "did you mean?" hint)</li>
     *   <li>A list of all public methods on the class with the same argument count,
     *       or all public methods if none match the arity</li>
     * </ul>
     *
     * <p>Example output:</p>
     * <pre>
     *   ffi error[E004]: method 'readLi' not found on java.io.BufferedReader (0 argument(s))
     *    -->
     *     = help: did you mean 'readLine'?
     *
     *   methods with 0 argument(s) on BufferedReader:
     *     readLine()           — returns String
     *     read()               — returns int
     *     ready()              — returns boolean
     *     close()              — returns void
     *     reset()              — returns void
     *     mark(int)            — returns void
     *     markSupported()      — returns boolean
     *     ...
     * </pre>
     */
    private @NotNull LarvError buildMethodNotFoundError(@NotNull Class<?> clazz, String name, int arity) {
        // ── Collect all public methods ────────────────────────────────────
        Method[] all = clazz.getMethods();

        // ── Find closest name by Levenshtein distance ─────────────────────
        String suggestion = null;
        int    bestDist   = Integer.MAX_VALUE;
        for (Method m : all) {
            int d = levenshtein(name, m.getName());
            if (d < bestDist) { bestDist = d; suggestion = m.getName(); }
        }
        // Only suggest if reasonably close (≤ 3 edits, or ≤ half the name length)
        boolean hasSuggestion = suggestion != null && bestDist <= Math.max(3, name.length() / 2);

        // ── Build method list (same arity first, then all if none match) ──
        List<Method> sameArity = new ArrayList<>();
        for (Method m : all) {
            if (m.getParameterTypes().length == arity) sameArity.add(m);
        }
        List<Method> candidates = sameArity.isEmpty() ? java.util.Arrays.asList(all) : sameArity;

        // ── Format the available-methods table ────────────────────────────
        StringBuilder available = new StringBuilder();
        String arityLabel = sameArity.isEmpty()
                ? "\n  all public methods on " + clazz.getSimpleName() + ":"
                : "\n  methods with " + arity + " argument(s) on " + clazz.getSimpleName() + ":";
        available.append(arityLabel);

        // Deduplicate by name+arity, then sort alphabetically
        java.util.Map<String, Method> seen = new java.util.LinkedHashMap<>();
        for (Method m : candidates) {
            String key = m.getName() + "/" + m.getParameterCount();
            seen.putIfAbsent(key, m);
        }
        seen.values().stream()
                .sorted(java.util.Comparator.comparing(Method::getName))
                .limit(12)
                .forEach(m -> {
                    // Parameter types (simplified — strip package name)
                    StringBuilder sig = new StringBuilder(m.getName()).append("(");
                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length; i++) {
                        if (i > 0) sig.append(", ");
                        sig.append(params[i].getSimpleName());
                    }
                    sig.append(")");
                    String ret = m.getReturnType().getSimpleName();
                    available.append("\n    ").append(String.format("%-30s", sig)).append(" — returns ").append(ret);
                });

        if (seen.size() > 12) {
            available.append("\n    ... and ").append(seen.size() - 12).append(" more");
        }

        // ── Assemble the error ────────────────────────────────────────────
        String hint = hasSuggestion
                ? "did you mean '" + suggestion + "'?" + available
                : available.toString().substring(1); // trim leading \n

        String argWord = arity == 1 ? "argument" : "arguments";
        LarvError err = new LarvError(
                "method '" + name + "' not found on " + clazz.getName() + " (" + arity + " " + argWord + ")",
                -1, LarvError.Kind.FFI);
        err.withHint(hint);
        return err;
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     * Used to find the closest matching method name for "did you mean?" suggestions.
     */
    private static int levenshtein(@NotNull String a, @NotNull String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }

        /**
     * Builds a {@link MethodHandle} for {@code method}, or returns {@code null}
     * when the JVM module system blocks handle access (e.g. methods on
     * package-private JDK classes like {@code sun.nio.fs.WindowsPath}).  Callers
     * fall back to plain reflection in that case.
     */
    private @Nullable MethodHandle toHandle(@NotNull Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        MethodType type  = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

        MethodHandle handle;
        try {
            handle = isStatic
                    ? lookup.findStatic(method.getDeclaringClass(), method.getName(), type)
                    : lookup.findVirtual(method.getDeclaringClass(), method.getName(), type);
        } catch (IllegalAccessException e) {
            try {
                handle = PUBLIC_LOOKUP.unreflect(method);
            } catch (IllegalAccessException e2) {
                return null;
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Varargs methods produce "varargs collector" handles whose
        // invokeWithArguments would spread a trailing array argument;
        // fix them so the pre-built array is passed as-is.
        return method.isVarArgs() ? handle.asFixedArity() : handle;
    }

    /**
     * Invokes {@code method} on {@code instance} (or statically when
     * {@code instance} is null).  Uses the fast {@link MethodHandle} path when
     * available, otherwise falls back to plain reflection — required for
     * package-private JDK classes that the module system hides from handles.
     */
    private Object invokeMethodHandle(@Nullable MethodHandle handle, @NotNull Method method,
                                      @Nullable Object instance, Object @NotNull [] args) throws Throwable {
        if (handle != null) {
            return instance != null
                    ? handle.bindTo(instance).invokeWithArguments(args)
                    : handle.invokeWithArguments(args);
        }

        // Handle inaccessible classes (e.g. sun.nio.fs.WindowsPath): try to find
        // the same method on a public interface/superclass and use that instead.
        Method accessible = findAccessibleMethod(method, instance != null ? instance.getClass() : method.getDeclaringClass());
        if (accessible != method) {
            MethodHandle ah = toHandle(accessible);
            if (ah != null) {
                return instance != null
                        ? ah.bindTo(instance).invokeWithArguments(args)
                        : ah.invokeWithArguments(args);
            }
        }

        // Last resort: try plain reflection on the (possibly still inaccessible) method
        accessible.setAccessible(true);
        return accessible.invoke(instance, args);
    }

    /**
     * Finds a method with the same name/parameter types that is accessible
     * (declared on a public class or interface).  Searches the target class's
     * public interfaces, its superclass chain's interfaces, and public superclasses.
     */
    private @Nullable Method findAccessibleMethod(@NotNull Method original, @NotNull Class<?> targetClass) {
        // Original already accessible?
        if (Modifier.isPublic(original.getDeclaringClass().getModifiers())) {
            return original;
        }

        // Search public interfaces of targetClass AND its superclasses (and their super-interfaces)
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        // Start with targetClass's direct interfaces
        for (Class<?> iface : targetClass.getInterfaces()) {
            if (Modifier.isPublic(iface.getModifiers())) queue.add(iface);
        }
        // Also add interfaces of superclasses
        Class<?> superC = targetClass.getSuperclass();
        while (superC != null) {
            for (Class<?> iface : superC.getInterfaces()) {
                if (Modifier.isPublic(iface.getModifiers())) queue.add(iface);
            }
            superC = superC.getSuperclass();
        }

        while (!queue.isEmpty()) {
            Class<?> iface = queue.pop();
            if (!visited.add(iface)) continue;
            try {
                Method m = iface.getMethod(original.getName(), original.getParameterTypes());
                if (Modifier.isPublic(m.getModifiers())) return m;
            } catch (NoSuchMethodException ignored) {}
            for (Class<?> superIface : iface.getInterfaces()) {
                if (Modifier.isPublic(superIface.getModifiers())) queue.add(superIface);
            }
        }

        // Search public superclasses
        superC = targetClass.getSuperclass();
        while (superC != null) {
            if (Modifier.isPublic(superC.getModifiers())) {
                try {
                    return superC.getMethod(original.getName(), original.getParameterTypes());
                } catch (NoSuchMethodException ignored) {}
            }
            superC = superC.getSuperclass();
        }

        return original;
    }

    // ── result normalisation ──────────────────────────────────────────────────

    private Object normalizeResult(Object result) {
        if (result == null)                return null;
        if (result instanceof Double)      return result;
        if (result instanceof Float f)     return f.doubleValue();
        if (result instanceof Integer i)   return i.doubleValue();
        if (result instanceof Long l)      return l.doubleValue();
        if (result instanceof Short s)     return s.doubleValue();
        if (result instanceof Byte b)      return b.doubleValue();
        if (result instanceof Character c) return String.valueOf(c);
        if (result instanceof Boolean)     return result;
        if (result instanceof String)      return result;
        if (result instanceof List)        return result;
        if (result.getClass().isArray())   return java.util.Arrays.asList((Object[]) result);
        return result;
    }

    private int scoreParams(Class<?> @NotNull [] params, Object[] args) {
        int score = 0;
        for (int i = 0; i < params.length; i++) score += scoreOne(params[i], args[i]);
        return score;
    }

    /**
     * Scores a varargs method against the given arguments: the fixed parameters
     * are scored directly, every remaining argument is scored against the
     * varargs component type (as Java would expand them).
     */
    private int scoreParamsVarargs(@NotNull Method m, Object[] args) {
        Class<?>[] params = m.getParameterTypes();
        int fixed   = params.length - 1;
        int score   = 0;
        for (int i = 0; i < fixed; i++) score += scoreOne(params[i], args[i]);
        Class<?> varType = params[fixed].getComponentType();
        for (int i = fixed; i < args.length; i++) score += scoreOne(varType, args[i]);
        return score;
    }

    private int scoreOne(Class<?> p, Object a) {
        if (a == null) return 0;
        if (p.isAssignableFrom(a.getClass()))                                    { return 10; }
        if (a instanceof Double   && isNumeric(p))                               { return 7;  }
        if (a instanceof Integer  && isNumeric(p))                               { return 7;  }
        if (a instanceof Double   && (p == char.class || p == Character.class))  { return 6;  }
        if (a instanceof Boolean  && (p == boolean.class || p == Boolean.class)) { return 10; }
        if (a instanceof String   && (p == String.class || p == char.class))     { return 8;  }
        return -50;
    }

    private boolean isNumeric(Class<?> c) {
        return c == int.class || c == long.class || c == float.class || c == double.class
                || c == short.class || c == byte.class || c == Integer.class || c == Long.class
                || c == Float.class || c == Double.class;
    }

    private Object @NotNull [] convertArgs(Class<?>[] types, Object @NotNull [] args) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) out[i] = convert(args[i], types[i]);
        return out;
    }

    /**
     * Converts arguments for a method, expanding varargs parameters into a
     * real array (Java's single/multi-element varargs expansion).
     */
    private Object @NotNull [] convertArgsFor(@NotNull Method method, Object @NotNull [] args) {
        Class<?>[] types = method.getParameterTypes();
        if (!method.isVarArgs()) return convertArgs(types, args);

        int fixed    = types.length - 1;
        Object[] out = new Object[types.length];
        for (int i = 0; i < fixed; i++) out[i] = convert(args[i], types[i]);

        Class<?> varType = types[fixed].getComponentType();
        int varCount = args.length - fixed;
        Object varArray = Array.newInstance(varType, varCount);
        for (int i = 0; i < varCount; i++) {
            Array.set(varArray, i, convert(args[fixed + i], varType));
        }
        out[fixed] = varArray;
        return out;
    }

    private Object convert(Object value, Class<?> target) {
        if (value == null) return null;
        if (target == Object.class) return value;
        if (target.isInstance(value)) return value;

        if (value instanceof Number num) {
            if (target == double.class  || target == Double.class)  return num.doubleValue();
            if (target == float.class   || target == Float.class)   return num.floatValue();
            if (target == int.class     || target == Integer.class) return num.intValue();
            if (target == long.class    || target == Long.class)    return num.longValue();
            if (target == short.class   || target == Short.class)   return num.shortValue();
            if (target == byte.class    || target == Byte.class)    return num.byteValue();
            if (target == char.class    || target == Character.class) return (char) num.intValue();
        }
        if (value instanceof Boolean b) {
            if (target == boolean.class || target == Boolean.class) return b;
            throw new LarvError("Cannot pass boolean to parameter of type " + target.getSimpleName(),
                    -1, LarvError.Kind.FFI);
        }
        if (value instanceof String s) {
            if (target == String.class) return s;
            if (target == char.class || target == Character.class) return s.isEmpty() ? '\0' : s.charAt(0);
        }
        throw new LarvError(
                "Cannot convert " + value.getClass().getSimpleName() + " to " + target.getSimpleName(),
                -1, LarvError.Kind.FFI);
    }
    // ── Invocation error helpers ──────────────────────────────────────────────

    /**
     * Unwraps {@link java.lang.reflect.InvocationTargetException} — the reflective
     * wrapper that Java throws when the invoked method itself threw — and builds a
     * {@link LarvError} that shows the *real* cause instead of the useless wrapper.
     *
     * <p>Example: calling {@code sc.readLine()} on a closed reader throws
     * {@code InvocationTargetException} wrapping {@code IOException("Stream closed")}.
     * This method surfaces that as:</p>
     * <pre>
     *   ffi error[E004]: sc.readLine() threw java.io.IOException: Stream closed
     *    = note: the exception originated inside Java, not in your Larv code
     * </pre>
     */
    private @NotNull LarvError buildInvocationError(String alias, String methodName, @NotNull Throwable t) {
        // Unwrap InvocationTargetException (and UndeclaredThrowableException) recursively
        Throwable cause = t;
        while ((cause instanceof java.lang.reflect.InvocationTargetException ||
                cause instanceof java.lang.reflect.UndeclaredThrowableException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String javaType = cause.getClass().getName();
        String msg      = cause.getMessage();

        String header = "'" + alias + "." + methodName + "()' threw " + javaType +
                (msg != null && !msg.isBlank() ? ": " + msg : "");

        LarvError err = new LarvError(header, -1, LarvError.Kind.FFI);

        // Attach a context-aware hint for common Java exceptions
        String hint = switch (cause.getClass().getSimpleName()) {
            case "FileNotFoundException"    -> "check that the file path is correct and the file exists";
            case "IOException"              -> "an I/O error occurred — the stream may be closed or the file unreadable";
            case "NullPointerException"     -> "a null value was passed to a Java method that does not accept null";
            case "IllegalArgumentException" -> "one of the arguments passed to '" + methodName + "' was invalid — check types and ranges";
            case "IllegalStateException"    -> "'" + methodName + "' was called at the wrong time — the object may not be initialised yet";
            case "NumberFormatException"    -> "a string could not be parsed as a number — check the input value";
            case "ArrayIndexOutOfBoundsException",
                 "IndexOutOfBoundsException",
                 "StringIndexOutOfBoundsException"
                    -> "an index was out of range inside the Java method";
            case "ClassCastException"       -> "Java could not cast between types — check that you are passing the right argument types";
            case "UnsupportedOperationException"
                    -> "'" + methodName + "' is not supported by this implementation";
            case "SocketException",
                 "ConnectException"         -> "a network error occurred — check your connection and remote host";
            case "SQLException"             -> "a database error occurred — check your query and connection";
            default                         -> null;
        };

        if (hint != null) err.withHint(hint);

        err.withNote("this exception was thrown inside Java — the call site in your Larv code is the line above");

        return err;
    }

    /**
     * Returns a short human-readable description of a throwable for use in error
     * messages: {@code "ClassName: message"} or just {@code "ClassName"} if there
     * is no message.
     */
    private static @NotNull String describeThrowable(@NotNull Throwable t) {
        String name = t.getClass().getSimpleName();
        String msg  = t.getMessage();
        return (msg != null && !msg.isBlank()) ? name + ": " + msg : name;
    }

}