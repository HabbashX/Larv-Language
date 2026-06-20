package com.habbashx.larv.compiler.runtime;

import com.habbashx.larv.error.LarvError;
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

    private final String className;


    public LarvObject(String className) {
        this.className = className;
    }
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
        if (fields.containsKey(name)) return fields.get(name);
        return methods.get(name);
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
     * Returns a read-only view of all fields on this object.
     * Used by the method invoker to inject fields into the method scope.
     */
    public Map<String, Object> getFields() {
        return java.util.Collections.unmodifiableMap(fields);
    }

    public Object getOrThrow(String name) {
        Object value = get(name);
        if (value == null && !fields.containsKey(name))
            throw new LarvError("Undefined field '" + name + "' on object");
        return value;
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "<" + className + " " + fields + ">";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
