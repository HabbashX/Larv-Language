package com.habbashx.larv.runtime;

import com.habbashx.larv.error.ErrorReporter;
import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Lexer;
import com.habbashx.larv.parser.Parser;
import com.habbashx.larv.parser.ast.statement.Statement;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for running Larv source files through the tree-walking
 * interpreter (no JVM bytecode compilation).
 *
 * <h2>Usage</h2>
 * <pre>
 *   larv run &lt;source.larv&gt;
 * </pre>
 *
 * <p>All errors — lexer, parser, runtime, FFI — are routed through
 * {@link ErrorReporter} to produce rich, coloured diagnostics with source
 * snippets and actionable hints instead of raw Java stack traces.</p>
 */
@Deprecated(since = "1.1.0" , forRemoval = true)
public class LarvRunner {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String CYAN  = "\u001B[36m";
    private static final String DIM   = "\u001B[2m";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
            printUsage();
            System.exit(0);
        }

        String filePath = args[0];
        Path   sourcePath;
        String source;

        try {
            sourcePath = Path.of(filePath);
            source     = Files.readString(sourcePath, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            LarvError err = new LarvError(
                    "source file not found: '" + filePath + "'",
                    -1, -1, LarvError.Kind.RUNTIME)
                    .withHint("check the path and make sure the file exists");
            System.err.print(err.format());
            System.exit(1);
            return;
        } catch (Exception e) {
            System.err.println("Could not read '" + filePath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        String fileName = Path.of(filePath).getFileName().toString();

        try {
            Lexer       lexer  = new Lexer(source);
            var         tokens = lexer.tokenize();

            Parser      parser = new Parser(tokens);
            List<Statement> ast = parser.parse();

            Interpreter interp = new Interpreter(sourcePath.toAbsolutePath().getParent());
            interp.execute(ast);

        } catch (Throwable t) {
            if (t instanceof LarvError le && le.getLine() >= 0) {
                le.withSource(source).withFile(fileName);
            }
            ErrorReporter.report(t, source, fileName);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("""
            
            %sLarv Interpreter%s
            %sRuns a .larv source file through the tree-walking interpreter.%s
            
            %sUsage:%s
              larv run <source.larv>
            
            %sOptions:%s
              -h, --help    Show this help message
            """.formatted(BOLD + CYAN, RESET, DIM, RESET, BOLD, RESET, BOLD, RESET));
    }
}