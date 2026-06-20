package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public final class ServerSocketLib implements LarvStdlib {

    private static final ThreadLocal<ServerSocket> SERVER = new ThreadLocal<>();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "serversocket"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("bind",          "server_bind"),
                Map.entry("accept",        "server_accept"),
                Map.entry("close",         "server_close"),
                Map.entry("getPort",       "server_getPort"),
                Map.entry("setSoTimeout",  "server_setSoTimeout"),
                Map.entry("isClosed",      "server_isClosed")
        );
    }

    public static @Nullable Object server_bind(Object port) {
        try {
            int p = (port instanceof Number n) ? n.intValue() : 0;
            SERVER.set(new ServerSocket(p));
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("bind(): " + e.getMessage()); }
    }

    public static Object server_accept() {
        try {
            return getServer("accept").accept();
        } catch (IOException e) { throw new LarvRuntimeException("accept(): " + e.getMessage()); }
    }

    public static @Nullable Object server_setSoTimeout(Object timeout) {
        try { getServer("setSoTimeout").setSoTimeout(((Number) timeout).intValue()); return null; }
        catch (IOException e) { throw new LarvRuntimeException("setSoTimeout(): " + e.getMessage()); }
    }

    public static @Nullable Object server_close() {
        try {
            ServerSocket ss = SERVER.get();
            if (ss != null) { ss.close(); SERVER.remove(); }
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("close(): " + e.getMessage()); }
    }

    public static Object server_getPort() { return (double) getServer("getPort").getLocalPort(); }

    public static @NotNull Object server_isClosed() {
        ServerSocket ss = SERVER.get();
        return ss == null || ss.isClosed();
    }

    private static @NotNull ServerSocket getServer(String fn) {
        ServerSocket ss = SERVER.get();
        if (ss == null) throw new LarvRuntimeException(fn + "(): no server bound");
        return ss;
    }
}