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

public class Main {

    private static final String RED   = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private static void printError(String message) {
        System.err.println(RED + message + RESET);
    }

    public static void main(String @NotNull [] args) {

        try {

            if (args.length == 0) {
                System.out.println("Larv Language");
                System.out.println("Usage:");
                System.out.println("  larv run <file.larv>");
                System.out.println("  larv --version");
                System.out.println("  larv --creator");
                return;
            }

            switch (args[0]) {

                case "--version" ->
                        System.out.println("Larv Version: 1.0.2");

                case "--creator" ->
                        System.out.println("""
                                Creator: Abd Allah Al Habbash
                                or you can call me Habbash
                                
                                github: Habbashxl
                                """);

                case "run" -> {

                    if (args.length < 2) {
                        printError("please provide file name");
                        System.exit(1);
                        return;
                    }

                    String filePath = args[1];
                    runLarv(filePath);
                }

                default -> {
                    printError("Unknown command: " + args[0]);

                    System.out.println();
                    System.out.println("Available commands:");
                    System.out.println("  larv run <file.larv>");
                    System.out.println("  larv --version");
                    System.out.println("  larv --creator");

                    System.exit(1);
                }
            }

        } catch (Exception e) {

            printError("Fatal Internal Error: " + e.getMessage());
            e.printStackTrace();

            System.exit(1);
        }
    }

    private static void runLarv(final String filePath) {

        String source;

        final Path file = Path.of(filePath)
                .toAbsolutePath()
                .normalize();

        try {

            source = Files.readString(file);

        } catch (IOException e) {

            printError("Error: cannot read file '" + filePath + "'");
            printError(e.getMessage());

            System.exit(1);
            return;
        }

        try {

            final Lexer lexer = new Lexer(source);
            final List<Token> tokens = lexer.tokenize();

            final Parser parser = new Parser(tokens);
            final List<Statement> program = parser.parse();

            final Path projectRoot =
                    file.getParent() != null
                            ? file.getParent()
                            : Path.of(".");

            final Interpreter interpreter =
                    new Interpreter(projectRoot);

            interpreter.execute(program);

        } catch (LarvError e) {

            printError(e.format());
            System.exit(1);

        } catch (Exception e) {

            printError("Internal Error: " + e.getMessage());
            printError("Java Exception: " + e.getClass().getSimpleName());

            e.printStackTrace();

            System.exit(1);
        }
    }
}