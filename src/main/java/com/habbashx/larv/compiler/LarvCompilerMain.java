package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.classloader.LarvClassLoader;
import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.error.ErrorReporter;
import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Lexer;
import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.parser.Parser;
import com.habbashx.larv.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command-line entry point for the Larv → JVM bytecode compiler.
 *
 * <h2>Usage</h2>
 * <pre>
 *   larv compile &lt;source.larv&gt; [--out &lt;dir&gt;] [--class &lt;ClassName&gt;] [--dump] [--run]
 * </pre>
 *
 * <h3>Options</h3>
 * <ul>
 *   <li>{@code --out <dir>}       — output directory for {@code .class} files (default: {@code ./out})</li>
 *   <li>{@code --class <name>}    — name of the generated main class (default: stem of source file)</li>
 *   <li>{@code --dump}            — print ASM textifier output to stdout for debugging</li>
 *   <li>{@code --run}             — compile and immediately run via a custom classloader</li>
 *   <li>{@code --debug}           — enable debug mode: log every compiled statement to stderr
 *       (type + line number), emit JVM line-number table entries, and trace function entry/exit</li>
 * </ul>
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Lex source → {@link Token} list</li>
 *   <li>Parse tokens → {@link Statement} list</li>
 *   <li>Compile AST → {@link CompiledClass} list (bytecode)</li>
 *   <li>Write {@code .class} files to output directory</li>
 *   <li>Optionally run via {@link LarvClassLoader}</li>
 * </ol>
 */
public class LarvCompilerMain {

    public static void main(String @NotNull [] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        String[] capturedSource = {null};
        String[] capturedFile   = {null};
        // Pre-scan args for --debug so the top-level catch can use it
        boolean debugFlag = false;
        for (String a : args) if ("--debug".equals(a)) { debugFlag = true; break; }
        try {
            runCompiler(args, capturedSource, capturedFile);
        } catch (Throwable t) {
            if (debugFlag) {
                System.err.println("[DEBUG] FATAL exception caught at top level:");
                t.printStackTrace(System.err);
                System.err.println();
            }
            ErrorReporter.report(t, capturedSource[0], capturedFile[0]);
            System.exit(1);
        }
    }

