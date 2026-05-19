package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * String standard library — import string
 *
 *   strLen(s)              → number   character count
 *   strUpper(s)            → string   uppercase
 *   strLower(s)            → string   lowercase
 *   strTrim(s)             → string   strip leading/trailing whitespace
 *   strTrimLeft(s)         → string   strip leading whitespace
 *   strTrimRight(s)        → string   strip trailing whitespace
 *   strContains(s, sub)    → boolean  true if s contains sub
 *   strStartsWith(s, pre)  → boolean  true if s starts with pre
 *   strEndsWith(s, suf)    → boolean  true if s ends with suf
 *   strIndexOf(s, sub)     → number   first index of sub, -1 if not found
 *   strSlice(s, from, to)  → string   substring [from, to)
 *   strReplace(s, old, new) → string  replace first occurrence
 *   strReplaceAll(s,old,new)→ string  replace all occurrences
 *   strSplit(s, delim)     → array    split by delimiter
 *   strJoin(array, glue)   → string   join array with glue
 *   strRepeat(s, n)        → string   repeat s n times
 *   strReverse(s)          → string   reverse characters
 *   strCharAt(s, i)        → string   single char at index
 *   strToNumber(s)         → number   parse string as number
 *   strFromNumber(n)       → string   number to string
 *   strIsEmpty(s)          → boolean  true if empty or whitespace-only
 *   strPadLeft(s,n,ch)     → string   left-pad to length n with ch
 *   strPadRight(s,n,ch)    → string   right-pad to length n with ch
 *   strChars(s)            → array    array of individual characters
 */
@Native("String Library")
public class NativeStringLibrary implements NativeLibrary {

    private final ExecutionContext context;

