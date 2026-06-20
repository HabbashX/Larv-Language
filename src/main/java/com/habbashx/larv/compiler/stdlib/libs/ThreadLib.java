package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.*;

public final class ThreadLib implements LarvStdlib {

    private static final Map<String, Thread>              THREADS   = new ConcurrentHashMap<>();
    private static final Map<String, Future<?>>           FUTURES   = new ConcurrentHashMap<>();
    private static final Map<String, BlockingQueue<Object>> CHANNELS = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newCachedThreadPool();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "thread"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("spawn",          "thread_spawn"),
                Map.entry("threadSleep",    "thread_threadSleep"),
                Map.entry("threadId",       "thread_threadId"),
                Map.entry("threadName",     "thread_threadName"),
                Map.entry("threadCount",    "thread_threadCount"),
                Map.entry("cpuCount",       "thread_cpuCount"),
                Map.entry("threadIsAlive",  "thread_threadIsAlive"),
                Map.entry("threadJoin",     "thread_threadJoin"),
                Map.entry("channelNew",     "thread_channelNew"),
                Map.entry("channelSend",    "thread_channelSend"),
                Map.entry("channelRecv",    "thread_channelRecv"),
                Map.entry("channelClose",   "thread_channelClose")
        );
    }

    /**
     * Spawns a new task in the thread pool.
     * @param alias A name to identify the thread for future joining.
     * @param fnName The Larv function to execute.
     */
    public static @Nullable Object thread_spawn(Object alias, Object fnName) {
        String name = str(alias, "spawn");
        String func = str(fnName, "spawn");

        Future<?> future = POOL.submit(() -> {
            System.out.println("Executing " + func + " on thread: " + Thread.currentThread().getName());
        });

        FUTURES.put(name, future);
        return null;
    }

    public static @Nullable Object thread_threadSleep(Object millis) {
        try { Thread.sleep((long) num(millis, "threadSleep")); return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }

    public static Object thread_threadId()    { return (double) Thread.currentThread().getId(); }
    public static @Unmodifiable Object thread_threadName()  { return Thread.currentThread().getName(); }
    public static Object thread_threadCount() { return (double) Thread.activeCount(); }
    public static Object thread_cpuCount()    { return (double) Runtime.getRuntime().availableProcessors(); }

    public static @NotNull Object thread_threadIsAlive(Object alias) {
        Future<?> f = FUTURES.get(str(alias, "threadIsAlive"));
        return f != null && !f.isDone();
    }

    public static @Nullable Object thread_threadJoin(Object alias) {
        Future<?> f = FUTURES.get(str(alias, "threadJoin"));
        if (f == null) throw new LarvRuntimeException("threadJoin(): no thread with alias: " + alias);
        try {
            f.get();
            return null;
        } catch (InterruptedException | ExecutionException e) {
            throw new LarvRuntimeException("threadJoin(): error joining thread: " + e.getMessage());
        }
    }

    public static @Nullable Object thread_channelNew(Object name) {
        CHANNELS.put(str(name, "channelNew"), new LinkedBlockingQueue<>());
        return null;
    }

    public static @Nullable Object thread_channelSend(Object name, Object value) {
        BlockingQueue<Object> q = CHANNELS.get(str(name, "channelSend"));
        if (q == null) throw new LarvRuntimeException("channelSend(): no channel: " + name);
        try { q.put(value); return null; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }

    public static @Nullable Object thread_channelRecv(Object name) {
        BlockingQueue<Object> q = CHANNELS.get(str(name, "channelRecv"));
        if (q == null) throw new LarvRuntimeException("channelRecv(): no channel: " + name);
        try { return q.take(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
    }

    public static @Nullable Object thread_channelClose(Object name) {
        CHANNELS.remove(str(name, "channelClose"));
        return null;
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