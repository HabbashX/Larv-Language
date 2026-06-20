package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class SystemLib implements LarvStdlib {

    @Contract(pure = true)
    @Override public @NotNull String name() { return "system"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("exit",        "system_exit"),
                Map.entry("getEnv",      "system_getEnv"),
                Map.entry("clock",       "system_clock"),
                Map.entry("nanoTime",    "system_nanoTime"),
                Map.entry("sleep",       "system_sleep"),
                Map.entry("exec",        "system_exec"),
                Map.entry("osName",      "system_osName"),
                Map.entry("osArch",      "system_osArch"),
                Map.entry("javaVersion", "system_javaVersion"),
                Map.entry("freeMemory",  "system_freeMemory"),
                Map.entry("totalMemory", "system_totalMemory"),
                Map.entry("maxMemory",   "system_maxMemory"),
                Map.entry("gc",          "system_gc"),
                Map.entry("sysProperty", "system_sysProperty"),
                Map.entry("userName",    "system_userName"),
                Map.entry("userHome",    "system_userHome")
        );
    }

    public static Object system_exit(Object code)        { System.exit((int) num(code, "exit")); return null; }
    public static @Unmodifiable Object system_getEnv(Object name)      { return System.getenv(str(name, "getEnv")); }
    public static Object system_clock()                  { return (double) System.currentTimeMillis(); }
    public static Object system_nanoTime()               { return (double) System.nanoTime(); }
    public static @Unmodifiable Object system_osName()                 { return System.getProperty("os.name"); }
    public static @Unmodifiable Object system_osArch()                 { return System.getProperty("os.arch"); }
    public static @Unmodifiable Object system_javaVersion()            { return System.getProperty("java.version"); }
    public static Object system_freeMemory()             { return (double) Runtime.getRuntime().freeMemory(); }
    public static Object system_totalMemory()            { return (double) Runtime.getRuntime().totalMemory(); }
    public static Object system_maxMemory()              { return (double) Runtime.getRuntime().maxMemory(); }
    public static @Nullable Object system_gc()                     { Runtime.getRuntime().gc(); return null; }
    public static @Unmodifiable Object system_sysProperty(Object key)  { return System.getProperty(str(key, "sysProperty")); }
    public static @Unmodifiable Object system_userName()               { return System.getProperty("user.name"); }
    public static @Unmodifiable Object system_userHome()               { return System.getProperty("user.home"); }

    public static @Nullable Object system_sleep(Object millis) {
        try { Thread.sleep((long) num(millis, "sleep")); return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }

    public static @NotNull Object system_exec(Object command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", str(command, "exec"));
            pb.redirectErrorStream(false);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            String err = new String(p.getErrorStream().readAllBytes());
            int code = p.waitFor();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("exit", (double) code);
            m.put("out",  out);
            m.put("err",  err);
            return m;
        } catch (Exception e) { throw new LarvRuntimeException("exec(): " + e.getMessage()); }
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }

    @Contract("null, _ -> fail")
    private static double num(Object v, String fn) {
        if (v instanceof Double d) return d;
        if (v instanceof Number n) return n.doubleValue();
        throw new LarvRuntimeException(fn + "(): argument must be a number");
    }
}