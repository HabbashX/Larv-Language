package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public final class HttpLib implements LarvStdlib {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "http"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("httpGet",         "http_httpGet"),
                Map.entry("httpPost",        "http_httpPost"),
                Map.entry("httpPostJson",    "http_httpPostJson"),
                Map.entry("httpPut",         "http_httpPut"),
                Map.entry("httpDelete",      "http_httpDelete"),
                Map.entry("httpGetStatus",   "http_httpGetStatus"),
                Map.entry("httpGetBody",     "http_httpGetBody")
        );
    }

    public static @NotNull Object http_httpGet(Object url) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpGet"))).GET().build();
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return response(res);
        } catch (Exception e) { throw new LarvRuntimeException("httpGet(): " + e.getMessage()); }
    }

    public static @NotNull Object http_httpPost(Object url, Object body) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpPost")))
                    .POST(HttpRequest.BodyPublishers.ofString(str(body, "httpPost"))).build();
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return response(res);
        } catch (Exception e) { throw new LarvRuntimeException("httpPost(): " + e.getMessage()); }
    }

    public static @NotNull Object http_httpPostJson(Object url, Object json) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpPostJson")))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(str(json, "httpPostJson"))).build();
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return response(res);
        } catch (Exception e) { throw new LarvRuntimeException("httpPostJson(): " + e.getMessage()); }
    }

    public static @NotNull Object http_httpPut(Object url, Object body) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpPut")))
                    .PUT(HttpRequest.BodyPublishers.ofString(str(body, "httpPut"))).build();
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return response(res);
        } catch (Exception e) { throw new LarvRuntimeException("httpPut(): " + e.getMessage()); }
    }

    public static @NotNull Object http_httpDelete(Object url) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpDelete"))).DELETE().build();
            var res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return response(res);
        } catch (Exception e) { throw new LarvRuntimeException("httpDelete(): " + e.getMessage()); }
    }

    public static Object http_httpGetStatus(Object url) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpGetStatus"))).GET().build();
            return (double) CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) { throw new LarvRuntimeException("httpGetStatus(): " + e.getMessage()); }
    }

    public static Object http_httpGetBody(Object url) {
        try {
            var req = HttpRequest.newBuilder(URI.create(str(url, "httpGetBody"))).GET().build();
            return CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) { throw new LarvRuntimeException("httpGetBody(): " + e.getMessage()); }
    }

    private static @NotNull Map<String, Object> response(@NotNull HttpResponse<String> res) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", (double) res.statusCode());
        m.put("body",   res.body());
        return m;
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}