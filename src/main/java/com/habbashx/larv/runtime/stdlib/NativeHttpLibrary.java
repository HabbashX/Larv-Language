package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP standard library — import http
 *
 * All functions return a map with these keys:
 *   "status"  → number  HTTP status code
 *   "body"    → string  response body text
 *   "ok"      → boolean true if status is 2xx
 *
 *   httpGet(url)                      → map   GET request
 *   httpPost(url, body)               → map   POST with plain text body
 *   httpPostJson(url, body)           → map   POST with application/json
 *   httpPut(url, body)                → map   PUT with plain text body
 *   httpDelete(url)                   → map   DELETE request
 *   httpRequest(method,url,body,type) → map   custom method/content-type
 *
 * Headers can be added to httpRequest as a 5th argument (a map of strings).
 * Timeout is fixed at 30 seconds.
 */
@Native("Http Library")
public class NativeHttpLibrary implements NativeLibrary {

    private final ExecutionContext context;
    private final HttpClient client;

    public NativeHttpLibrary(ExecutionContext context) {
        this.context = context;
        this.client  = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public void registerAll() {
        context.registerNative("httpGet",      this::httpGet);
        context.registerNative("httpPost",     this::httpPost);
        context.registerNative("httpPostJson", this::httpPostJson);
        context.registerNative("httpPut",      this::httpPut);
        context.registerNative("httpDelete",   this::httpDelete);
        context.registerNative("httpRequest",  this::httpRequest);
    }

    private String strArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArg(@NotNull List<Object> args, int i) {
        if (args.size() <= i || !(args.get(i) instanceof Map)) return new LinkedHashMap<>();
        return (Map<String, Object>) args.get(i);
    }

    private @NotNull Object httpGet(List<Object> args) {
        return send(strArg(args,0,"httpGet"), "GET", null, null, new LinkedHashMap<>());
    }

    private @NotNull Object httpPost(List<Object> args) {
        return send(strArg(args,0,"httpPost"), "POST",
                args.size() > 1 ? args.get(1).toString() : "",
                "text/plain", new LinkedHashMap<>());
    }

    private @NotNull Object httpPostJson(List<Object> args) {
        return send(strArg(args,0,"httpPostJson"), "POST",
                args.size() > 1 ? args.get(1).toString() : "",
                "application/json", new LinkedHashMap<>());
    }

    private @NotNull Object httpPut(List<Object> args) {
        return send(strArg(args,0,"httpPut"), "PUT",
                args.size() > 1 ? args.get(1).toString() : "",
                "text/plain", new LinkedHashMap<>());
    }

    private @NotNull Object httpDelete(List<Object> args) {
        return send(strArg(args,0,"httpDelete"), "DELETE", null, null, new LinkedHashMap<>());
    }

    private @NotNull Object httpRequest(List<Object> args) {
        String url    = strArg(args, 0, "httpRequest");
        String method = strArg(args, 1, "httpRequest").toUpperCase();
        String body   = args.size() > 2 && args.get(2) != null ? args.get(2).toString() : null;
        String ct     = args.size() > 3 && args.get(3) instanceof String s ? s : null;
        Map<String, Object> headers = mapArg(args, 4);
        return send(url, method, body, ct, headers);
    }

    private @NotNull Map<String, Object> send(String url, String method, String body, String contentType,
                                              @NotNull Map<String, Object> extraHeaders) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            for (Map.Entry<String, Object> e : extraHeaders.entrySet())
                builder.header(e.getKey(), e.getValue().toString());

            HttpRequest.BodyPublisher publisher = (body == null || body.isEmpty())
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);

            if (contentType != null && !contentType.isEmpty())
                builder.header("Content-Type", contentType);

            builder.method(method, publisher);

            HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", (double) resp.statusCode());
            result.put("body",   resp.body());
            result.put("ok",     resp.statusCode() >= 200 && resp.statusCode() < 300);
            return result;

        } catch (IllegalArgumentException e) {
            throw new LarvError("http: invalid URL '" + url + "': " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        } catch (IOException | InterruptedException e) {
            throw new LarvError("http: request failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }
}