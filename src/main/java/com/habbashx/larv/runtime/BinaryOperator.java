package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.NotNull;

public final class BinaryOperator {

    private BinaryOperator() {}

    public static Object apply(String op, Object left, Object right) {
        return switch (op) {
            case "==" -> nullSafeEquals(left, right);
            case "!=" -> !((boolean) nullSafeEquals(left, right));
            case "+"  -> addOrConcat(left, right);
            case "-"  -> toDouble(left, op) - toDouble(right, op);
            case "*"  -> toDouble(left, op) * toDouble(right, op);
            case "/"  -> {
                double r = toDouble(right, op);
                if (r == 0) throw new LarvError("Division by zero");
                yield toDouble(left, op) / r;
            }
            case "<"  -> toDouble(left, op) <  toDouble(right, op);
            case ">"  -> toDouble(left, op) >  toDouble(right, op);
            case "<=" -> toDouble(left, op) <= toDouble(right, op);
            case ">=" -> toDouble(left, op) >= toDouble(right, op);
            default   -> throw new LarvError("Unknown operator '" + op + "'");
        };
    }

    private static @NotNull Object nullSafeEquals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        if (isNumeric(left) && isNumeric(right))
            return toDouble(left, "==") == toDouble(right, "==");
        return left.equals(right);
    }

    private static @NotNull Object addOrConcat(Object left, Object right) {
        if (left instanceof String || right instanceof String)
            return stringify(left) + stringify(right);
        return toDouble(left, "+") + toDouble(right, "+");
    }

    private static double toDouble(Object v, String op) {
        if (v instanceof Double d)  return d;
        if (v instanceof Integer i) return i.doubleValue();
        if (v instanceof Long l)    return l.doubleValue();
        if (v instanceof Float f)   return f.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) {
                throw new LarvError("Cannot use string '" + s + "' as a number in '" + op + "' operation");
            }
        }
        if (v == null) throw new LarvError("Cannot use 'nil' as a number in '" + op + "' operation");
        throw new LarvError("Expected a number for '" + op + "', got: " + v.getClass().getSimpleName());
    }

    private static boolean isNumeric(Object v) {
        return v instanceof Double || v instanceof Integer || v instanceof Long || v instanceof Float;
    }

    public static String stringify(Object v) {
        if (v == null) return "nil";
        if (v instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf(d.longValue());
        return v.toString();
    }
}
