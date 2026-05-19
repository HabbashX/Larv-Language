package com.habbashx.larv.runtime.ffi;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            MethodHandle handle = cache.computeIfAbsent(cacheKey, k -> {
                try { return toHandle(method); }
                catch (Exception ex) { throw new RuntimeException(ex); }
            });

            Object[] converted = convertArgs(method.getParameterTypes(), rawArgs);

            Object result;
            if (isStatic) {
                result = handle.invokeWithArguments(converted);
            } else {
                if (instance == null) throw new LarvError(
                        "'" + alias + "." + methodName + "' is an instance method but '" + alias +
                                "' was bound without an instance. Use 'include " + alias +
                                " from \"...\" involve { <args> }' to construct an instance.",
                        -1, LarvError.Kind.FFI);

                Object[] fullArgs = new Object[converted.length + 1];
                fullArgs[0] = instance;
                System.arraycopy(converted, 0, fullArgs, 1, converted.length);
                result = handle.invokeWithArguments(fullArgs);
            }

            return normalizeResult(result);

        } catch (LarvError e) {
            throw e;
        } catch (Throwable t) {
            throw new LarvError(
                    "Error calling " + alias + "." + methodName + "(): " + t.getMessage(),
                    -1, LarvError.Kind.FFI);
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
     * Constructs an object from a spec like {@code java.io.FileWriter("example.txt")}.
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

        if (best == null) throw new LarvError(
                "No constructor for '" + clazz.getSimpleName() + "' matching " +
                        args.size() + " argument(s)", -1, LarvError.Kind.FFI);

        Object[] converted = convertArgs(best.getParameterTypes(), rawArgs);
        return best.newInstance(converted);
    }

    private @NotNull Method findBestMatch(@NotNull Class<?> clazz, String name, Object[] args, boolean hasInstance) {
        Method best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterTypes().length != args.length) continue;

            boolean isStatic = Modifier.isStatic(m.getModifiers());
            int methodScore = scoreParams(m.getParameterTypes(), args);
            if (hasInstance && !isStatic) methodScore += 5;
            if (!hasInstance && isStatic)  methodScore += 5;

            if (methodScore > bestScore) { bestScore = methodScore; best = m; }
        }

        if (best == null) throw new LarvError(
                "No method '" + name + "' with " + args.length +
                        " argument(s) found in " + clazz.getSimpleName(),
                -1, LarvError.Kind.FFI);

        return best;
    }

    private MethodHandle toHandle(@NotNull Method method) throws Exception {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        MethodType type  = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

        return isStatic
                ? lookup.findStatic(method.getDeclaringClass(), method.getName(), type)
                : lookup.findVirtual(method.getDeclaringClass(), method.getName(), type);
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
        return result;
    }

    private int scoreParams(Class<?> @NotNull [] params, Object[] args) {
        int score = 0;
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i]; Object a = args[i];
            if (a == null) continue;
            if (p.isAssignableFrom(a.getClass()))                                   { score += 10; continue; }
            if (a instanceof Double   && isNumeric(p))                               { score += 7;  continue; }
            if (a instanceof Integer  && isNumeric(p))                               { score += 7;  continue; }
            if (a instanceof Double   && (p == char.class || p == Character.class))  { score += 6;  continue; }
            if (a instanceof Boolean  && (p == boolean.class || p == Boolean.class)) { score += 10; continue; }
            if (a instanceof String   && (p == String.class || p == char.class))     { score += 8;  continue; }
            score -= 50;
        }
        return score;
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
}