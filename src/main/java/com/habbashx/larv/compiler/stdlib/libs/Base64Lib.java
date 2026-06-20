package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Base64Lib implements LarvStdlib {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "base64"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("base64Encode", "base64_base64Encode"),
                Map.entry("base64Decode", "base64_base64Decode"),
                Map.entry("urlEncode",    "base64_urlEncode"),
                Map.entry("urlDecode",    "base64_urlDecode"),
                Map.entry("hexEncode",    "base64_hexEncode"),
                Map.entry("hexDecode",    "base64_hexDecode")
        );
    }

    public static @Unmodifiable Object base64_base64Encode(Object input) {
        return Base64.getEncoder().encodeToString(str(input, "base64Encode").getBytes(StandardCharsets.UTF_8));
    }

    @Contract("_ -> new")
    public static @NotNull Object base64_base64Decode(Object input) {
        return new String(Base64.getDecoder().decode(str(input, "base64Decode")), StandardCharsets.UTF_8);
    }

    public static @Unmodifiable Object base64_urlEncode(Object input) {
        return URLEncoder.encode(str(input, "urlEncode"), StandardCharsets.UTF_8);
    }

    public static @Unmodifiable Object base64_urlDecode(Object input) {
        return URLDecoder.decode(str(input, "urlDecode"), StandardCharsets.UTF_8);
    }

    @Contract("_ -> new")
    public static @NotNull Object base64_hexEncode(Object input) {
        byte[] bytes = str(input, "hexEncode").getBytes(StandardCharsets.UTF_8);
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2]     = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    @Contract("_ -> new")
    public static @NotNull Object base64_hexDecode(Object input) {
        String hex = str(input, "hexDecode");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}