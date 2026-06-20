package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.regex.Pattern;

public final class StringLib implements LarvStdlib {

    @Contract(pure = true)
    @Override public @NotNull String name() { return "string"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("strLen",        "string_strLen"),
                Map.entry("strUpper",      "string_strUpper"),
                Map.entry("strLower",      "string_strLower"),
                Map.entry("strTrim",       "string_strTrim"),
                Map.entry("strTrimLeft",   "string_strTrimLeft"),
                Map.entry("strTrimRight",  "string_strTrimRight"),
                Map.entry("strContains",   "string_strContains"),
                Map.entry("strStartsWith", "string_strStartsWith"),
                Map.entry("strEndsWith",   "string_strEndsWith"),
                Map.entry("strIndexOf",    "string_strIndexOf"),
                Map.entry("strSlice",      "string_strSlice"),
                Map.entry("strReplace",    "string_strReplace"),
                Map.entry("strReplaceAll", "string_strReplaceAll"),
                Map.entry("strSplit",      "string_strSplit"),
                Map.entry("strJoin",       "string_strJoin"),
                Map.entry("strRepeat",     "string_strRepeat"),
                Map.entry("strReverse",    "string_strReverse"),
                Map.entry("strCharAt",     "string_strCharAt"),
                Map.entry("strToNumber",   "string_strToNumber"),
                Map.entry("strFromNumber", "string_strFromNumber"),
                Map.entry("strIsEmpty",    "string_strIsEmpty"),
                Map.entry("strPadLeft",    "string_strPadLeft"),
                Map.entry("strPadRight",   "string_strPadRight"),
                Map.entry("strChars",      "string_strChars"),
                Map.entry("strFormat",     "string_strFormat")
        );
    }

    public static Object string_strLen(Object s)                        { return (double) str(s, "strLen").length(); }
    public static @NotNull @Unmodifiable Object string_strUpper(Object s)                      { return str(s, "strUpper").toUpperCase(); }
    public static @NotNull @Unmodifiable Object string_strLower(Object s)                      { return str(s, "strLower").toLowerCase(); }
    public static @NotNull @Unmodifiable Object string_strTrim(Object s)                       { return str(s, "strTrim").strip(); }
    public static @NotNull @Unmodifiable Object string_strTrimLeft(Object s)                   { return str(s, "strTrimLeft").stripLeading(); }
    public static @NotNull @Unmodifiable Object string_strTrimRight(Object s)                  { return str(s, "strTrimRight").stripTrailing(); }
    public static @NotNull @Unmodifiable Object string_strContains(Object s, Object sub)       { return str(s, "strContains").contains(str(sub, "strContains")); }
    public static @NotNull @Unmodifiable Object string_strStartsWith(Object s, Object prefix)  { return str(s, "strStartsWith").startsWith(str(prefix, "strStartsWith")); }
    public static @NotNull @Unmodifiable Object string_strEndsWith(Object s, Object suffix)    { return str(s, "strEndsWith").endsWith(str(suffix, "strEndsWith")); }
    public static Object string_strIndexOf(Object s, Object sub)        { return (double) str(s, "strIndexOf").indexOf(str(sub, "strIndexOf")); }
    public static @NotNull @Unmodifiable Object string_strReplace(Object s, Object from, Object to)    { return str(s, "strReplace").replace(str(from, "strReplace"), str(to, "strReplace")); }
    public static @NotNull @Unmodifiable Object string_strReplaceAll(Object s, Object re, Object to)   { return str(s, "strReplaceAll").replaceAll(str(re, "strReplaceAll"), str(to, "strReplaceAll")); }
    public static @NotNull @Unmodifiable Object string_strRepeat(Object s, Object n)           { return str(s, "strRepeat").repeat((int) num(n, "strRepeat")); }
    public static @NotNull @Unmodifiable Object string_strReverse(Object s)                    { return new StringBuilder(str(s, "strReverse")).reverse().toString(); }
    public static @NotNull @Unmodifiable Object string_strCharAt(Object s, Object i)           { return String.valueOf(str(s, "strCharAt").charAt((int) num(i, "strCharAt"))); }
    public static @NotNull @Unmodifiable Object string_strIsEmpty(Object s)                    { return str(s, "strIsEmpty").isBlank(); }
    @Contract(value = "null -> !null", pure = true)
    public static @Unmodifiable Object string_strFromNumber(Object n)                 { return String.valueOf(n); }
    public static @NotNull @Unmodifiable Object string_strFormat(Object fmt, Object arg)       { return String.format(str(fmt, "strFormat"), arg); }

    public static @NotNull @Unmodifiable Object string_strSlice(Object s, Object from, Object to) {
        String sv = str(s, "strSlice");
        int f = (int) num(from, "strSlice"), t = (int) num(to, "strSlice");
        return sv.substring(Math.max(0, f), Math.min(sv.length(), t));
    }

    public static @NotNull Object string_strSplit(Object s, Object delim) {
        String[] parts = str(s, "strSplit").split(Pattern.quote(str(delim, "strSplit")));
        List<Object> result = new ArrayList<>(parts.length);
        result.addAll(Arrays.asList(parts));
        return result;
    }

    @Contract("null, _ -> fail")
    public static @NotNull @Unmodifiable Object string_strJoin(Object list, Object glue) {
        if (!(list instanceof List<?> l)) throw new LarvRuntimeException("strJoin(): first argument must be an array");
        String sep = str(glue, "strJoin");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) { if (i > 0) sb.append(sep); sb.append(l.get(i)); }
        return sb.toString();
    }

    public static @NotNull @Unmodifiable Object string_strToNumber(Object s) {
        try { return Double.parseDouble(str(s, "strToNumber")); }
        catch (NumberFormatException e) { throw new LarvRuntimeException("strToNumber(): not a valid number"); }
    }

    public static Object string_strPadLeft(Object s, Object n, Object ch) {
        String sv = str(s, "strPadLeft"); int len = (int) num(n, "strPadLeft"); String pad = str(ch, "strPadLeft");
        if (pad.isEmpty()) pad = " ";
        while (sv.length() < len) sv = pad + sv;
        return sv;
    }

    public static Object string_strPadRight(Object s, Object n, Object ch) {
        String sv = str(s, "strPadRight"); int len = (int) num(n, "strPadRight"); String pad = str(ch, "strPadRight");
        if (pad.isEmpty()) pad = " ";
        while (sv.length() < len) sv = sv + pad;
        return sv;
    }

    public static @NotNull Object string_strChars(Object s) {
        String sv = str(s, "strChars");
        List<Object> r = new ArrayList<>(sv.length());
        for (char c : sv.toCharArray()) r.add(String.valueOf(c));
        return r;
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