package com.habbashx.larv.runtime.stdlib;
import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Math standard library for Larv.
 *
 * Available functions:
 *
 *   sqrt(n)         → number   — square root
 *   pow(base, exp)  → number   — base raised to exp
 *   abs(n)          → number   — absolute value
 *   floor(n)        → number   — round down
 *   ceil(n)         → number   — round up
 *   round(n)        → number   — round to nearest integer
 *   max(a, b)       → number   — larger of two numbers
 *   min(a, b)       → number   — smaller of two numbers
 *   log(n)          → number   — natural logarithm
 *   log10(n)        → number   — base-10 logarithm
 *   sin(n)          → number   — sine (radians)
 *   cos(n)          → number   — cosine (radians)
 *   tan(n)          → number   — tangent (radians)
 *   asin(n)         → number   — arcsine (radians)
 *   acos(n)         → number   — arccosine (radians)
 *   atan(n)         → number   — arctangent (radians)
 *   atan2(y, x)     → number   — two-argument arctangent
 *   toRadians(n)    → number   — degrees to radians
 *   toDegrees(n)    → number   — radians to degrees
 *   random()        → number   — random number in [0, 1)
 *   randomInt(a, b) → number   — random integer in [a, b]
 *   clamp(n, lo,hi) → number   — clamp n between lo and hi
 *   sign(n)         → number   — -1, 0, or 1
 *   pi()            → number   — π
 *   e()             → number   — Euler's number
 *   isNaN(n)        → boolean  — check if value is NaN
 *   isInfinite(n)   → boolean  — check if value is infinite
 *   toInt(n)        → number   — truncate to integer
 */
