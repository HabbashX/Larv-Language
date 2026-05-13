package com.habbashx.larv.runtime;

import com.habbashx.larv.runtime.call.LarvCallable;

import java.util.HashMap;
import java.util.Map;

/**
 * A runtime instance of a user-defined Larv class.
 *
 * <p>A {@code LarvObject} is essentially a bag of named fields and methods.
 * It is created by evaluating a {@code new ClassName()} expression, which
 * copies the class body's method declarations onto the object and then
 * invokes the {@code init} method if present.</p>
 *
 * <h2>Fields vs Methods</h2>
 * <p>Fields are ordinary name-to-value bindings stored in {@link #fields}.
 * Methods are {@link LarvCallable} instances stored in {@link #methods}.
 * {@link #get(String)} checks fields first, then methods, so a field and a
 * method with the same name will shadow the method.</p>
 *
 * <h2>Internal fields</h2>
 * <p>The evaluator stores the method map under the reserved key
 * {@code "__methods__"} so that method dispatch can retrieve all methods
 * from a live object without a separate class reference.</p>
 */
public class LarvObject {

    /** Named field values, including the internal {@code "__methods__"} map. */
    private final Map<String, Object> fields = new HashMap<>();

    /**
     * Named callable methods registered on this object.
     *
     * @deprecated Methods are now stored in {@link #fields} under
     *             {@code "__methods__"}; this map is kept for backward
     *             compatibility but may not be fully populated.
     */
    private final Map<String, LarvCallable> methods = new HashMap<>();

    /**
     * Returns the field or method bound to {@code name}.
     *
     * <p>Fields take priority over methods.  If neither is found, a
     * {@link RuntimeException} is thrown (use this only after verifying
     * the field exists).</p>
     *
     * @param name the field or method name
     * @return the stored value or callable
     * @throws RuntimeException if no field or method with the given name exists
     */
    public Object get(String name) {

        if (fields.containsKey(name)) {
            return fields.get(name);
        }

        LarvCallable method = methods.get(name);
        if (method != null) return method;

        throw new RuntimeException("Undefined field: " + name);
    }

    /**
     * Stores or updates a field value.
     *
     * @param name  the field name
     * @param value the new value ({@code null} represents {@code nil})
     */
    public void set(String name, Object value) {
        fields.put(name, value);
    }

    /**
     * Registers a method on this object instance.
     *
     * @param name the method name
     * @param fn   the callable implementation
     */
    public void defineMethod(String name, LarvCallable fn) {
        methods.put(name, fn);
    }
}
