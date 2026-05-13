package com.habbashx.larv;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Lexer;
import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.parser.Parser;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.runtime.Interpreter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point for the Larv interpreter.
 *
 * <h2>Usage</h2>
 * <pre>
 *   larv run &lt;file.larv&gt;     — execute a Larv source file
 *   larv --version            — print the interpreter version
 *   larv --creator            — print information about the creator
 * </pre>
 *
 * <h2>Execution pipeline</h2>
 * <ol>
 *   <li>Read the source file from disk.</li>
 *   <li>Lex: {@link Lexer#tokenize()} → {@code List<Token>}</li>
 *   <li>Parse: {@link Parser#parse()} → {@code List<Statement>}</li>
 *   <li>Interpret: {@link Interpreter#execute(List)}</li>
 * </ol>
 *
 * <p>Any {@link LarvError} is caught, formatted with line/column information,
 * and printed to {@code stderr} before exiting with code {@code 1}.  Unexpected
 * Java exceptions are also caught and reported as internal errors.</p>
 */
public class Main {

    /**
     * Entry point invoked by the JVM.
     *
     * @param args command-line arguments; {@code args[0]} is the sub-command
     *             and {@code args[1]} (for {@code run}) is the file path
     */
    public static void main(String @NotNull [] args) {
        
        if (args.length < 0) return;

        switch(args[0]) {
            case "--version" -> System.out.println("Larv Version: 1.0.0-beta");
            case "--creator" -> System.out.println("""
                    Creator: Abd Allah Al Habbash
                    or you can call me Habbash
                    
                    github: Habbashx
                    instagram: abdallah_alhabbash
                    """);
            case "run" -> {
                if (args.length < 1) {
                    System.out.println("please provide file name");
                    return;
                }
                String filePath = args[1];
                runLarv(filePath);
            }
        }

    }

    /**
     * Reads, lexes, parses, and executes the given {@code .larv} file.
     *
     * <p>The file path is resolved to an absolute, normalized {@link Path}.
     * The parent directory of the file becomes the project root for resolving
     * relative imports.</p>
     *
     * @param filePath the path to the {@code .larv} file to execute
     */
    private static void runLarv(final String filePath){
        String source;

        final Path file = Path.of(filePath).toAbsolutePath().normalize();

        try {
            source = Files.readString(file);
        } catch (IOException e) {
            System.err.println("Error Cannot read file'" + filePath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        try {
            final Lexer lexer  = new Lexer(source);
            final List<Token> tokens = lexer.tokenize();

            Parser parser  = new Parser(tokens);
            List<Statement> program = parser.parse();

            Path projectRoot = file.getParent() != null ? file.getParent() : Path.of(".");
            Interpreter interpreter = new Interpreter(projectRoot);
            interpreter.execute(program);

        } catch (LarvError e) {
            System.err.println(e.format());
            System.exit(1);

        } catch (Exception e) {
            System.err.println("Internal Error " + e.getMessage());
            System.err.println("Please report this bug. Java detail: " + e.getClass().getSimpleName());
            System.exit(1);
        }
    }

}
