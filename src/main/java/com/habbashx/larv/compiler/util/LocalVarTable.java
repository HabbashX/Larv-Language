package com.habbashx.larv.compiler.util;

import java.util.*;

/**
 * Tracks the JVM local variable table for a single method being compiled.
 *
 * <p>Larv is dynamically typed, so every local is of type {@code Object}
 * (slot width = 1).  Slot 0 is {@code this} for instance methods, and the
 * first user-visible slot starts at 0 for static methods.</p>
 *
 * <p>Scopes are pushed/popped as we enter/leave blocks, but JVM local slots
 * are only reclaimed when the slot pool is explicitly reset (not supported
 * here — we use a simple monotonically increasing counter for safety).
 * This is conservative but correct for small programs.</p>
 */
public class LocalVarTable {

    /** Maps variable name → JVM slot index in the current scope chain. */
    private final Deque<Map<String, Integer>> scopes = new ArrayDeque<>();

    /** Maps slot index → JVM internal type name for slots that hold a known concrete type.
     *  Absent means the slot holds {@code java/lang/Object} (unknown / erased). */
    private final Map<Integer, String> typeBySlot = new HashMap<>();

    /** Next free slot index. */
    private int nextSlot;

    /** Maximum slots used (for ClassWriter.visitMaxs). */
    private int maxSlot;

    public LocalVarTable(int startSlot) {
        this.nextSlot = startSlot;
        this.maxSlot  = startSlot;
        scopes.push(new LinkedHashMap<>());
    }

    /** Allocates a new slot for {@code name} and returns the slot index. */
    public int define(String name) {
        int slot = nextSlot++;
        if (nextSlot > maxSlot) maxSlot = nextSlot;
        scopes.peek().put(name, slot);
        return slot;
    }

    /**
     * Like {@link #define} but also records {@code jvmInternalType} (e.g.
     * {@code "UserService"}) for this slot.  Subsequent calls to
     * {@link #getType} will return that type, letting call sites skip
     * redundant {@code CHECKCAST} instructions.
     */
    public int defineTyped(String name, String jvmInternalType) {
        int slot = define(name);
        if (jvmInternalType != null && !jvmInternalType.equals("java/lang/Object")) {
            typeBySlot.put(slot, jvmInternalType);
        }
        return slot;
    }

    /**
     * Returns the JVM internal type recorded for {@code name}'s slot, or
     * {@code null} if the slot is untyped (holds {@code java/lang/Object}).
     */
    public String getType(String name) {
        int slot = get(name);
        if (slot < 0) return null;
        return typeBySlot.get(slot);
    }

    /**
     * Returns the slot for {@code name}, or -1 if not found.
     * Searches from innermost to outermost scope.
     */
    public int get(String name) {
        for (Map<String, Integer> scope : scopes) {
            Integer slot = scope.get(name);
            if (slot != null) return slot;
        }
        return -1;
    }

    /** Returns true if the variable is defined in any scope. */
    public boolean isDefined(String name) {
        return get(name) >= 0;
    }

    /** Pushes a new lexical scope (for blocks, if-bodies, loops, etc.). */
    public void pushScope() {
        scopes.push(new LinkedHashMap<>());
    }

    /** Pops the innermost lexical scope. */
    public void popScope() {
        if (scopes.size() > 1) scopes.pop();
    }

    /** Maximum slot index seen — used for ClassWriter.visitMaxs(). */
    public int maxSlot() { return maxSlot; }

    /** Current next-free-slot (for temporary allocations). */
    public int allocTemp() {
        int slot = nextSlot++;
        if (nextSlot > maxSlot) maxSlot = nextSlot;
        return slot;
    }

    @Deprecated
    public void freeTemp() {}
}