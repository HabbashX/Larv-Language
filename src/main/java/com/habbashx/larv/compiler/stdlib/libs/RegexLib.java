package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexLib implements LarvStdlib {

    private static final ThreadLocal<Matcher> MATCHER = new ThreadLocal<>();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "regex"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("compile",      "regex_compile"),
                Map.entry("matches",      "regex_matches"),
                Map.entry("find",         "regex_find"),
                Map.entry("group",        "regex_group"),
                Map.entry("groupCount",   "regex_groupCount"),
                Map.entry("start",        "regex_start"),
                Map.entry("end",          "regex_end"),
                Map.entry("replaceFirst", "regex_replaceFirst"),
                Map.entry("replaceAll",   "regex_replaceAll"),
                Map.entry("split",        "regex_split"),
                Map.entry("quote",        "regex_quote"),
                Map.entry("reset",        "regex_reset")
        );
    }

    public static @Nullable Object regex_compile(Object regex, Object input) {
        Pattern pattern = Pattern.compile(str(regex, "compile"));
        MATCHER.set(pattern.matcher(str(input, "compile")));
        return null;
    }

    public static @Nullable Object regex_reset() {
        getMatcher("reset").reset();
        return null;
    }

    public static @NotNull @Unmodifiable Object regex_matches() {
        return getMatcher("matches").matches();
    }

    public static @NotNull @Unmodifiable Object regex_find() {
        return getMatcher("find").find();
    }

    public static @Unmodifiable Object regex_group(Object groupIndex) {
        int idx = (groupIndex instanceof Number n) ? n.intValue() : 0;
        return getMatcher("group").group(idx);
    }

    public static @NotNull @Unmodifiable Object regex_groupCount() {
        return getMatcher("groupCount").groupCount();
    }

    public static @NotNull @Unmodifiable Object regex_start(Object groupIndex) {
        int idx = (groupIndex instanceof Number n) ? n.intValue() : 0;
        return getMatcher("start").start(idx);
    }

    public static @NotNull @Unmodifiable Object regex_end(Object groupIndex) {
        int idx = (groupIndex instanceof Number n) ? n.intValue() : 0;
        return getMatcher("end").end(idx);
    }

    public static @Unmodifiable Object regex_replaceFirst(Object replacement) {
        return getMatcher("replaceFirst").replaceFirst(str(replacement, "replaceFirst"));
    }

    public static @Unmodifiable Object regex_replaceAll(Object replacement) {
        return getMatcher("replaceAll").replaceAll(str(replacement, "replaceAll"));
    }

    public static @NotNull @Unmodifiable Object regex_split(Object regex, Object input) {
        return Pattern.compile(str(regex, "split"))
                .splitAsStream(str(input, "split"))
                .toList();
    }

    public static @NotNull @Unmodifiable Object regex_quote(Object input) {
        return Pattern.quote(str(input, "quote"));
    }


    private static @NotNull Matcher getMatcher(String fn) {
        Matcher m = MATCHER.get();
        if (m == null) throw new LarvRuntimeException(fn + "(): no regex compiled/active");
        return m;
    }

    @Contract("null,_ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s))
            throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}