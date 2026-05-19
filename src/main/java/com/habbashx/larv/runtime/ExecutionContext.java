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
 * Holds all mutable runtime state shared across the interpreter pipeline.
 *
 * <p>Every component ({@link StatementExecutor}, {@link ExpressionEvaluator},
 * {@link FunctionInvoker}, standard library loaders) receives a single
 * {@code ExecutionContext} reference and reads/writes through it rather than
 * depending on each other directly.  This keeps cross-cutting concerns
 * (scope, registries) in one place and avoids circular constructor
 * dependencies.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Scope chain</b> — the active {@link Environment} and the
 *       {@link #pushScope()} / {@link #popScope(Environment)} helpers that
 *       manage the lexical scope stack.</li>
 *   <li><b>Function registry</b> — user-defined {@link FunctionStatement}
 *       nodes keyed by name, registered at declaration time.</li>
 *   <li><b>Class registry</b> — user-defined {@link ClassStatement} nodes
 *       keyed by name, registered at declaration time.</li>
 *   <li><b>Native registry</b> — Java lambdas (built-ins and stdlib functions)
 *       keyed by name, callable from Larv code.</li>
 *   <li><b>Java FFI registry</b> — the {@link JavaClassRegistry} that handles
 *       {@code include … from} bindings.</li>
 *   <li><b>Project root</b> — the filesystem path used to resolve relative
 *       file imports.</li>
 * </ul>
 */
public class ExecutionContext {

    /** The currently active lexical scope. */
    private Environment environment;

    /**
     * Filesystem path of the project root.
     * Used by {@link com.habbashx.larv.runtime.importer.LarvFileImporter} to
     * resolve dotted import paths.  Defaults to the JVM working directory.
     */
    private Path projectRoot = Path.of(System.getProperty("user.dir"));

    /** User-defined functions keyed by their declared name. */
    private final Map<String, FunctionStatement> functions = new HashMap<>();

    /** User-defined classes keyed by their declared name. */
    private final Map<String, ClassStatement> classes = new HashMap<>();

    /**
     * Native (Java-backed) functions keyed by name.
     * Populated by {@link com.habbashx.larv.runtime.registry.NativeRegistry}
     * and each stdlib library's {@code registerAll()} method.
     */
    private final Map<String, Function<List<Object>, Object>> natives = new HashMap<>();

    /** Registry for Java FFI bindings ({@code include … from "…"}). */
    private final JavaClassRegistry javaRegistry = new JavaClassRegistry();

    /** Creates a new context with a fresh global {@link Environment}. */
    public ExecutionContext() {
        this.environment = new Environment();
    }


    /** Returns the currently active environment (scope). */
    public Environment getEnvironment() { return environment; }

    /** Replaces the active environment. Use with caution — prefer {@link #pushScope}. */
    public void setEnvironment(Environment environment) { this.environment = environment; }

    /**
     * Creates a child scope and makes it the active environment.
     *
     * @return the new child scope (not the old one — callers save the return of
     *         {@link #getEnvironment()} <em>before</em> calling this if they need to restore later)
     */
    public Environment pushScope() {
        Environment child = new Environment(environment);
        environment = child;
        return child;
    }

    /**
     * Restores a previously saved environment, discarding the current scope.
     *
     * @param saved the environment to restore (typically saved before {@link #pushScope})
     */
    public void popScope(Environment saved) { environment = saved; }


    /**
     * Registers a user-defined function by name.
     *
     * @param name the function's declared name
     * @param fn   the AST node that holds its parameters and body
     */
    public void defineFunction(String name, FunctionStatement fn) { functions.put(name, fn); }

    /**
     * Looks up a user-defined function by name.
     *
     * @param name the function name
     * @return the {@link FunctionStatement}, or {@code null} if not declared
     */
    public FunctionStatement getFunction(String name) { return functions.get(name); }

    /**
     * Returns {@code true} if a user-defined function with this name exists.
     *
     * @param name the function name to check
     */
    public boolean hasFunction(String name) { return functions.containsKey(name); }

    /**
     * Registers a user-defined class by name.
     *
     * @param name the class name
     * @param cls  the AST node that holds the class body
     */
    public void defineClass(String name, ClassStatement cls) { classes.put(name, cls); }

    /**
     * Looks up a user-defined class by name.
     *
     * @param name the class name
     * @return the {@link ClassStatement}, or {@code null} if not declared
     */
    public ClassStatement getClass(String name) { return classes.get(name); }

    // ── Native function registry ──────────────────────────────────────────────

    /**
     * Registers a native (Java-backed) function callable from Larv code.
     *
     * @param name the name as it appears in Larv source
     * @param fn   a Java function that receives evaluated args and returns the result
     */
    public void registerNative(String name, Function<List<Object>, Object> fn) {
        natives.put(name, fn);
    }

    /**
     * Returns {@code true} if a native function with this name is registered.
     *
     * @param name the function name to check
     */
    public boolean hasNative(String name) { return natives.containsKey(name); }

    /**
     * Invokes a previously registered native function.
     *
     * @param name the native function name
     * @param args the evaluated argument list
     * @return the result of the native call
     */
    public Object invokeNative(String name, List<Object> args) {
        return natives.get(name).apply(args);
    }


    /** Returns the Java FFI class registry. */
    public JavaClassRegistry getJavaRegistry() { return javaRegistry; }

    /** Returns the project root path used to resolve file imports. */
    public Path getProjectRoot() { return projectRoot; }

    /**
     * Sets the project root.  Called once by the {@link Interpreter} after
     * reading the entry-point file's directory.
     *
     * @param root the new project root path
     */
    public void setProjectRoot(Path root) { this.projectRoot = root; }
}