    private static void runCompiler(String @NotNull [] args,
                                    String[] capturedSource,
                                    String[] capturedFile) throws Exception {
        String sourceFile = null;
        String outputDir  = "out";
        String className  = null;
        boolean dump      = false;
        boolean run       = false;
        boolean debug     = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--out"   -> outputDir = args[++i];
                case "--class" -> className  = args[++i];
                case "--dump"  -> dump       = true;
                case "--run"   -> run        = true;
                case "--debug" -> debug      = true;
                default -> {
                    if (!args[i].startsWith("--")) sourceFile = args[i];
                    else { throw new LarvError("unknown flag: '" + args[i] + "'", -1, LarvError.Kind.PARSE).withHint("run without arguments to see usage"); }
                }
            }
        }

        if (sourceFile == null) {
            throw new LarvError("no source file specified", -1, LarvError.Kind.PARSE).withHint("usage: larv compile <source.larv> [options]");
        }

        Path sourcePath = Path.of(sourceFile);
        if (className == null) {
            String stem = sourcePath.getFileName().toString();
            int dot = stem.lastIndexOf('.');
            className = dot >= 0 ? stem.substring(0, dot) : stem;
            className = Character.toUpperCase(className.charAt(0)) + className.substring(1);
        }

        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        capturedSource[0] = source;
        capturedFile[0]   = sourceFile;
        System.out.println("Compiling: " + sourceFile + " → " + className);

        Lexer lexer  = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);
        List<Statement> ast = parser.parse();
        Set<String> alreadyImported = new HashSet<>();
        alreadyImported.add(sourcePath.toAbsolutePath().normalize().toString());
        ast = resolveFileImports(ast, sourcePath.getParent(), alreadyImported);

        System.out.println("Parsed " + ast.size() + " top-level statement(s).");

        LarvCompiler compiler = new LarvCompiler(className);
        compiler.debugMode = debug;
        List<CompiledClass> classes = compiler.compile(ast);
        System.out.println("Generated " + classes.size() + " class(es).");

        Path outPath = Path.of(outputDir);
        Files.createDirectories(outPath);

        for (CompiledClass cc : classes) {
            String fileName = cc.name() + ".class";
            Path classFile  = outPath.resolve(fileName);
            Files.write(classFile, cc.bytecode());
            System.out.println("  Wrote: " + classFile);

            if (dump) {
                System.out.println("\n── Bytecode dump: " + cc.name() + " ──");
                dumpBytecode(cc.bytecode());
            }
        }

        if (run) {
            System.out.println("\n── Running " + className + ".main() ──\n");
            LarvClassLoader loader = new LarvClassLoader(classes, outPath);
            Class<?> mainClass = loader.loadClass(className);
            mainClass.getMethod("main", String[].class)
                    .invoke(null, (Object) new String[0]);
        }
    }

    /**
     * Walks {@code statements} and replaces every file-import node
     * ({@code import "SomeFile"}) with the parsed statements of that file,
     * recursively.  Stdlib imports are left untouched.
     *
     * <p>Circular imports are detected via {@code alreadyImported} (a set of
     * canonical absolute paths).  A file imported more than once is silently
     * skipped on subsequent encounters, matching the behaviour of most
     * include-style systems.</p>
     *
     * @param statements     the AST to process
     * @param baseDir        directory to resolve relative import paths against
     * @param alreadyImported canonical paths already on the include stack
     * @return a new statement list with all file imports inlined
     */
    private static @NotNull List<Statement> resolveFileImports(
            @NotNull List<Statement> statements,
            Path baseDir,
            Set<String> alreadyImported) throws Exception {

        List<Statement> result = new ArrayList<>();
        for (Statement s : statements) {
            if (s instanceof com.habbashx.larv.parser.ast.statement.ImportStatement is
                    && is.isFileImport()) {

                String importPath = is.path();
                Path candidate = baseDir != null
                        ? baseDir.resolve(importPath + ".larv")
                        : Path.of(importPath + ".larv");
                if (!Files.exists(candidate)) {
                    candidate = baseDir != null
                            ? baseDir.resolve(importPath)
                            : Path.of(importPath);
                }
                if (!Files.exists(candidate)) {
                    throw new CompileException(
                            "File import not found: '" + importPath + "' " +
                                    "(tried '" + importPath + ".larv' and '" + importPath + "')", is.line());
                }

                String canonical = candidate.toAbsolutePath().normalize().toString();
                if (alreadyImported.contains(canonical)) {
                    System.out.println("  [import] Skipping already-imported: " + canonical);
                    continue;
                }
                alreadyImported.add(canonical);
                System.out.println("  [import] Inlining: " + canonical);

                String importedSource = Files.readString(candidate, StandardCharsets.UTF_8);
                Lexer importedLexer   = new Lexer(importedSource);
                Parser importedParser = new Parser(importedLexer.tokenize());
                List<Statement> importedAst = importedParser.parse();
                importedAst = resolveFileImports(
                        importedAst, candidate.getParent(), alreadyImported);

                result.addAll(importedAst);
            } else {
                result.add(s);
            }
        }
        return result;
    }

    private static void printUsage() {
        System.out.println("""
            Larv → JVM Bytecode Compiler
            Usage:
              larv compile <source.larv> [options]

            Options:
              --out <dir>        Output directory for .class files (default: ./out)
              --class <name>     Main class name (default: filename stem, capitalised)
              --dump             Print ASM bytecode disassembly to stdout
              --run              Compile and immediately execute the program
              --debug            Enable debug mode: log every statement compiled to
                                 stderr (type + line), emit JVM line-number entries so
                                 stack traces point at Larv source lines, and announce
                                 function entry/exit
            """);
    }

    private static void dumpBytecode(byte[] bytecode) {
        try {
            org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytecode);
            org.objectweb.asm.util.TraceClassVisitor tcv =
                    new org.objectweb.asm.util.TraceClassVisitor(new PrintWriter(System.out));
            cr.accept(tcv, 0);
        } catch (Exception e) {
            System.err.println("Dump failed: " + e.getMessage());
        }
    }
}