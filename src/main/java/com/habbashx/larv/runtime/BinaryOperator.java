package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.NotNull;

/**
 * Stateless utility that implements every binary and arithmetic operation
 * available in Larv.
 *
 * <p>At runtime, {@link ExpressionEvaluator} evaluates both operands and then
 * delegates to {@link #apply(String, Object, Object)} with the operator symbol
 * as a string.  No switch/case duplication exists anywhere else — this class
 * is the single source of truth for every operator's semantics.</p>
 *
 * <h2>Special cases</h2>
 * <ul>
 *   <li>{@code +} — if either operand is a {@code String}, both are stringified
 *       and concatenated; otherwise numeric addition is performed.</li>
 *   <li>{@code /} — division by zero throws a {@link LarvError}.</li>
 *   <li>{@code ==} / {@code !=} — null-safe; two {@code nil} values are
 *       equal, {@code nil} and any other value are not; numeric types are
 *       compared by value regardless of Java wrapper type.</li>
 * </ul>
 *
 * <p>This class is a utility class and cannot be instantiated.</p>
 */
public final class BinaryOperator {

    private BinaryOperator() {}

    /**
     * Applies the given binary operator to two already-evaluated operands.
     *
     * @param op    the operator symbol: {@code "+"}, {@code "-"}, {@code "*"},
     *              {@code "/"}, {@code "=="}, {@code "!="}, {@code "<"},
     *              {@code ">"}, {@code "<="}, {@code ">="}
     * @param left  the left operand (any Larv runtime value)
     * @param right the right operand (any Larv runtime value)
     * @return the result of the operation
     * @throws LarvError if an operand is the wrong type or division by zero occurs
     */
    public static Object apply(String op, Object left, Object right) {
        return switch (op) {
            case "==" -> nullSafeEquals(left, right);
            case "!=" -> !((boolean) nullSafeEquals(left, right));
            case "+"  -> addOrConcat(left, right);
            case "-"  -> toDouble(left, op) - toDouble(right, op);
            case "*"  -> toDouble(left, op) * toDouble(right, op);
            case "/" -> {
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

    /**
     * Null-safe equality check.
     *
     * <ul>
     *   <li>Two {@code null} values (both {@code nil}) are equal.</li>
     *   <li>{@code null} and a non-null value are never equal.</li>
     *   <li>Two numeric values are compared by {@code double} value,
     *       regardless of Java wrapper type.</li>
     *   <li>All other values use {@link Object#equals}.</li>
     * </ul>
     *
     * @param left  left operand
     * @param right right operand
     * @return {@code true} if the two values are considered equal
     */
    private static @NotNull Object nullSafeEquals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        if (isNumeric(left) && isNumeric(right))
            return toDouble(left, "==") == toDouble(right, "==");
        return left.equals(right);
    }

    /**
     * Implements the {@code +} operator: numeric addition when both sides are
     * numbers, string concatenation when either side is a string.
     *
     * @param left  left operand
     * @param right right operand
     * @return a {@code Double} (addition) or {@code String} (concatenation)
     */
    private static @NotNull Object addOrConcat(Object left, Object right) {
        if (left instanceof String || right instanceof String)
            return stringify(left) + stringify(right);
        return toDouble(left, "+") + toDouble(right, "+");
    }

    /**
     * Coerces a runtime value to a Java {@code double}.
     *
     * <p>Accepts {@link Double}, {@link Integer}, {@link Long}, {@link Float},
     * and numeric strings.  Throws for {@code null} ({@code nil}) or any other
     * type.</p>
     *
     * @param v  the value to coerce
     * @param op the operator name, used only for the error message
     * @return the coerced {@code double}
     * @throws LarvError if {@code v} cannot be converted to a number
     */
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

    /**
     * Returns {@code true} if {@code v} is one of the Java numeric wrapper
     * types that Larv treats as a number.
     *
     * @param v the value to test
     * @return {@code true} for {@link Double}, {@link Integer}, {@link Long}, {@link Float}
     */
    private static boolean isNumeric(Object v) {
        return v instanceof Double || v instanceof Integer || v instanceof Long || v instanceof Float;
    }

    /**
     * Converts any Larv runtime value to its canonical string representation.
     *
     * <ul>
     *   <li>{@code null} → {@code "nil"}</li>
     *   <li>Whole-number {@link Double} (e.g. {@code 42.0}) → {@code "42"} (no decimal)</li>
     *   <li>Everything else → {@link Object#toString()}</li>
     * </ul>
     *
     * @param v the value to stringify; {@code null} is permitted
     * @return the string representation
     */
    public static String stringify(Object v) {
        if (v == null) return "nil";
        if (v instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf(d.longValue());
        return v.toString();
    }
}