@Native("Math Library")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeMathLibrary extends NativeLibrary {


    public NativeMathLibrary(ExecutionContext context) {
        super(context);
    }

    @Override
    public void registerAll() {
        getExecutionContext().registerNative("sqrt",      this::sqrt);
        getExecutionContext().registerNative("pow",       this::pow);
        getExecutionContext().registerNative("abs",       this::abs);
        getExecutionContext().registerNative("floor",     this::floor);
        getExecutionContext().registerNative("ceil",      this::ceil);
        getExecutionContext().registerNative("round",     this::round);
        getExecutionContext().registerNative("max",       this::max);
        getExecutionContext().registerNative("min",       this::min);
        getExecutionContext().registerNative("log",       this::log);
        getExecutionContext().registerNative("log10",     this::log10);
        getExecutionContext().registerNative("sin",       this::sin);
        getExecutionContext().registerNative("cos",       this::cos);
        getExecutionContext().registerNative("tan",       this::tan);
        getExecutionContext().registerNative("asin",      this::asin);
        getExecutionContext().registerNative("acos",      this::acos);
        getExecutionContext().registerNative("atan",      this::atan);
        getExecutionContext().registerNative("atan2",     this::atan2);
        getExecutionContext().registerNative("toRadians", this::toRadians);
        getExecutionContext().registerNative("toDegrees", this::toDegrees);
        getExecutionContext().registerNative("random",    this::random);
        getExecutionContext().registerNative("randomInt", this::randomInt);
        getExecutionContext().registerNative("clamp",     this::clamp);
        getExecutionContext().registerNative("sign",      this::sign);
        getExecutionContext().registerNative("pi",        this::pi);
        getExecutionContext().registerNative("e",         this::e);
        getExecutionContext().registerNative("isNaN",     this::isNaN);
        getExecutionContext().registerNative("isInfinite",this::isInfinite);
        getExecutionContext().registerNative("toInt",     this::toInt);
    }

    private double numArg(@NotNull List<Object> args, int index, String fnName) {
        if (args.size() <= index)
            throw new LarvError(fnName + "() requires at least " + (index + 1) + " argument(s)", -1, LarvError.Kind.RUNTIME);
        Object v = args.get(index);
        if (v instanceof Double d)  return d;
        if (v instanceof Integer i) return i.doubleValue();
        throw new LarvError(fnName + "() expects a number as argument " + (index + 1) + ", got: " + (v == null ? "nil" : v.getClass().getSimpleName()), -1, LarvError.Kind.RUNTIME);
    }

    private @NotNull @Unmodifiable Object sqrt(List<Object> args)      { return Math.sqrt(numArg(args, 0, "sqrt")); }
    private @NotNull @Unmodifiable Object pow(List<Object> args)       { return Math.pow(numArg(args, 0, "pow"), numArg(args, 1, "pow")); }
    private @NotNull @Unmodifiable Object abs(List<Object> args)       { return Math.abs(numArg(args, 0, "abs")); }
    private @NotNull @Unmodifiable Object floor(List<Object> args)     { return Math.floor(numArg(args, 0, "floor")); }
    private @NotNull @Unmodifiable Object ceil(List<Object> args)      { return Math.ceil(numArg(args, 0, "ceil")); }
    private Object round(List<Object> args)     { return (double) Math.round(numArg(args, 0, "round")); }
    private @NotNull @Unmodifiable Object max(List<Object> args)       { return Math.max(numArg(args, 0, "max"), numArg(args, 1, "max")); }
    private @NotNull @Unmodifiable Object min(List<Object> args)       { return Math.min(numArg(args, 0, "min"), numArg(args, 1, "min")); }
    private @NotNull @Unmodifiable Object log(List<Object> args)       { return Math.log(numArg(args, 0, "log")); }
    private @NotNull @Unmodifiable Object log10(List<Object> args)     { return Math.log10(numArg(args, 0, "log10")); }
    private @NotNull @Unmodifiable Object sin(List<Object> args)       { return Math.sin(numArg(args, 0, "sin")); }
    private @NotNull @Unmodifiable Object cos(List<Object> args)       { return Math.cos(numArg(args, 0, "cos")); }
    private @NotNull @Unmodifiable Object tan(List<Object> args)       { return Math.tan(numArg(args, 0, "tan")); }
    private @NotNull @Unmodifiable Object asin(List<Object> args)      { return Math.asin(numArg(args, 0, "asin")); }
    private @NotNull @Unmodifiable Object acos(List<Object> args)      { return Math.acos(numArg(args, 0, "acos")); }
    private @NotNull @Unmodifiable Object atan(List<Object> args)      { return Math.atan(numArg(args, 0, "atan")); }
    private @NotNull @Unmodifiable Object atan2(List<Object> args)     { return Math.atan2(numArg(args, 0, "atan2"), numArg(args, 1, "atan2")); }
    private @NotNull @Unmodifiable Object toRadians(List<Object> args) { return Math.toRadians(numArg(args, 0, "toRadians")); }
    private @NotNull @Unmodifiable Object toDegrees(List<Object> args) { return Math.toDegrees(numArg(args, 0, "toDegrees")); }
    private @NotNull @Unmodifiable Object random(List<Object> args)    { return Math.random(); }
    private Object pi(List<Object> args)        { return Math.PI; }
    private Object e(List<Object> args)         { return Math.E; }
    private Object toInt(List<Object> args)     { return (double)(long) numArg(args, 0, "toInt"); }

    private Object randomInt(@NotNull List<Object> args) {
        int a = (int) numArg(args, 0, "randomInt");
        int b = (int) numArg(args, 1, "randomInt");
        if (a > b) throw new LarvError("randomInt() requires a <= b", -1, LarvError.Kind.RUNTIME);
        return (double) (a + (int)(Math.random() * (b - a + 1)));
    }

    private @NotNull @Unmodifiable Object clamp(@NotNull List<Object> args) {
        double n  = numArg(args, 0, "clamp");
        double lo = numArg(args, 1, "clamp");
        double hi = numArg(args, 2, "clamp");
        return Math.max(lo, Math.min(hi, n));
    }

    private Object sign(@NotNull List<Object> args) {
        double n = numArg(args, 0, "sign");
        return n > 0 ? 1.0 : n < 0 ? -1.0 : 0.0;
    }

    private @NotNull @Unmodifiable Object isNaN(@NotNull List<Object> args) {
        return Double.isNaN(numArg(args, 0, "isNaN"));
    }

    private @NotNull @Unmodifiable Object isInfinite(@NotNull List<Object> args) {
        return Double.isInfinite(numArg(args, 0, "isInfinite"));
    }
}