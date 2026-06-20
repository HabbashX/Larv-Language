package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Built-in method dispatcher for Larv array values.
 *
 * <p>Arrays are represented as {@code java.util.ArrayList<Object>} at runtime.
 * When the evaluator encounters {@code arr.method(args)} and the receiver is a
 * {@code List}, it calls {@link #dispatch} to handle the method call without
 * requiring the array to be wrapped in a dedicated class.</p>
 *
 * <h2>Available methods</h2>
 * <table border="1">
 *   <tr><th>Method</th>         <th>Args</th>          <th>Returns</th>      <th>Description</th></tr>
 *   <tr><td>{@code push}</td>   <td>value</td>          <td>nil</td>          <td>Appends to end</td></tr>
 *   <tr><td>{@code pop}</td>    <td></td>               <td>removed value</td><td>Removes &amp; returns last</td></tr>
 *   <tr><td>{@code peek}</td>   <td></td>               <td>last value</td>   <td>Returns last without removing</td></tr>
 *   <tr><td>{@code first}</td>  <td></td>               <td>first value</td>  <td>Returns first element</td></tr>
 *   <tr><td>{@code last}</td>   <td></td>               <td>last value</td>   <td>Returns last element</td></tr>
 *   <tr><td>{@code contains}</td><td>value</td>         <td>boolean</td>      <td>True if element exists</td></tr>
 *   <tr><td>{@code indexOf}</td><td>value</td>          <td>number</td>       <td>First index, or -1</td></tr>
 *   <tr><td>{@code isEmpty}</td><td></td>               <td>boolean</td>      <td>True if size is 0</td></tr>
 *   <tr><td>{@code clear}</td>  <td></td>               <td>nil</td>          <td>Removes all elements</td></tr>
 *   <tr><td>{@code reverse}</td><td></td>               <td>nil</td>          <td>Reverses in place</td></tr>
 *   <tr><td>{@code remove}</td> <td>index</td>          <td>removed value</td><td>Removes element at index</td></tr>
 *   <tr><td>{@code slice}</td>  <td>from, to</td>       <td>new array</td>    <td>Sub-array [from, to)</td></tr>
 *   <tr><td>{@code join}</td>   <td>separator?</td>     <td>string</td>       <td>Joins with separator</td></tr>
 * </table>
 *
 * <p>This class is a utility class and cannot be instantiated.</p>
 */
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class ArrayMethods {

    /**
     * Dispatches a method call on a Larv array value.
     *
     * @param array  the target array
     * @param method the method name
     * @param args   the evaluated arguments
     * @return the method result, or {@code null} for side-effect-only methods
     * @throws LarvError if the method name is unknown or arguments are invalid
     */
    public static @Nullable Object dispatch(@NotNull List<Object> array,
                                            @NotNull String method,
                                            @NotNull List<Object> args) {
        return switch (method) {
            case "push"     -> { array.add(args.getFirst()); yield null; }
            case "pop"      -> {
                if (array.isEmpty()) throw new LarvError("pop() called on an empty array");
                yield array.removeLast();
            }
            case "peek"     -> {
                if (array.isEmpty()) throw new LarvError("peek() called on an empty array");
                yield array.getLast();
            }
            case "first"    -> {
                if (array.isEmpty()) throw new LarvError("first() called on an empty array");
                yield array.getFirst();
            }
            case "last"     -> {
                if (array.isEmpty()) throw new LarvError("last() called on an empty array");
                yield array.getLast();
            }
            case "contains" -> array.contains(args.getFirst());
            case "indexOf"  -> (double) array.indexOf(args.getFirst());
            case "isEmpty"  -> array.isEmpty();
            case "clear"    -> { array.clear(); yield null; }
            case "reverse"  -> { Collections.reverse(array); yield null; }
            case "remove"   -> {
                int idx = toIndex(args.getFirst(), "remove");
                if (idx < 0 || idx >= array.size())
                    throw new LarvError("remove() index " + idx + " is out of bounds — array has " + array.size() + " element(s)");
                yield array.remove(idx);
            }
            case "slice"    -> {
                int from = toIndex(args.get(0), "slice");
                int to   = toIndex(args.get(1), "slice");
                if (from > to)
                    throw new LarvError("slice() 'from' (" + from + ") must be <= 'to' (" + to + ")");
                if (from < 0 || to > array.size())
                    throw new LarvError("slice(" + from + ", " + to + ") is out of bounds — array has " + array.size() + " element(s)");
                yield new ArrayList<>(array.subList(from, to));
            }
            case "join"     -> {
                String sep = args.isEmpty() ? "" : BinaryOperator.stringify(args.get(0));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < array.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(BinaryOperator.stringify(array.get(i)));
                }
                yield sb.toString();
            }
            default -> throw new LarvError("Unknown array method '" + method + "' — available: push, pop, peek, first, last, contains, indexOf, isEmpty, clear, reverse, remove, slice, join");
        };
    }

    /**
     * Converts a runtime value to an array index ({@code int}).
     *
     * @param v      the value to convert; must be a {@code Double}
     * @param method the calling method name (used in the error message)
     * @return the integer index
     * @throws LarvError if {@code v} is not a number
     */
    private static int toIndex(Object v, String method) {
        if (v instanceof Double d) return d.intValue();
        throw new LarvError(method + "() index must be a number, got: " + (v == null ? "nil" : v.getClass().getSimpleName()));
    }
}
