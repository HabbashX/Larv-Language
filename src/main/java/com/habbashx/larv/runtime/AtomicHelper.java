package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central helper for Larv {@code atomic<...>} variables.
 *
 * <p>Atomic variables are stored as raw {@code java.util.concurrent.atomic}
 * instances ({@link AtomicInteger}, {@link AtomicLong}, {@link AtomicBoolean},
 * {@link AtomicReference}).  This helper implements the two halves of the
 * transparent-atomic contract:</p>
 * <ul>
 *   <li><b>Read</b> ({@link #unwrap}) — converts the atomic holder back to a
 *       plain Larv value ({@link Double}, {@link Boolean}, or the referenced
 *       object).  Non-atomic values pass through unchanged, so the helper is
 *       safe to call unconditionally.</li>
 *   <li><b>Write</b> ({@link #setAtomic}) — stores a new Larv value into an
 *       existing holder <em>in place</em> (via {@code set(...)}) so the
 *       reference identity — and therefore thread-safety — is preserved.
 *       Assigning must never replace the holder object itself.</li>
 * </ul>
 */
public final class AtomicHelper {

    private AtomicHelper() {}

    /**
     * Returns {@code true} if {@code v} is one of the four supported atomic
     * holder types.
     */
    @Contract(pure = true)
    public static boolean isAtomic(@Nullable Object v) {
        return v instanceof AtomicInteger
                || v instanceof AtomicLong
                || v instanceof AtomicBoolean
                || v instanceof AtomicReference;
    }

    /**
     * Converts an atomic holder to its plain Larv value.
     *
     * <ul>
     *   <li>{@link AtomicInteger} / {@link AtomicLong} → {@link Double}</li>
     *   <li>{@link AtomicBoolean} → {@link Boolean}</li>
     *   <li>{@link AtomicReference} → the referenced value (may be {@code null}/nil)</li>
     *   <li>anything else → returned unchanged</li>
     * </ul>
     */
    @Contract(pure = true)
    public static Object unwrap(@Nullable Object v) {
        if (v instanceof AtomicInteger ai)   return (double) ai.get();
        if (v instanceof AtomicLong al)      return (double) al.get();
        if (v instanceof AtomicBoolean ab)   return ab.get();
        if (v instanceof AtomicReference<?> ref) return ref.get();
        return v;
    }

    /**
     * Stores {@code value} into an existing atomic holder in place.
     *
     * @param atomic must be an atomic holder (checked with {@link #isAtomic})
     * @param value  the new Larv value
     * @throws LarvError if a numeric holder receives a non-numeric string
     */
    public static void setAtomic(@NotNull Object atomic, @Nullable Object value) {
        if (atomic instanceof AtomicInteger ai) {
            ai.set(toInt(value, "atomic<int>"));
            return;
        }
        if (atomic instanceof AtomicLong al) {
            al.set(toLong(value, "atomic<long>"));
            return;
        }
        if (atomic instanceof AtomicBoolean ab) {
            ab.set(TruthinessEvaluator.isTruthy(value));
            return;
        }
        if (atomic instanceof AtomicReference<?> ref) {
            @SuppressWarnings("unchecked")
            AtomicReference<Object> aref = (AtomicReference<Object>) ref;
            aref.set(value);
            return;
        }
        throw new LarvError("setAtomic() requires an atomic holder, got: "
                + (atomic == null ? "nil" : atomic.getClass().getSimpleName()));
    }

    private static int toInt(@Nullable Object v, String what) {
        if (v == null)             return 0;
        if (v instanceof Double d) return d.intValue();
        if (v instanceof Integer i) return i;
        if (v instanceof Long l)    return l.intValue();
        if (v instanceof Boolean b) return b ? 1 : 0;
        if (v instanceof String s) {
            try { return (int) Double.parseDouble(s); }
            catch (NumberFormatException e) {
                throw new LarvError(what + " requires a number, got: '" + s + "'");
            }
        }
        throw new LarvError(what + " requires a number, got: " + v.getClass().getSimpleName());
    }

    private static long toLong(@Nullable Object v, String what) {
        if (v == null)             return 0L;
        if (v instanceof Double d) return d.longValue();
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l)    return l;
        if (v instanceof Boolean b) return b ? 1L : 0L;
        if (v instanceof String s) {
            try { return (long) Double.parseDouble(s); }
            catch (NumberFormatException e) {
                throw new LarvError(what + " requires a number, got: '" + s + "'");
            }
        }
        throw new LarvError(what + " requires a number, got: " + v.getClass().getSimpleName());
    }
}
