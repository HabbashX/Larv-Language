package com.habbashx.larv.runtime.importer;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Lexer;
import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.parser.Parser;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.runtime.ExecutionContext;
import com.habbashx.larv.runtime.stdlib.loader.NativeLibraryLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a dotted import path to a .larv file, parses it,
 * and registers its classes and functions into the current ExecutionContext.
 *
 * Resolution:
 *   "com.habbashx.Testing" → <root>/com/habbashx/Testing.larv
 *
 * Root is resolved relative to the directory of the file being executed.
 * Falls back to the current working directory if no root is set.
 *
 * Circular imports are silently skipped — if A imports B and B imports A,
 * the second load of A is a no-op.
 *
 * Only top-level class and function declarations are imported.
 * Top-level executable statements in the imported file do NOT run.
 */
public class LarvFileImporter {

    /** Tracks already-imported paths across the entire runtime to prevent re-importing. */
    private static final Set<String> IMPORTED = new HashSet<>();

    private final ExecutionContext context;
    private final Path             root;

    public LarvFileImporter(@NotNull ExecutionContext context, @NotNull Path root) {
        this.context = context;
        this.root    = root;
    }

    /**
     * Import a file by its dotted path string.
     *
     * @param dottedPath e.g. "com.habbashx.Testing"
     */
    public void importFile(@NotNull String dottedPath) {
        Path filePath = resolve(dottedPath);
        String canonical = filePath.toAbsolutePath().normalize().toString();

        if (IMPORTED.contains(canonical)) return;
        IMPORTED.add(canonical);

        // Read source
        String source;
        try {
            source = Files.readString(filePath);
        } catch (IOException e) {
            throw new LarvError(
                    "import: cannot read file '" + filePath + "' (from \"" + dottedPath + "\"): " + e.getMessage(),
                    -1, LarvError.Kind.RUNTIME
            );
        }

        // Lex + parse
        List<Token>     tokens  = new Lexer(source).tokenize();
        List<Statement> program = new Parser(tokens).parse();

        // Register only class and function declarations — skip executable statements
        for (Statement stmt : program) {
            if (stmt instanceof ClassStatement cls) {
                context.defineClass(cls.name(), cls);
            } else if (stmt instanceof FunctionStatement fn) {
                context.defineFunction(fn.name(), fn);
            } else if (stmt instanceof ImportStatement nested) {
            if (nested.isFileImport()) {
                Path nestedRoot = filePath.getParent() != null ? filePath.getParent() : root;
                new LarvFileImporter(context, nestedRoot).importFile(nested.path());
            } else {
                new NativeLibraryLoader(context).load(nested.library());
            }
        }
        }
    }

    /**
     * Convert "com.habbashx.Testing" → root/com/habbashx/Testing.larv
     */
    private @NotNull Path resolve(@NotNull String dottedPath) {
        String relativePath = dottedPath.replace('.', '/') + ".larv";
        Path resolved = root.resolve(relativePath).normalize();

        if (!Files.exists(resolved)) {
            throw new LarvError(
                    "import: file not found: '" + resolved + "' (from \"" + dottedPath + "\")\n" +
                            "  Tip: make sure the file exists relative to your project root.",
                    -1, LarvError.Kind.RUNTIME
            );
        }

        return resolved;
    }

    /**
     * Reset imported file tracking — call this between interpreter runs
     * so that re-running a program imports files fresh.
     */
    public static void reset() {
        IMPORTED.clear();
    }
}