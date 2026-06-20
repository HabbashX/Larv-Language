package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * NativeServerSocketLibrary
 *
 * Larv-native implementation of java.net.ServerSocket
 *
 * Provides:
 * - bind(port)
 * - accept()
 * - close()
 * - isBound()
 * - isClosed()
 * - getLocalPort()
 * - setTimeout(ms)
 * - setReuseAddress(boolean)
 */

@Native("ServerSocket")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeServerSocketLibrary extends NativeLibrary {

    private ServerSocket server;

    public NativeServerSocketLibrary(ExecutionContext context) {
        super(context);
    }

    @Override
    public void registerAll() {
        getExecutionContext().registerNative("server_bind", this::bind);
        getExecutionContext().registerNative("server_accept", this::accept);
        getExecutionContext().registerNative("server_close", this::close);
        getExecutionContext().registerNative("server_isBound", this::isBound);
        getExecutionContext().registerNative("server_isClosed", this::isClosed);
        getExecutionContext().registerNative("server_getPort", this::getPort);
        getExecutionContext().registerNative("server_setTimeout", this::setTimeout);
        getExecutionContext().registerNative("server_setReuse", this::setReuse);
    }

    private @Nullable Object bind(@NotNull List<Object> args) {
        int port = ((Double) args.getFirst()).intValue();

        try {
            server = new ServerSocket(port);
            return null;

        } catch (IOException e) {
            throw new LarvError("ServerSocket.bind(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull Object accept(List<Object> args) {
        try {
            Socket socket = server.accept();
            return wrapSocket(socket);

        } catch (IOException e) {
            throw new LarvError("ServerSocket.accept(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object close(List<Object> args) {
        try {
            if (server != null) server.close();
            return null;

        } catch (IOException e) {
            throw new LarvError("ServerSocket.close(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull Object isBound(List<Object> args) {
        return server != null && !server.isClosed();
    }

    private @NotNull Object isClosed(List<Object> args) {
        return server == null || server.isClosed();
    }

    private Object getPort(List<Object> args) {
        return server == null ? -1 : (double) server.getLocalPort();
    }

    private @Nullable Object setTimeout(@NotNull List<Object> args) {
        try {
            server.setSoTimeout(((Double) args.get(0)).intValue());
            return null;

        } catch (IOException e) {
            throw new LarvError("ServerSocket.setTimeout(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object setReuse(@NotNull List<Object> args) {
        try {
            server.setReuseAddress((Boolean) args.get(0));
            return null;

        } catch (SocketException e) {
            throw new LarvError("ServerSocket.setReuseAddress(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull Object wrapSocket(Socket socket) {
        Map<String, Object> obj = new HashMap<>();

        try {
            obj.put("id", socket.hashCode());

            obj.put("read", (Function<List<Object>, Object>) args -> {
                try {
                    int size = args.isEmpty() ? 1024 : ((Double) args.get(0)).intValue();
                    byte[] buf = new byte[size];

                    int r = socket.getInputStream().read(buf);
                    if (r == -1) return null;

                    return new String(buf, 0, r);

                } catch (IOException e) {
                    throw new LarvError("Socket.read(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
                }
            });

            obj.put("write", (Function<List<Object>, Object>) args -> {
                try {
                    String data = args.get(0).toString();
                    socket.getOutputStream().write(data.getBytes());
                    socket.getOutputStream().flush();
                    return null;

                } catch (IOException e) {
                    throw new LarvError("Socket.write(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
                }
            });

            obj.put("close", (Runnable) () -> {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            });

            obj.put("remote", socket.getRemoteSocketAddress().toString());
            obj.put("local", socket.getLocalSocketAddress().toString());
            obj.put("isClosed", socket.isClosed());

        } catch (Exception e) {
            throw new LarvError("wrapSocket(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }

        return obj;
    }
}