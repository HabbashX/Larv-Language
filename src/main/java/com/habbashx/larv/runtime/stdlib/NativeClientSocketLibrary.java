package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * NativeSocketLibrary
 *
 * Larv-native implementation of java.net.Socket
 */

@Native("Socket")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeClientSocketLibrary extends NativeLibrary {

    private Socket socket;

    public NativeClientSocketLibrary(ExecutionContext context) {
        super(context);
    }

    @Override
    public void registerAll() {
       getExecutionContext().registerNative("socket_connect", this::connect);
       getExecutionContext().registerNative("socket_close", this::close);
       getExecutionContext().registerNative("socket_isConnected", this::isConnected);
       getExecutionContext().registerNative("socket_setTimeout", this::setTimeout);
    }

    // =========================================================
    // CONNECT
    // =========================================================
    private Object connect(List<Object> args) {
        String host = args.get(0).toString();
        int port = ((Double) args.get(1)).intValue();

        try {
            socket = new Socket(host, port);
            return wrap(socket);

        } catch (IOException e) {
            throw new LarvError("Socket.connect(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object close(List<Object> args) {
        try {
            if (socket != null) socket.close();
            return null;

        } catch (IOException e) {
            throw new LarvError("Socket.close(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }


    private @NotNull Object isConnected(List<Object> args) {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private @Nullable Object setTimeout(@NotNull List<Object> args) {
        try {
            socket.setSoTimeout(((Double) args.get(0)).intValue());
            return null;

        } catch (SocketException e) {
            throw new LarvError("Socket.setTimeout(): " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }


    private @NotNull Object wrap(Socket socket) {
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