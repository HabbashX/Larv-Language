import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Lexer;
import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.parser.Parser;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.runtime.Interpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestingMain {

    private static final String RED   = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private static void printError(String message) {
        System.err.println(RED + message + RESET);
    }

    public static void main(String[] args) {

        runLarv("Testing.larv");
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
            printError("Error: cannot read file '" + filePath + "': " + e.getMessage());
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
            printError(e.format());
            System.exit(1);

        } catch (Exception e) {
            printError("Internal Error: " + e.getMessage());
            printError("Please report this bug. Java detail: " + e.getClass().getSimpleName());
            System.exit(1);
        }
    }
}