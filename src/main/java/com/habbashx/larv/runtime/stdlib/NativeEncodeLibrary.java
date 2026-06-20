package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Encode standard library — import "encode"
 *
 * Base64:
 *   base64Encode(str)          → string   encode string to Base64
 *   base64Decode(str)          → string   decode Base64 string
 *   base64EncodeUrl(str)       → string   URL-safe Base64 encode
 *   base64DecodeUrl(str)       → string   URL-safe Base64 decode
 *
 * Hashing:
 *   hashMd5(str)               → string   MD5 hex digest
 *   hashSha1(str)              → string   SHA-1 hex digest
 *   hashSha256(str)            → string   SHA-256 hex digest
 *   hashSha512(str)            → string   SHA-512 hex digest
 *
 * Hex:
 *   hexEncode(str)             → string   string → hex
 *   hexDecode(str)             → string   hex → string
 *
 * URL:
 *   urlEncode(str)             → string   percent-encode a URL component
 *   urlDecode(str)             → string   decode a percent-encoded string
 */
@Native("Base64 Encoder & Decoder Library")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeEncodeLibrary extends NativeLibrary {


    public NativeEncodeLibrary(ExecutionContext executionContext) {
        super(executionContext);
    }


    @Override
    public void registerAll() {
        getExecutionContext().registerNative("base64Encode",    this::base64Encode);
        getExecutionContext().registerNative("base64Decode",    this::base64Decode);
        getExecutionContext().registerNative("base64EncodeUrl", this::base64EncodeUrl);
        getExecutionContext().registerNative("base64DecodeUrl", this::base64DecodeUrl);
        getExecutionContext().registerNative("hashMd5",         this::hashMd5);
        getExecutionContext().registerNative("hashSha1",        this::hashSha1);
        getExecutionContext().registerNative("hashSha256",      this::hashSha256);
        getExecutionContext().registerNative("hashSha512",      this::hashSha512);
        getExecutionContext().registerNative("hexEncode",       this::hexEncode);
        getExecutionContext().registerNative("hexDecode",       this::hexDecode);
        getExecutionContext().registerNative("urlEncode",       this::urlEncode);
        getExecutionContext().registerNative("urlDecode",       this::urlDecode);
    }

    private String hash(@NotNull String input, String algorithm, String fn) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new LarvError(fn + "(): algorithm not available: " + algorithm, -1, LarvError.Kind.RUNTIME);
        }
    }


    private @Unmodifiable Object base64Encode(@NotNull List<Object> args) {
        return Base64.getEncoder().encodeToString(strArg(args, 0, "base64Encode").getBytes(StandardCharsets.UTF_8));
    }

    @Contract("_ -> new")
    private @NotNull Object base64Decode(@NotNull List<Object> args) {
        try {
            return new String(Base64.getDecoder().decode(strArg(args, 0, "base64Decode")), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new LarvError("base64Decode(): invalid Base64 input", -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Unmodifiable Object base64EncodeUrl(@NotNull List<Object> args) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(strArg(args, 0, "base64EncodeUrl").getBytes(StandardCharsets.UTF_8));
    }

    @Contract("_ -> new")
    private @NotNull Object base64DecodeUrl(@NotNull List<Object> args) {
        try {
            return new String(Base64.getUrlDecoder().decode(strArg(args, 0, "base64DecodeUrl")), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new LarvError("base64DecodeUrl(): invalid URL-safe Base64 input", -1, LarvError.Kind.RUNTIME);
        }
    }


    private @Unmodifiable Object hashMd5(@NotNull List<Object> args)    { return hash(strArg(args, 0, "hashMd5"),    "MD5",     "hashMd5"); }
    private @Unmodifiable Object hashSha1(@NotNull List<Object> args)   { return hash(strArg(args, 0, "hashSha1"),   "SHA-1",   "hashSha1"); }
    private @Unmodifiable Object hashSha256(@NotNull List<Object> args) { return hash(strArg(args, 0, "hashSha256"), "SHA-256", "hashSha256"); }
    private @Unmodifiable Object hashSha512(@NotNull List<Object> args) { return hash(strArg(args, 0, "hashSha512"), "SHA-512", "hashSha512"); }


    private @Unmodifiable Object hexEncode(@NotNull List<Object> args) {
        return HexFormat.of().formatHex(strArg(args, 0, "hexEncode").getBytes(StandardCharsets.UTF_8));
    }

    @Contract("_ -> new")
    private @NotNull Object hexDecode(@NotNull List<Object> args) {
        try {
            return new String(HexFormat.of().parseHex(strArg(args, 0, "hexDecode")), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new LarvError("hexDecode(): invalid hex string", -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object urlEncode(@NotNull List<Object> args) {
        try {
            return java.net.URLEncoder.encode(strArg(args, 0, "urlEncode"), StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            throw new LarvError("urlEncode(): encoding failed", -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Unmodifiable Object urlDecode(@NotNull List<Object> args) {
        try {
            return java.net.URLDecoder.decode(strArg(args, 0, "urlDecode"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new LarvError("urlDecode(): decoding failed", -1, LarvError.Kind.RUNTIME);
        }
    }
}