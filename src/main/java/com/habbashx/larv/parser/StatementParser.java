package com.habbashx.larv.parser;

import com.habbashx.larv.lexer.TokenType;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.rule.StatementRule;
import com.habbashx.larv.parser.stream.TokenStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses every statement in the Larv grammar using a
 * <b>Strategy + Command Registry</b> pattern.
 *
 * <p>Instead of an {@code if/else-if} chain that grows with every new keyword,
 * {@link #parse()} does a single {@link Map} lookup keyed on the current
 * {@link TokenType}. Each entry is a {@link StatementRule} — a method reference
 * that owns one keyword's full parse logic.</p>
 *
 * <p>Adding a new keyword requires exactly two steps:
 * <ol>
 *   <li>Write a private {@code parseXxx()} method.</li>
 *   <li>Register it in {@link #buildRegistry()}.</li>
 * </ol>
 * {@link #parse()} itself never needs to change.</p>
 *
 * <pre>
 * Grammar (statements)
 * ─────────────────────
 *   statement → letDecl | constDecl | printStmt | ifStmt | whileStmt
 *             | forStmt | breakStmt | continueStmt | funcDecl
 *             | returnStmt | classDecl | exprStmt
 * </pre>
 */
public class StatementParser {

    private final TokenStream stream;
    private final ExpressionParser exprParser;
    private final ArgumentParser argParser;

    /** Maps each keyword token type to its dedicated parse strategy. */
    private final Map<TokenType, StatementRule> registry;

    public StatementParser(TokenStream stream, ExpressionParser exprParser, ArgumentParser argParser) {
        this.stream     = stream;
        this.exprParser = exprParser;
        this.argParser  = argParser;
        this.registry   = buildRegistry();
    }


    /**
     * Builds the keyword → rule map once at construction time.
     * {@link EnumMap} gives O(1) lookup with zero boxing overhead.
     */
    private @NotNull Map<TokenType, StatementRule> buildRegistry() {
        Map<TokenType, StatementRule> map = new EnumMap<>(TokenType.class);

        map.put(TokenType.VAR,      this::parseVar);
        map.put(TokenType.CONST,    this::parseConst);
        map.put(TokenType.PRINT,    this::parsePrint);
        map.put(TokenType.IF,       this::parseIf);
        map.put(TokenType.WHILE,    this::parseWhile);
        map.put(TokenType.FOR,      this::parseFor);
        map.put(TokenType.BREAK,    () -> new BreakStatement(-1));
        map.put(TokenType.CONTINUE, () -> new ContinueStatement(-1));
        map.put(TokenType.FUNC,     this::parseFunctionDecl);
        map.put(TokenType.RETURN,   this::parseReturn);
        map.put(TokenType.CLASS,    this::parseClassDecl);
        map.put(TokenType.INCLUDE, this::parseJavaBind);
        map.put(TokenType.IMPORT,  this::parseImport);

        return map;
    }

    /**
     * Looks up the current token type in the registry and fires the matching rule.
     * Falls back to an expression-statement when no keyword matches.
     * No {@code if/else} chain — one map lookup.
     */
    public Statement parse() {
        int line = stream.peek().line(); // capture before consuming
        StatementRule rule = registry.get(stream.peek().tokenType());

        if (rule != null) {
            stream.advance(); // consume the matched keyword token
            Statement st = rule.parse();
            return withLine(st, line);
        }

        return withLine(parseExprStatement(), line);
    }

    private @NotNull Statement parseVar() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected variable name after 'var'");
        Expression value = stream.match(TokenType.EQUAL) ? exprParser.parse() : null;
        return new LetStatement(name, value, -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseConst() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected constant name after 'const'");
        stream.consume(TokenType.EQUAL, "Expected '=' after constant name");
        return new ConstStatement(name, exprParser.parse(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parsePrint() {
        return new PrintStatement(exprParser.parse(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseIf() {
        Expression condition   = exprParser.parse();
        stream.consume(TokenType.LBRACE, "Expected '{' to open if-body");
        List<Statement> thenBranch = parseBlock();
        List<Statement> elseBranch = parseElseBranch();
        return new IfStatement(condition, thenBranch, elseBranch, -1);
    }

    private List<Statement> parseElseBranch() {
        if (!stream.match(TokenType.ELSE)) return new ArrayList<>();
        stream.consume(TokenType.LBRACE, "Expected '{' to open else-body");
        return parseBlock();
    }

    private Statement parseWhile() {
        Expression condition = exprParser.parse();
        stream.consume(TokenType.LBRACE, "Expected '{' to open while-body");
        return new WhileStatement(condition, parseBlock(), -1);
    }

    private Statement parseFor() {
        Statement  init      = parseForInit();
        stream.consume(TokenType.SEMICOLON, "Expected ';' after for-init");
        Expression condition = exprParser.parse();
        stream.consume(TokenType.SEMICOLON, "Expected ';' after for-condition");
        Statement  increment = parseForIncrement();
        stream.consume(TokenType.LBRACE, "Expected '{' to open for-body");
        return new ForStatement(init, condition, increment, parseBlock(), -1);
    }

    private Statement parseForInit() {
        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.EQUAL)) {
            String name = stream.advance().value();
            stream.consume(TokenType.EQUAL, "Expected '='");
            return new LetStatement(name, exprParser.parse(), -1);
        }
        return parseExprStatement();
    }

    private @NotNull Statement parseForIncrement() {
        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.PLUS_PLUS)) {
            String name = stream.advance().value();
            stream.consume(TokenType.PLUS_PLUS, "Expected '++'");
            return new IncrementStatement(name, -1);
        }
        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.MINUS_MINUS)) {
            String name = stream.advance().value();
            stream.consume(TokenType.MINUS_MINUS, "Expected '--'");
            return new DecrementStatement(name, -1);
        }
        return parseExprStatement();
    }

    @Contract(" -> new")
    private @NotNull Statement parseFunctionDecl() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected function name after 'func'");
        stream.consume(TokenType.LPAREN, "Expected '(' after function name");
        List<String> params = argParser.parseParameters();
        stream.consume(TokenType.LBRACE, "Expected '{' to open function body");
        return new FunctionStatement(name, params, parseBlock(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseReturn() {
        return new ReturnStatement(exprParser.parse(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseClassDecl() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected class name after 'class'");
        stream.consume(TokenType.LBRACE, "Expected '{' to open class body");

        List<Statement> body = new ArrayList<>();
        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
            body.add(parse());
        }

        stream.consume(TokenType.RBRACE, "Expected '}' to close class body");
        return new ClassStatement(name, body, -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseExprStatement() {
        return new ExprStatement(exprParser.parse(), -1);
    }

    /**
     * Parses two forms:
     *   import math                    — stdlib library (identifier)
     *   import "com.habbashx.Testing"  — Larv file import (quoted string)
     *
     * The IMPORT token has already been consumed by the registry dispatcher.
     */
    @Contract(" -> new")
    private @NotNull Statement parseImport() {
        if (stream.check(TokenType.STRING)) {
            // File import — import "com.habbashx.Testing"
            String path = stream.consumeValue(TokenType.STRING, "Expected file path after 'import'");
            stream.match(TokenType.SEMICOLON);
            return ImportStatement.ofPath(path);
        }
        // Stdlib import — import math
        String library = stream.consumeValue(TokenType.IDENTIFIER, "Expected library name after 'import'");
        stream.match(TokenType.SEMICOLON);
        return ImportStatement.ofLibrary(library);
    }

    /**
     * Parses:
     *   include Alias from "com.example.ClassName"
     *   include Alias from "com.example.ClassName" involve { "arg1", "arg2" }
     *
     * The INCLUDE token has already been consumed by the registry dispatcher.
     */
    @Contract(" -> new")
    private @NotNull Statement parseJavaBind() {
        String alias     = stream.consumeValue(TokenType.IDENTIFIER, "Expected alias after 'include'");
        stream.consume(TokenType.FROM, "Expected 'from' after alias in java binding");
        String className = stream.consumeValue(TokenType.STRING, "Expected fully-qualified class name string after 'from'");

        // Optional:  involve { "arg1", "arg2", ... }
        boolean hasInvolve = false;
        List<String> constructorArgs = new ArrayList<>();
        if (stream.match(TokenType.INVOLVE)) {
            hasInvolve = true;
            stream.consume(TokenType.LBRACE, "Expected '{' after 'involve'");
            while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
                String arg = stream.consumeValue(TokenType.STRING,
                        "Expected string argument inside 'involve { ... }'");
                constructorArgs.add(arg);
                stream.match(TokenType.COMMA); // optional trailing comma
            }
            stream.consume(TokenType.RBRACE, "Expected '}' to close 'involve' argument list");
        }

        // Optional trailing semicolon
        stream.match(TokenType.SEMICOLON);

        return new JavaBindStatement(alias, className, constructorArgs, hasInvolve, -1);
    }

    /**
     * Parses statements until a closing {@code }} is found.
     * The opening {@code {} must already have been consumed before calling this.
     */
    public List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();
        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
            statements.add(parse());
        }
        stream.consume(TokenType.RBRACE, "Expected '}' to close block");
        return statements;
    }

    /**
     * Returns a copy of {@code st} with its {@code line} field set.
     * Each record constructor's last parameter is {@code int line}.
     */
    private static Statement withLine(Statement st, int line) {
        return switch (st) {
            case LetStatement        s -> new LetStatement(s.name(), s.expression(), line);
            case ConstStatement      s -> new ConstStatement(s.name(), s.value(), line);
            case AssignStatement     s -> new AssignStatement(s.name(), s.value(), line);
            case PrintStatement      s -> new PrintStatement(s.value(), line);
            case ExprStatement       s -> new ExprStatement(s.value(), line);
            case IfStatement         s -> new IfStatement(s.condition(), s.thenBranch(), s.elseBranch(), line);
            case WhileStatement      s -> new WhileStatement(s.condition(), s.body(), line);
            case ForStatement        s -> new ForStatement(s.init(), s.condition(), s.increment(), s.body(), line);
            case ForeachStatement    s -> new ForeachStatement(s.variable(), s.iterable(), s.body(), line);
            case FunctionStatement   s -> new FunctionStatement(s.name(), s.params(), s.body(), line);
            case ReturnStatement     s -> new ReturnStatement(s.value(), line);
            case ClassStatement      s -> new ClassStatement(s.name(), s.body(), line);
            case BlockStatement      s -> new BlockStatement(s.statements(), line);
            case BreakStatement      s -> new BreakStatement(line);
            case ContinueStatement   s -> new ContinueStatement(line);
            case IncrementStatement  s -> new IncrementStatement(s.name(), line);
            case DecrementStatement  s -> new DecrementStatement(s.name(), line);
            case IndexAssignStatement s -> new IndexAssignStatement(s.target(), s.index(), s.value(), line);
            case SetFieldStatement   s -> new SetFieldStatement(s.object(), s.field(), s.value(), line);
            case JavaBindStatement   s -> new JavaBindStatement(s.alias(), s.className(), s.constructorArgs(), s.hasInvolve(), line);
            case ImportStatement     s -> new ImportStatement(s.library(), s.path(), line);
        };
    }
}