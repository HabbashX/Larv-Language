package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A single lexical scope in the Larv runtime.
 *
 * <p>Environments form a singly-linked chain: each scope holds a reference to
 * its {@link #parent}.  Variable lookup walks this chain outward until the
 * name is found or the root scope is reached.  Assignment also walks outward
 * to find the declaring scope before mutating it — ensuring that inner scopes
 * update outer variables rather than shadowing them.</p>
 *
 * <h2>Constant protection</h2>
 * <p>Names declared via {@link #defineConst} are tracked in {@link #constants}.
 * Any attempt to {@link #assign} a constant throws immediately.</p>
 *
 * <h2>Scope lifecycle</h2>
 * <pre>
 *   Environment saved = context.getEnvironment();
 *   context.pushScope();          // creates a child scope
 *   try {
 *       // ... run block code ...
 *   } finally {
 *       context.popScope(saved);  // restores the parent scope
 *   }
 * </pre>
 */
public class Environment {

    /** Variable bindings for this scope only. */
    private final Map<String, Object> values   = new HashMap<>();

    /** Names that have been declared {@code const} in this scope. */
    private final Set<String> constants = new HashSet<>();

    /**
     * The enclosing scope, or {@code null} for the root (global) environment.
     */
    private final Environment parent;

    /** Creates a root (global) environment with no parent. */
    public Environment()                 { this.parent = null; }

    /**
     * Creates a child environment that delegates unfound names to {@code p}.
     *
     * @param p the enclosing scope
     */
    public Environment(Environment p)    { this.parent = p; }

    /**
     * Declares (or re-declares) a mutable variable in this scope.
     *
     * <p>If the name already exists in this scope it is overwritten silently
     * (this is intentional for function parameter binding).</p>
     *
     * @param name  the variable name
     * @param value the initial value ({@code null} represents {@code nil})
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Declares an immutable constant in this scope.
     *
     * <p>Any subsequent call to {@link #assign} with this name will throw.</p>
     *
     * @param name  the constant name
     * @param value the (fixed) value
     */
    public void defineConst(String name, Object value) {
        values.put(name, value);
        constants.add(name);
    }

    /**
     * Retrieves the value bound to {@code name} in this or any enclosing scope.
     *
     * @param name the variable name to look up
     * @return the current value
     * @throws LarvError if the name is not found in any scope
     */
    public Object get(String name) {
        if (values.containsKey(name)) return values.get(name);
        if (parent != null)           return parent.get(name);
        throw new LarvError("Undefined variable '" + name + "'");
    }

    /**
     * Updates the value of an existing variable in the nearest scope that
     * declares it.
     *
     * @param name  the variable name
     * @param value the new value
     * @throws LarvError if the name is a constant, or if it is not declared in any scope
     */
    public void assign(String name, Object value) {
        if (constants.contains(name))
            throw new LarvError("Cannot reassign constant '" + name + "'");
        if (values.containsKey(name)) { values.put(name, value); return; }
        if (parent != null)           { parent.assign(name, value); return; }
        throw new LarvError("Undefined variable '" + name + "' — declare it with 'var' first");
    }
}
