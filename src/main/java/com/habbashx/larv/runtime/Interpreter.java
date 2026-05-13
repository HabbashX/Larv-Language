package com.habbashx.larv.runtime;

import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.runtime.importer.LarvFileImporter;
import com.habbashx.larv.runtime.registry.NativeRegistry;

import java.nio.file.Path;
import java.util.List;

/**
 * The top-level entry point for executing a parsed Larv program.
 *
 * <p>{@code Interpreter} wires all runtime components together and exposes a
 * single {@link #execute(List)} method.  It is the only class that callers
 * outside the runtime package need to interact with directly.</p>
 *
 * <h2>Component wiring</h2>
 * <ol>
 *   <li>Creates a shared {@link ExecutionContext}.</li>
 *   <li>Registers built-in native functions via {@link NativeRegistry}.</li>
 *   <li>Creates {@link FunctionInvoker} and {@link ExpressionEvaluator}.</li>
 *   <li>Creates {@link StatementExecutor} and injects its
 *       {@code runBlock} method into the invoker (resolving the circular dep).</li>
 * </ol>
 *
 * <h2>File import support</h2>
 * <p>The {@code projectRoot} path tells the importer where to resolve
 * {@code import "com.example.File"} paths.  Pass the directory that contains
 * the entry-point {@code .larv} file.</p>
 */
public class Interpreter {

    private final StatementExecutor executor;
    private final ExecutionContext  context;

    /**
     * Creates an interpreter that resolves file imports relative to
     * {@code projectRoot}.
     *
     * @param projectRoot the directory from which file imports are resolved;
     *                    typically the directory of the {@code .larv} file being executed
     */
    public Interpreter(Path projectRoot) {
        context = new ExecutionContext();
        context.setProjectRoot(projectRoot);

        new NativeRegistry(context).registerAll();

        FunctionInvoker invoker = new FunctionInvoker(context);

        ExpressionEvaluator evaluator = new ExpressionEvaluator(context, invoker);

        executor = new StatementExecutor(context, evaluator);

        invoker.setBlockRunner(executor::runBlock);
    }

    /**
     * Creates an interpreter that resolves imports relative to the current
     * working directory.
     */
    public Interpreter() {
        this(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Executes a fully parsed list of top-level statements.
     *
     * <p>Resets the import tracker before running so that re-executing
     * the same interpreter instance processes imports freshly.</p>
     *
     * @param statements the AST produced by {@link com.habbashx.larv.parser.Parser#parse()}
     */
    public void execute(List<Statement> statements) {
        LarvFileImporter.reset();
        executor.execute(statements);
    }
}
