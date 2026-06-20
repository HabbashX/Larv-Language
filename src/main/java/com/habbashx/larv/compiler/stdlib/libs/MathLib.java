package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class MathLib implements LarvStdlib {

    private static final Random RNG = new Random();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "math"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("sqrt",       "math_sqrt"),
                Map.entry("pow",        "math_pow"),
                Map.entry("abs",        "math_abs"),
                Map.entry("floor",      "math_floor"),
                Map.entry("ceil",       "math_ceil"),
                Map.entry("round",      "math_round"),
                Map.entry("max",        "math_max"),
                Map.entry("min",        "math_min"),
                Map.entry("log",        "math_log"),
                Map.entry("log10",      "math_log10"),
                Map.entry("sin",        "math_sin"),
                Map.entry("cos",        "math_cos"),
                Map.entry("tan",        "math_tan"),
                Map.entry("asin",       "math_asin"),
                Map.entry("acos",       "math_acos"),
                Map.entry("atan",       "math_atan"),
                Map.entry("atan2",      "math_atan2"),
                Map.entry("toRadians",  "math_toRadians"),
                Map.entry("toDegrees",  "math_toDegrees"),
                Map.entry("random",     "math_random"),
                Map.entry("randomInt",  "math_randomInt"),
                Map.entry("clamp",      "math_clamp"),
                Map.entry("sign",       "math_sign"),
                Map.entry("pi",         "math_pi"),
                Map.entry("e",          "math_e"),
                Map.entry("isNaN",      "math_isNaN"),
                Map.entry("isInfinite", "math_isInfinite"),
                Map.entry("toInt",      "math_toInt")
        );
    }

    public static @NotNull @Unmodifiable Object math_sqrt(Object n)              { return Math.sqrt(num(n, "sqrt")); }
    public static @NotNull @Unmodifiable Object math_pow(Object b, Object e)     { return Math.pow(num(b, "pow"), num(e, "pow")); }
    public static @NotNull @Unmodifiable Object math_abs(Object n)               { return Math.abs(num(n, "abs")); }
    public static @NotNull @Unmodifiable Object math_floor(Object n)             { return Math.floor(num(n, "floor")); }
    public static @NotNull @Unmodifiable Object math_ceil(Object n)              { return Math.ceil(num(n, "ceil")); }
    public static Object math_round(Object n)             { return (double) Math.round(num(n, "round")); }
    public static @NotNull @Unmodifiable Object math_max(Object a, Object b)     { return Math.max(num(a, "max"), num(b, "max")); }
    public static @NotNull @Unmodifiable Object math_min(Object a, Object b)     { return Math.min(num(a, "min"), num(b, "min")); }
    public static @NotNull @Unmodifiable Object math_log(Object n)               { return Math.log(num(n, "log")); }
    public static @NotNull @Unmodifiable Object math_log10(Object n)             { return Math.log10(num(n, "log10")); }
    public static @NotNull @Unmodifiable Object math_sin(Object n)               { return Math.sin(num(n, "sin")); }
    public static @NotNull @Unmodifiable Object math_cos(Object n)               { return Math.cos(num(n, "cos")); }
    public static @NotNull @Unmodifiable Object math_tan(Object n)               { return Math.tan(num(n, "tan")); }
    public static @NotNull @Unmodifiable Object math_asin(Object n)              { return Math.asin(num(n, "asin")); }
    public static @NotNull @Unmodifiable Object math_acos(Object n)              { return Math.acos(num(n, "acos")); }
    public static @NotNull @Unmodifiable Object math_atan(Object n)              { return Math.atan(num(n, "atan")); }
    public static @NotNull @Unmodifiable Object math_atan2(Object y, Object x)  { return Math.atan2(num(y, "atan2"), num(x, "atan2")); }
    public static @NotNull @Unmodifiable Object math_toRadians(Object n)         { return Math.toRadians(num(n, "toRadians")); }
    public static @NotNull @Unmodifiable Object math_toDegrees(Object n)         { return Math.toDegrees(num(n, "toDegrees")); }
    public static @NotNull @Unmodifiable Object math_random()                    { return Math.random(); }
    public static Object math_randomInt(Object lo, Object hi) {
        int l = (int) num(lo, "randomInt"), h = (int) num(hi, "randomInt");
        if (l > h) throw new LarvRuntimeException("randomInt(): lo > hi");
        return (double)(l + RNG.nextInt(h - l + 1));
    }
    public static @NotNull @Unmodifiable Object math_clamp(Object n, Object lo, Object hi) {
        return Math.max(num(lo, "clamp"), Math.min(num(hi, "clamp"), num(n, "clamp")));
    }
    public static Object math_sign(Object n)   { double d = num(n, "sign"); return d > 0 ? 1.0 : d < 0 ? -1.0 : 0.0; }
    @Contract(pure = true)
    public static Object math_pi()             { return Math.PI; }
    @Contract(pure = true)
    public static Object math_e()              { return Math.E; }
    public static @NotNull @Unmodifiable Object math_isNaN(Object n)       { return Double.isNaN(num(n, "isNaN")); }
    public static @NotNull @Unmodifiable Object math_isInfinite(Object n)  { return Double.isInfinite(num(n, "isInfinite")); }
    public static Object math_toInt(Object n)       { return (double)(long) num(n, "toInt"); }

    @Contract("null, _ -> fail")
    static double num(Object v, String fn) {
        if (v instanceof Double d) return d;
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) { try { return Double.parseDouble(s); } catch (NumberFormatException e) { throw new LarvRuntimeException(fn + "(): not a number: " + s); } }
        throw new LarvRuntimeException(fn + "(): argument must be a number");
    }
}