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
 * System standard library — import system
 *
 *   exit(code)        → nil      exit the process with code
 *   getEnv(name)      → string   read environment variable (nil if missing)
 *   getArgs()         → array    command-line arguments
 *   clock()           → number   wall-clock milliseconds since Unix epoch
 *   nanoTime()        → number   nanoseconds (for benchmarking)
 *   sleep(ms)         → nil      pause execution for ms milliseconds
 *   exec(cmd)         → map      run shell command, returns {exit, out, err}
 *   osName()          → string   OS name  (e.g. "Linux")
 *   osArch()          → string   CPU arch (e.g. "amd64")
 *   javaVersion()     → string   JVM version string
 *   freeMemory()      → number   JVM free heap bytes
 *   totalMemory()     → number   JVM total heap bytes
 *   gc()              → nil      suggest garbage collection
 */
public class NativeSystemLibrary implements NativeLibrary {

    private final ExecutionContext context;

    public NativeSystemLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("exit",        this::exit);
        context.registerNative("getEnv",      this::getEnv);
        context.registerNative("getArgs",     this::getArgs);
        context.registerNative("clock",       this::clock);
        context.registerNative("nanoTime",    this::nanoTime);
        context.registerNative("sleep",       this::sleep);
        context.registerNative("exec",        this::exec);
        context.registerNative("osName",      this::osName);
        context.registerNative("osArch",      this::osArch);
        context.registerNative("javaVersion", this::javaVersion);
        context.registerNative("freeMemory",  this::freeMemory);
        context.registerNative("totalMemory", this::totalMemory);
        context.registerNative("gc",          this::gc);
    }

    private String strArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    private double numArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof Double d))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a number", -1, LarvError.Kind.RUNTIME);
        return (Double) args.get(i);
    }

    private Object exit(@NotNull List<Object> args) {
        int code = args.isEmpty() ? 0 : (int) numArg(args, 0, "exit");
        System.exit(code);
        return null;
    }

    private @Unmodifiable Object getEnv(@NotNull List<Object> args) {
        return System.getenv(strArg(args, 0, "getEnv"));
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull Object getArgs(List<Object> args) {
        return new ArrayList<>();
    }

    private Object clock(List<Object> args)      { return (double) System.currentTimeMillis(); }
    private Object nanoTime(List<Object> args)   { return (double) System.nanoTime(); }
    private @Unmodifiable Object osName(List<Object> args)     { return System.getProperty("os.name"); }
    private @Unmodifiable Object osArch(List<Object> args)     { return System.getProperty("os.arch"); }
    private @Unmodifiable Object javaVersion(List<Object> args){ return System.getProperty("java.version"); }
    private Object freeMemory(List<Object> args) { return (double) Runtime.getRuntime().freeMemory(); }
    private Object totalMemory(List<Object> args){ return (double) Runtime.getRuntime().totalMemory(); }
    private @Nullable Object gc(List<Object> args)         { Runtime.getRuntime().gc(); return null; }

    private @Nullable Object sleep(@NotNull List<Object> args) {
        long ms = (long) numArg(args, 0, "sleep");
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return null;
    }

    private @NotNull Object exec(@NotNull List<Object> args) {
        String cmd = strArg(args, 0, "exec");
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(false);
            Process proc = pb.start();

            String out = new String(proc.getInputStream().readAllBytes());
            String err = new String(proc.getErrorStream().readAllBytes());
            int    code = proc.waitFor();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("exit", (double) code);
            result.put("out",  out);
            result.put("err",  err);
            result.put("ok",   code == 0);
            return result;

        } catch (Exception e) {
            throw new LarvError("exec(): failed to run command: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }
}