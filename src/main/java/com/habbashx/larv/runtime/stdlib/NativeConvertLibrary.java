package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert standard library — import "convert"
 *
 *   toNumber(val)      → number   parse string or boolean to number
 *   toString(val)      → string   convert any value to its string representation
 *   toBool(val)        → boolean  convert value to boolean
 *   toInt(val)         → number   truncate number to integer (floor toward zero)
 *   toHex(n)           → string   integer → lowercase hex string  (255 → "ff")
 *   toOctal(n)         → string   integer → octal string          (8   → "10")
 *   toBinary(n)        → string   integer → binary string         (10  → "1010")
 *   fromHex(str)       → number   hex string → number             ("ff" → 255)
 *   fromOctal(str)     → number   octal string → number           ("10" → 8)
 *   fromBinary(str)    → number   binary string → number          ("1010" → 10)
 *   toBytes(str)       → array    string → array of byte values
 *   fromBytes(array)   → string   array of byte values → string
 *   typeOf(val)        → string   runtime type name of a value
 */
@Native("Converter Library")
public class NativeConvertLibrary implements NativeLibrary {

    private final ExecutionContext context;

    public NativeConvertLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("toNumber",   this::toNumber);
        context.registerNative("toString",   this::toStr);
        context.registerNative("toBool",     this::toBool);
        context.registerNative("toInt",      this::toInt);
        context.registerNative("toHex",      this::toHex);
        context.registerNative("toOctal",    this::toOctal);
        context.registerNative("toBinary",   this::toBinary);
        context.registerNative("fromHex",    this::fromHex);
        context.registerNative("fromOctal",  this::fromOctal);
        context.registerNative("fromBinary", this::fromBinary);
        context.registerNative("toBytes",    this::toBytes);
        context.registerNative("fromBytes",  this::fromBytes);
        context.registerNative("typeOf",     this::typeOf);
    }

    private Object requireArg(@NotNull List<Object> args, String fn) {
        if (args.isEmpty()) throw new LarvError(fn + "(): expected 1 argument", -1, LarvError.Kind.RUNTIME);
        return args.get(0);
    }

    private double numArg(@NotNull List<Object> args, String fn) {
        Object v = requireArg(args, fn);
        if (v instanceof Double d) return d;
        throw new LarvError(fn + "(): argument must be a number", -1, LarvError.Kind.RUNTIME);
    }

    private Object toNumber(@NotNull List<Object> args) {
        Object v = requireArg(args, "toNumber");
        if (v instanceof Double d)  return d;
        if (v instanceof Boolean b) return b ? 1.0 : 0.0;
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); }
            catch (NumberFormatException e) {
                throw new LarvError("toNumber(): cannot convert \"" + s + "\" to a number", -1, LarvError.Kind.RUNTIME);
            }
        }
        if (v == null) return 0.0;
        throw new LarvError("toNumber(): cannot convert " + typeName(v) + " to a number", -1, LarvError.Kind.RUNTIME);
    }

    private @Unmodifiable Object toStr(@NotNull List<Object> args) {
        Object v = requireArg(args, "toString");
        return stringify(v);
    }

    private Object toBool(@NotNull List<Object> args) {
        Object v = requireArg(args, "toBool");
        if (v instanceof Boolean b) return b;
        if (v instanceof Double d)  return d != 0.0;
        if (v instanceof String s)  return switch (s.trim().toLowerCase()) {
            case "true", "yes", "1", "on"  -> true;
            case "false", "no", "0", "off" -> false;
            default -> throw new LarvError("toBool(): cannot convert \"" + s + "\" to boolean", -1, LarvError.Kind.RUNTIME);
        };
        if (v == null) return false;
        return true;
    }

    private Object toInt(@NotNull List<Object> args) {
        double n = numArg(args, "toInt");
        return (double) (long) n;
    }

    private @NotNull @Unmodifiable Object toHex(@NotNull List<Object> args) {
        long n = (long) numArg(args, "toHex");
        return Long.toHexString(n);
    }

    private @NotNull @Unmodifiable Object toOctal(@NotNull List<Object> args) {
        long n = (long) numArg(args, "toOctal");
        return Long.toOctalString(n);
    }

    private @NotNull @Unmodifiable Object toBinary(@NotNull List<Object> args) {
        long n = (long) numArg(args, "toBinary");
        return Long.toBinaryString(n);
    }

    private Object fromHex(@NotNull List<Object> args) {
        String s = strArg(args, "fromHex");
        try { return (double) Long.parseLong(s.trim(), 16); }
        catch (NumberFormatException e) {
            throw new LarvError("fromHex(): invalid hex string \"" + s + "\"", -1, LarvError.Kind.RUNTIME);
        }
    }

    private Object fromOctal(@NotNull List<Object> args) {
        String s = strArg(args, "fromOctal");
        try { return (double) Long.parseLong(s.trim(), 8); }
        catch (NumberFormatException e) {
            throw new LarvError("fromOctal(): invalid octal string \"" + s + "\"", -1, LarvError.Kind.RUNTIME);
        }
    }

    private Object fromBinary(@NotNull List<Object> args) {
        String s = strArg(args, "fromBinary");
        try { return (double) Long.parseLong(s.trim(), 2); }
        catch (NumberFormatException e) {
            throw new LarvError("fromBinary(): invalid binary string \"" + s + "\"", -1, LarvError.Kind.RUNTIME);
        }
    }

    private Object toBytes(@NotNull List<Object> args) {
        String s = strArg(args, "toBytes");
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        List<Object> out = new ArrayList<>(bytes.length);
        for (byte b : bytes) out.add((double) (b & 0xFF));
        return out;
    }

    @Contract("_ -> new")
    @SuppressWarnings("unchecked")
    private @NotNull Object fromBytes(@NotNull List<Object> args) {
        Object v = requireArg(args, "fromBytes");
        if (!(v instanceof List)) throw new LarvError("fromBytes(): argument must be a list", -1, LarvError.Kind.RUNTIME);
        List<Object> list = (List<Object>) v;
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Double d))
                throw new LarvError("fromBytes(): list element " + i + " must be a number", -1, LarvError.Kind.RUNTIME);
            bytes[i] = d.byteValue();
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private @NotNull @Unmodifiable Object typeOf(@NotNull List<Object> args) {
        return typeName(requireArg(args, "typeOf"));
    }

    private String strArg(@NotNull List<Object> args, String fn) {
        Object v = requireArg(args, fn);
        if (!(v instanceof String s)) throw new LarvError(fn + "(): argument must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    private @NotNull String typeName(Object v) {
        if (v == null)               return "nil";
        if (v instanceof Boolean)    return "bool";
        if (v instanceof Double)     return "number";
        if (v instanceof String)     return "string";
        if (v instanceof List)       return "list";
        if (v instanceof java.util.Map) return "map";
        return v.getClass().getSimpleName();
    }

    @Contract("")
    private String stringify(Object v) {
        if (v == null) return "nil";
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf(d.longValue());
            return d.toString();
        }
        return v.toString();
    }
}