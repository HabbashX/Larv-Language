package com.habbashx.larv.runtime;

import com.habbashx.larv.parser.ast.statement.ClassStatement;
import com.habbashx.larv.parser.ast.statement.FunctionStatement;
import com.habbashx.larv.runtime.ffi.JavaClassRegistry;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Holds all mutable runtime state: the current scope, registered functions,
 * classes, and native built-ins.
 *
 * Passed by reference into every component so they all share the same live
 * view of the program's state without coupling to each other.
 */
public class ExecutionContext {

    private Environment environment;
    private Path projectRoot = Path.of(System.getProperty("user.dir"));
    private final Map<String, FunctionStatement> functions = new HashMap<>();
    private final Map<String, ClassStatement> classes = new HashMap<>();
    private final Map<String, Function<List<Object>, Object>> natives = new HashMap<>();
    private final JavaClassRegistry javaRegistry = new JavaClassRegistry();

    public ExecutionContext() {
        this.environment = new Environment();
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Environment pushScope() {
        Environment child = new Environment(environment);
        environment = child;
        return child;
    }

    public void popScope(Environment saved) {
        environment = saved;
    }

    public void defineFunction(String name, FunctionStatement fn) {
        functions.put(name, fn);
    }

    public FunctionStatement getFunction(String name) {
        return functions.get(name);
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public void defineClass(String name, ClassStatement cls) {
        classes.put(name, cls);
    }

    public ClassStatement getClass(String name) {
        return classes.get(name);
    }

    public void registerNative(String name, Function<List<Object>, Object> fn) {
        natives.put(name, fn);
    }

    public boolean hasNative(String name) {
        return natives.containsKey(name);
    }

    public Object invokeNative(String name, List<Object> args) {
        return natives.get(name).apply(args);
    }

    public JavaClassRegistry getJavaRegistry() {
        return javaRegistry;
    }

    public Path getProjectRoot() { return projectRoot; }
    public void setProjectRoot(Path root) { this.projectRoot = root; }
}