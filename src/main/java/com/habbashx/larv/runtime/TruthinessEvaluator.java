package com.habbashx.larv.runtime;

/**
 * Decides whether a runtime value is considered truthy in Larv.
 *
 * <p>Used by all conditional constructs ({@code if}, {@code while}, {@code for})
 * to reduce an arbitrary runtime value to a {@code boolean}.</p>
 *
 * <h2>Truthiness rules</h2>
 * <table border="1">
 *   <tr><th>Type</th>         <th>Truthy when</th></tr>
 *   <tr><td>{@code nil}</td>  <td>never</td></tr>
 *   <tr><td>Boolean</td>      <td>value is {@code true}</td></tr>
 *   <tr><td>Integer</td>      <td>value is non-zero</td></tr>
 *   <tr><td>Double</td>       <td>value is non-zero</td></tr>
 *   <tr><td>String</td>       <td>non-empty string</td></tr>
 *   <tr><td>anything else</td><td>always truthy</td></tr>
 * </table>
 *
 * <p>This class is a utility class and cannot be instantiated.</p>
 */
public final class TruthinessEvaluator {

    private TruthinessEvaluator() {}

    /**
     * Returns {@code true} if {@code value} is considered truthy by Larv's rules.
     *
     * @param value the runtime value to test; {@code null} represents {@code nil}
     * @return {@code true} if the value is truthy
     */
    public static boolean isTruthy(Object value) {
        if (value == null)          return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Integer i) return i != 0;
        if (value instanceof Double d)  return d != 0.0;
        if (value instanceof String s)  return !s.isEmpty();
        return true;
    }
}