    @Contract(pure = true)
    public NativeStringLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("strLen",         this::strLen);
        context.registerNative("strUpper",       this::strUpper);
        context.registerNative("strLower",       this::strLower);
        context.registerNative("strTrim",        this::strTrim);
        context.registerNative("strTrimLeft",    this::strTrimLeft);
        context.registerNative("strTrimRight",   this::strTrimRight);
        context.registerNative("strContains",    this::strContains);
        context.registerNative("strStartsWith",  this::strStartsWith);
        context.registerNative("strEndsWith",    this::strEndsWith);
        context.registerNative("strIndexOf",     this::strIndexOf);
        context.registerNative("strSlice",       this::strSlice);
        context.registerNative("strReplace",     this::strReplace);
        context.registerNative("strReplaceAll",  this::strReplaceAll);
        context.registerNative("strSplit",       this::strSplit);
        context.registerNative("strJoin",        this::strJoin);
        context.registerNative("strRepeat",      this::strRepeat);
        context.registerNative("strReverse",     this::strReverse);
        context.registerNative("strCharAt",      this::strCharAt);
        context.registerNative("strToNumber",    this::strToNumber);
        context.registerNative("strFromNumber",  this::strFromNumber);
        context.registerNative("strIsEmpty",     this::strIsEmpty);
        context.registerNative("strPadLeft",     this::strPadLeft);
        context.registerNative("strPadRight",    this::strPadRight);
        context.registerNative("strChars",       this::strChars);
    }

    private String str(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    private double num(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof Double d))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a number", -1, LarvError.Kind.RUNTIME);
        return (Double) args.get(i);
    }

    private Object strLen(List<Object> a)        { return (double) str(a,0,"strLen").length(); }
    private @NotNull @Unmodifiable Object strUpper(List<Object> a)      { return str(a,0,"strUpper").toUpperCase(); }
    private @NotNull @Unmodifiable Object strLower(List<Object> a)      { return str(a,0,"strLower").toLowerCase(); }
    private @NotNull @Unmodifiable Object strTrim(List<Object> a)       { return str(a,0,"strTrim").strip(); }
    private @NotNull @Unmodifiable Object strTrimLeft(List<Object> a)   { return str(a,0,"strTrimLeft").stripLeading(); }
    private @NotNull @Unmodifiable Object strTrimRight(List<Object> a)  { return str(a,0,"strTrimRight").stripTrailing(); }
    private @NotNull @Unmodifiable Object strIsEmpty(List<Object> a)    { return str(a,0,"strIsEmpty").isBlank(); }
    private @NotNull @Unmodifiable Object strReverse(List<Object> a)    { return new StringBuilder(str(a,0,"strReverse")).reverse().toString(); }

    private @NotNull @Unmodifiable Object strContains(List<Object> a)   { return str(a,0,"strContains").contains(str(a,1,"strContains")); }
    private @NotNull @Unmodifiable Object strStartsWith(List<Object> a) { return str(a,0,"strStartsWith").startsWith(str(a,1,"strStartsWith")); }
    private @NotNull @Unmodifiable Object strEndsWith(List<Object> a)   { return str(a,0,"strEndsWith").endsWith(str(a,1,"strEndsWith")); }
    private Object strIndexOf(List<Object> a)    { return (double) str(a,0,"strIndexOf").indexOf(str(a,1,"strIndexOf")); }
    private @NotNull @Unmodifiable Object strReplace(List<Object> a)    { return str(a,0,"strReplace").replace(str(a,1,"strReplace"), str(a,2,"strReplace")); }
    private @NotNull @Unmodifiable Object strReplaceAll(List<Object> a) { return str(a,0,"strReplaceAll").replaceAll(str(a,1,"strReplaceAll"), str(a,2,"strReplaceAll")); }

    private @NotNull @Unmodifiable Object strSlice(List<Object> a) {
        String s = str(a,0,"strSlice");
        int from = (int) num(a,1,"strSlice");
        int to   = (int) num(a,2,"strSlice");
        if (from < 0 || to > s.length() || from > to)
            throw new LarvError("strSlice(): index out of bounds [" + from + ", " + to + ") on string of length " + s.length(), -1, LarvError.Kind.RUNTIME);
        return s.substring(from, to);
    }

    private @NotNull @Unmodifiable Object strCharAt(List<Object> a) {
        String s = str(a,0,"strCharAt");
        int i = (int) num(a,1,"strCharAt");
        if (i < 0 || i >= s.length())
            throw new LarvError("strCharAt(): index " + i + " out of bounds", -1, LarvError.Kind.RUNTIME);
        return String.valueOf(s.charAt(i));
    }

    private @NotNull Object strSplit(List<Object> a) {
        String[] parts = str(a,0,"strSplit").split(str(a,1,"strSplit"), -1);
        List<Object> out = new ArrayList<>();
        Collections.addAll(out, parts);
        return out;
    }

    private @NotNull @Unmodifiable Object strJoin(@NotNull List<Object> a) {
        if (!(a.getFirst() instanceof List<?> list))
            throw new LarvError("strJoin(): argument 1 must be an array", -1, LarvError.Kind.RUNTIME);
        String glue = str(a,1,"strJoin");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(glue);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private @NotNull @Unmodifiable Object strRepeat(List<Object> a) {
        return str(a,0,"strRepeat").repeat((int) num(a,1,"strRepeat"));
    }

    private @NotNull @Unmodifiable Object strToNumber(List<Object> a) {
        try { return Double.parseDouble(str(a,0,"strToNumber")); }
        catch (NumberFormatException e) { throw new LarvError("strToNumber(): cannot parse '" + a.get(0) + "' as a number", -1, LarvError.Kind.RUNTIME); }
    }

    private @NotNull @Unmodifiable Object strFromNumber(List<Object> a) {
        double n = num(a,0,"strFromNumber");
        return (n == Math.floor(n) && !Double.isInfinite(n)) ? String.valueOf((long)n) : String.valueOf(n);
    }

    private Object strPadLeft(List<Object> a) {
        String s = str(a,0,"strPadLeft");
        int n    = (int) num(a,1,"strPadLeft");
        String ch = str(a,2,"strPadLeft");
        if (ch.isEmpty()) ch = " ";
        while (s.length() < n) s = ch.charAt(0) + s;
        return s;
    }

    private Object strPadRight(List<Object> a) {
        String s = str(a,0,"strPadRight");
        int n    = (int) num(a,1,"strPadRight");
        String ch = str(a,2,"strPadRight");
        if (ch.isEmpty()) ch = " ";
        while (s.length() < n) s = s + ch.charAt(0);
        return s;
    }

    private @NotNull Object strChars(List<Object> a) {
        String s = str(a,0,"strChars");
        List<Object> out = new ArrayList<>(s.length());
        for (char c : s.toCharArray()) out.add(String.valueOf(c));
        return out;
    }
}