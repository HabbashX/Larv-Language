package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public final class SocketLib implements LarvStdlib {

    private static class SocketSession {
        final Socket socket;
        final InputStream in;
        final OutputStream out;

        SocketSession(@NotNull Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }
    }

    private static final ThreadLocal<SocketSession> SESSION = new ThreadLocal<>();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "socket"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("connect",        "socket_connect"),
                Map.entry("importSocket",   "socket_importSocket"), // CRITICAL
                Map.entry("send",           "socket_send"),
                Map.entry("receive",        "socket_receive"),
                Map.entry("writeBytes",     "socket_writeBytes"),
                Map.entry("readBytes",      "socket_readBytes"),
                Map.entry("setSoTimeout",   "socket_setSoTimeout"),
                Map.entry("setTcpNoDelay",  "socket_setTcpNoDelay"),
                Map.entry("setKeepAlive",   "socket_setKeepAlive"),
                Map.entry("getRemoteAddr",  "socket_getRemoteAddr"),
                Map.entry("close",          "socket_close")
        );
    }

    public static @Nullable Object socket_connect(Object host, Object port) {
        try {
            SESSION.set(new SocketSession(new Socket(str(host, "connect"), ((Number) port).intValue())));
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("connect(): " + e.getMessage()); }
    }

    @Contract("null -> fail")
    public static @Nullable Object socket_importSocket(Object socketObj) {
        if (!(socketObj instanceof Socket s))
            throw new LarvRuntimeException("importSocket(): invalid socket object");
        try {
            SESSION.set(new SocketSession(s));
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("importSocket(): " + e.getMessage()); }
    }

    public static @Nullable Object socket_send(Object data) {
        try {
            PrintWriter pw = new PrintWriter(getSession("send").out, true);
            pw.println(str(data, "send"));
            return null;
        } catch (Exception e) { throw new LarvRuntimeException("send(): " + e.getMessage()); }
    }

    public static @Unmodifiable Object socket_receive() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getSession("receive").in));
            return br.readLine();
        } catch (IOException e) { throw new LarvRuntimeException("receive(): " + e.getMessage()); }
    }

    @Contract("null -> fail")
    public static @Nullable Object socket_writeBytes(Object bytes) {
        if (!(bytes instanceof byte[] b)) throw new LarvRuntimeException("writeBytes(): requires byte array");
        try {
            getSession("writeBytes").out.write(b);
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("writeBytes(): " + e.getMessage()); }
    }

    public static @NotNull Object socket_readBytes(Object length) {
        try {
            int len = ((Number) length).intValue();
            return getSession("readBytes").in.readNBytes(len);
        } catch (IOException e) { throw new LarvRuntimeException("readBytes(): " + e.getMessage()); }
    }

    public static @Nullable Object socket_setSoTimeout(Object ms) {
        try { getSession("setSoTimeout").socket.setSoTimeout(((Number) ms).intValue()); return null; }
        catch (IOException e) { throw new LarvRuntimeException(e.getMessage()); }
    }

    public static @Nullable Object socket_setTcpNoDelay(Object enabled) {
        try { getSession("setTcpNoDelay").socket.setTcpNoDelay((Boolean) enabled); return null; }
        catch (IOException e) { throw new LarvRuntimeException(e.getMessage()); }
    }

    public static @Nullable Object socket_setKeepAlive(Object enabled) {
        try { getSession("setKeepAlive").socket.setKeepAlive((Boolean) enabled); return null; }
        catch (IOException e) { throw new LarvRuntimeException(e.getMessage()); }
    }

    public static @Unmodifiable Object socket_getRemoteAddr() {
        return getSession("getRemoteAddr").socket.getRemoteSocketAddress().toString();
    }

    public static @Nullable Object socket_close() {
        try {
            SocketSession s = SESSION.get();
            if (s != null) {
                s.in.close();
                s.out.close();
                s.socket.close();
                SESSION.remove();
            }
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("close(): " + e.getMessage()); }
    }

    private static @NotNull SocketSession getSession(String fn) {
        SocketSession s = SESSION.get();
        if (s == null) throw new LarvRuntimeException(fn + "(): no active socket");
        return s;
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}