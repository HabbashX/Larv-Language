package com.habbashx.larv.parser;

import com.habbashx.larv.lexer.TokenType;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.exception.ParseException;
import com.habbashx.larv.parser.rule.StatementRule;
import com.habbashx.larv.parser.stream.TokenStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses every statement in the Larv grammar using a
 * <b>Strategy + Command Registry</b> pattern.
 *
 * <h2>New statements in this version</h2>
 * <ul>
 *   <li>{@code try / catch / finally} — {@link #parseTryCatch()}</li>
 *   <li>{@code throw expr}            — {@link #parseThrow()}</li>
 *   <li>{@code switch expr { case … default … }}</li>
 *   <li>{@code enum Name { A, B, C }}</li>
 * </ul>
 */
public class StatementParser {

    private final TokenStream      stream;
    private final ExpressionParser exprParser;
    private final ArgumentParser   argParser;

    private final Map<TokenType, StatementRule> registry;

    public StatementParser(TokenStream stream, ExpressionParser exprParser, ArgumentParser argParser) {
        this.stream     = stream;
        this.exprParser = exprParser;
        this.argParser  = argParser;
        this.registry   = buildRegistry();
    }

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
        map.put(TokenType.RETURN,   this::parseReturn);
        map.put(TokenType.CLASS,    this::parseClassDecl);
        map.put(TokenType.INCLUDE,  this::parseJavaBind);
        map.put(TokenType.IMPORT,   this::parseImport);
        map.put(TokenType.MODULE,   this::parseModule);
        map.put(TokenType.TRY,      this::parseTryCatch);
        map.put(TokenType.THROW,    this::parseThrow);
        map.put(TokenType.SWITCH,   this::parseSwitch);
        map.put(TokenType.ENUM,     this::parseEnum);
        map.put(TokenType.DEFER,    this::parseDefer);
        map.put(TokenType.ATOMIC,   this::parseAtomic);
        map.put(TokenType.VOLATILE, this::parseVolatile);

        return map;
    }

    public Statement parse() {
        int line = stream.peek().line();

        if (stream.check(TokenType.FUNC) || stream.check(TokenType.SYNC) ||
                stream.check(TokenType.CORE) || stream.check(TokenType.OVERRIDE)) {
            return withLine(parseFunctionDecl(), line);
        }

        StatementRule rule = registry.get(stream.peek().tokenType());
        if (rule != null) {
            stream.advance();
            return withLine(rule.parse(), line);
        }

        if (stream.check(TokenType.IDENTIFIER)) {
            TokenType next = stream.peekNext().tokenType();
            String op = compoundOp(next);

            if (op != null) {
                String name = stream.advance().value();
                stream.advance();
                Expression rhs = exprParser.parse();
                return withLine(new CompoundAssignStatement(name, op, rhs, -1), line);
            }
            if (next == TokenType.PLUS_PLUS) {
                String name = stream.advance().value();
                stream.advance();
                return withLine(new IncrementStatement(name, -1), line);
            }
            if (next == TokenType.MINUS_MINUS) {
                String name = stream.advance().value();
                stream.advance();
                return withLine(new DecrementStatement(name, -1), line);
            }
        }

        // 4. Fallback to expression
        return withLine(parseExprStatement(), line);
    }
    @Contract(pure = true)
    private @Nullable String compoundOp(@NotNull TokenType t) {
        return switch (t) {
            case PLUS_EQUAL  -> "+";
            case MINUS_EQUAL -> "-";
            case STAR_EQUAL  -> "*";
            case SLASH_EQUAL -> "/";
            default          -> null;
        };
    }


    private @NotNull Statement parseVar() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected variable name after 'var'");

        String type = "any";

        if (stream.match(TokenType.COLON)) {
            type = stream.consumeValue(TokenType.IDENTIFIER,"expected type name after ':'");
        }

        Expression value = stream.match(TokenType.EQUAL) ? exprParser.parse() : null;

        boolean hasGetter = false;
        boolean hasSetter = false;
        if (stream.match(TokenType.COLON)) {
            do {
                if (stream.match(TokenType.GET)) {
                    hasGetter = true;
                } else if (stream.match(TokenType.SET)) {
                    hasSetter = true;
                } else {
                    throw new com.habbashx.larv.parser.exception.ParseException(
                            "Expected 'get' or 'set' after ':'", stream.peek());
                }
            } while (stream.match(TokenType.COMMA));
        }
        return new VarStatement(name,type, value, hasGetter, hasSetter, false,-1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseConst() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected constant name after 'const'");

        String type = "any";
        if (stream.match(TokenType.COLON)) {
            type = stream.consumeValue(TokenType.IDENTIFIER, "Expected type name after ':'");
        }

        stream.consume(TokenType.EQUAL, "Expected '=' after constant name");
        Expression value = exprParser.parse();

        boolean hasGetter = false;
        if (stream.match(TokenType.COLON)) {
            if (stream.match(TokenType.GET)) {
                hasGetter = true;
            } else {
                throw new com.habbashx.larv.parser.exception.ParseException(
                        "Constants can only declare 'get', not 'set'", stream.peek());
            }
        }

        return new ConstStatement(name, type, value, hasGetter, -1);
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
        if (stream.match(TokenType.IF)) {
            Statement nested = withLine(parseIf(), stream.peek().line());
            return List.of(nested);
        }
        stream.consume(TokenType.LBRACE, "Expected '{' to open else-body");
        return parseBlock();
    }

    @Contract(" -> new")
    private @NotNull Statement parseWhile() {
        Expression condition = exprParser.parse();
        stream.consume(TokenType.LBRACE, "Expected '{' to open while-body");
        return new WhileStatement(condition, parseBlock(), -1);
    }

    private Statement parseFor() {
        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.IN)) {
            return parseForeach();
        }
        // key, value in map — two identifiers separated by comma before 'in'
        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.COMMA)) {
            return parseForeach();
        }
        return parseTraditionalFor();
    }

    @Contract(" -> new")
    private @NotNull Statement parseForeach() {
        String variable = stream.consumeValue(TokenType.IDENTIFIER, "Expected loop variable after 'for'");
        // Optional second variable: for key, value in map
        String valueVariable = null;
        if (stream.match(TokenType.COMMA)) {
            valueVariable = stream.consumeValue(TokenType.IDENTIFIER, "Expected value variable name after ','");
        }
        stream.consume(TokenType.IN, "Expected 'in' after loop variable(s)");
        Expression iterable = exprParser.parse();
        stream.consume(TokenType.LBRACE, "Expected '{' to open foreach-body");
        return new ForeachStatement(variable, valueVariable, iterable, parseBlock(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseTraditionalFor() {
        Statement  init      = parseForInit();
        stream.consume(TokenType.SEMICOLON, "Expected ';' after for-init");
        Expression condition = exprParser.parse();
        stream.consume(TokenType.SEMICOLON, "Expected ';' after for-condition");
        Statement  increment = parseForIncrement();
        stream.consume(TokenType.LBRACE, "Expected '{' to open for-body");
        return new ForStatement(init, condition, increment, parseBlock(), -1);
    }

    private @NotNull Statement parseForInit() {
        if (stream.check(TokenType.VAR)) {
            stream.advance();
            return parseVar();
        }

        if (stream.check(TokenType.IDENTIFIER) && stream.checkNext(TokenType.EQUAL)) {
            String name = stream.advance().value();
            stream.consume(TokenType.EQUAL, "Expected '='");

            return new VarStatement(name, "any", exprParser.parse(), false, false, false, -1);
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

    /**
     * Parses function declarations including optional modifiers:
     * - func name() { ... }
     * - sync func name() { ... }
     * - core func name() { ... }
     * - sync override func name() { ... }
     */
    @Contract(" -> new")
    private @NotNull Statement parseFunctionDecl() {
        boolean isSync     = stream.match(TokenType.SYNC);
        boolean isCore     = stream.match(TokenType.CORE);
        boolean isOverride = stream.match(TokenType.OVERRIDE);

        stream.consume(TokenType.FUNC, "Expected 'func' keyword");
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected function name");

        stream.consume(TokenType.LPAREN, "Expected '('");
        List<FunctionStatement.Parameter> parameters = new ArrayList<>();
        if (!stream.check(TokenType.RPAREN)) {
            do {
                String pName = stream.consumeValue(TokenType.IDENTIFIER, "Expected parameter name");
                String pType = "any";

                if (stream.match(TokenType.COLON)) {
                    pType = stream.consumeValue(TokenType.IDENTIFIER, "Expected type after ':'");
                }
                parameters.add(new FunctionStatement.Parameter(pName, pType));
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(TokenType.RPAREN, "Expected ')'");

        String returnType = "void";
        if (stream.match(TokenType.ARROW)) {
            returnType = stream.consumeValue(TokenType.IDENTIFIER, "Expected return type after '->'");
        }

        stream.consume(TokenType.LBRACE, "Expected '{'");

        return new FunctionStatement(name, parameters, parseBlock(), returnType, isSync, isCore, isOverride, -1);
    }
    /** Parses {@code core [sync] func name(...) [-> type] { ... }} — non-inheritable method. */
    @Contract(" -> new")
    private @NotNull Statement parseCoreFunc() {
        boolean isSync = stream.match(TokenType.SYNC);

        stream.consume(TokenType.FUNC, "Expected 'func' after 'core'");
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected function name");

        stream.consume(TokenType.LPAREN, "Expected '(' after function name");
        List<FunctionStatement.Parameter> params = new ArrayList<>();
        if (!stream.check(TokenType.RPAREN)) {
            do {
                String pName = stream.consumeValue(TokenType.IDENTIFIER, "Expected parameter name");
                String pType = "any"; // Default type if omitted

                if (stream.match(TokenType.COLON)) {
                    pType = stream.consumeValue(TokenType.IDENTIFIER, "Expected type after ':'");
                }
                params.add(new FunctionStatement.Parameter(pName, pType));
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(TokenType.RPAREN, "Expected ')' after parameters");

        String returnType = "void";
        if (stream.match(TokenType.ARROW)) {
            returnType = stream.consumeValue(TokenType.IDENTIFIER, "Expected return type after '->'");
        }

        stream.consume(TokenType.LBRACE, "Expected '{' to open core function body");

        return new FunctionStatement(name, params, parseBlock(), returnType, isSync, true, false, -1);
    }

    /** Parses {@code override [sync] func name(...) [-> type] { ... }} — overrides a parent method. */
    @Contract(" -> new")
    private @NotNull Statement parseOverrideFunc() {
        boolean isSync = stream.match(TokenType.SYNC);

        stream.consume(TokenType.FUNC, "Expected 'func' after 'override'");
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected function name");

        stream.consume(TokenType.LPAREN, "Expected '(' after function name");
        List<FunctionStatement.Parameter> params = new ArrayList<>();
        if (!stream.check(TokenType.RPAREN)) {
            do {
                String pName = stream.consumeValue(TokenType.IDENTIFIER, "Expected parameter name");
                String pType = "any";

                if (stream.match(TokenType.COLON)) {
                    pType = stream.consumeValue(TokenType.IDENTIFIER, "Expected type after ':'");
                }
                params.add(new FunctionStatement.Parameter(pName, pType));
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(TokenType.RPAREN, "Expected ')' after parameters");

        String returnType = "void";
        if (stream.match(TokenType.ARROW)) {
            returnType = stream.consumeValue(TokenType.IDENTIFIER, "Expected return type after '->'");
        }

        stream.consume(TokenType.LBRACE, "Expected '{' to open override function body");

        return new FunctionStatement(name, params, parseBlock(), returnType, isSync, false, true, -1);
    }
    @Contract(" -> new")
    private @NotNull Statement parseReturn() {
        return new ReturnStatement(exprParser.parse(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseClassDecl() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected class name after 'class'");
        String superclassName = null;
        if (stream.match(TokenType.COLON)) {
            superclassName = stream.consumeValue(TokenType.IDENTIFIER, "Expected superclass name after ':'");
        }
        stream.consume(TokenType.LBRACE, "Expected '{' to open class body");
        List<Statement> body = new ArrayList<>();
        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
            body.add(parse());
        }
        stream.consume(TokenType.RBRACE, "Expected '}' to close class body");
        return new ClassStatement(name, superclassName, body, -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseExprStatement() {
        return new ExprStatement(exprParser.parse(), -1);
    }

    @Contract(" -> new")
    private @NotNull Statement parseModule() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected module name after 'module'");
        stream.consume(TokenType.LBRACE, "Expected '{' to open module body");
        List<Statement> body = new ArrayList<>();
        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
            body.add(parse());
        }
        stream.consume(TokenType.RBRACE, "Expected '}' to close module body");
        return new ModuleStatement(name, body, -1);
    }

    private static final java.util.Set<String> STDLIB = java.util.Set.of(
            "math", "io", "string", "http", "system",
            "regex", "date", "base64", "properties",
            "json", "jdbc", "socket", "thread","server"
    );

    @Contract
    private @NotNull Statement parseImport() {
        int line = stream.peek().line();
        String value = stream.consumeValue(TokenType.STRING,
                "Expected a quoted name after 'import', e.g. import \"math\" or import \"com.foo.MyFile\"");
        stream.match(TokenType.SEMICOLON);
        if (STDLIB.contains(value)) {
            return new ImportStatement(value, null, line);
        }
        return new ImportStatement(null, value, line);
    }

    @Contract(" -> new")
    private @NotNull Statement parseJavaBind() {
        int line         = stream.peek().line();
        String alias     = stream.consumeValue(TokenType.IDENTIFIER, "Expected alias after 'include'");
        stream.consume(TokenType.FROM, "Expected 'from' after alias in java binding");
        String className = stream.consumeValue(TokenType.STRING, "Expected fully-qualified class name string after 'from'");
        boolean hasInvolve = false;
        List<String> constructorArgs = new ArrayList<>();
        if (stream.match(TokenType.INVOLVE)) {
            hasInvolve = true;
            stream.consume(TokenType.LBRACE, "Expected '{' after 'involve'");
            while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
                String arg = stream.consumeValue(TokenType.STRING, "Expected string argument inside 'involve { ... }'");
                constructorArgs.add(arg);
                stream.match(TokenType.COMMA);
            }
            stream.consume(TokenType.RBRACE, "Expected '}' to close 'involve' argument list");
        }
        stream.match(TokenType.SEMICOLON);
        return new JavaBindStatement(alias, className, constructorArgs, hasInvolve, line);
    }

    /**
     * Parses:
     * <pre>
     *   try {
     *       ...
     *   } catch (e) {
     *       ...
     *   } finally {
     *       ...
     *   }
     * </pre>
     * Both {@code catch} and {@code finally} are optional, but at least one
     * must be present.
     *
     * The {@code try} keyword has already been consumed by the registry.
     */
    @Contract(" -> new")
    private @NotNull Statement parseTryCatch() {
        stream.consume(TokenType.LBRACE, "Expected '{' to open try-body");
        List<Statement> tryBody = parseBlock();

        String          catchVar  = null;
        List<Statement> catchBody = new ArrayList<>();
        List<Statement> finallyBody = new ArrayList<>();

        if (stream.match(TokenType.CATCH)) {
            stream.consume(TokenType.LPAREN, "Expected '(' after 'catch'");
            catchVar = stream.consumeValue(TokenType.IDENTIFIER, "Expected variable name in catch(...)");
            stream.consume(TokenType.RPAREN, "Expected ')' after catch variable");
            stream.consume(TokenType.LBRACE, "Expected '{' to open catch-body");
            catchBody = parseBlock();
        }

        if (stream.match(TokenType.FINALLY)) {
            stream.consume(TokenType.LBRACE, "Expected '{' to open finally-body");
            finallyBody = parseBlock();
        }

        if (catchBody.isEmpty() && finallyBody.isEmpty()) {
            throw new ParseException("try block must have at least a catch or finally clause", stream.peek());
        }

        return new TryCatchStatement(tryBody, catchVar, catchBody, finallyBody, -1);
    }

    /**
     * Parses:  {@code throw expr}
     * The {@code throw} keyword has already been consumed.
     */
    @Contract(" -> new")
    private @NotNull Statement parseThrow() {
        return new ThrowStatement(exprParser.parse(), -1);
    }

    /**
     * Parses:
     * <pre>
     *   switch expr {
     *       case "a", "b" : { ... }
     *       case 1        : { ... }
     *       default       : { ... }
     *   }
     * </pre>
     * The {@code switch} keyword has already been consumed.
     * Multiple comma-separated values on one {@code case} are treated as
     * an OR — the arm fires if the subject equals any of them.
     */
    @Contract(" -> new")
    private @NotNull Statement parseSwitch() {
        Expression subject = exprParser.parse();
        stream.consume(TokenType.LBRACE, "Expected '{' to open switch body");

        List<SwitchStatement.SwitchCase> cases = new ArrayList<>();
        List<Statement> defaultBody = new ArrayList<>();

        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {

            if (stream.match(TokenType.DEFAULT)) {
                stream.consume(TokenType.COLON, "Expected ':' after 'default'");
                stream.consume(TokenType.LBRACE, "Expected '{' to open default body");
                defaultBody = parseBlock();
                continue;
            }

            stream.consume(TokenType.CASE, "Expected 'case' or 'default' inside switch");

            List<Expression> values = new ArrayList<>();
            values.add(exprParser.parse());
            while (stream.match(TokenType.COMMA)) {
                values.add(exprParser.parse());
            }

            stream.consume(TokenType.COLON, "Expected ':' after case value(s)");
            stream.consume(TokenType.LBRACE, "Expected '{' to open case body");
            List<Statement> body = parseBlock();

            cases.add(new SwitchStatement.SwitchCase(values, body));
        }

        stream.consume(TokenType.RBRACE, "Expected '}' to close switch");
        return new SwitchStatement(subject, cases, defaultBody, -1);
    }

    /**
     * Parses:
     * <pre>
     *   enum Direction { NORTH, SOUTH, EAST, WEST }
     * </pre>
     * The {@code enum} keyword has already been consumed.
     */
    @Contract(" -> new")
    private @NotNull Statement parseEnum() {
        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected enum name after 'enum'");
        stream.consume(TokenType.LBRACE, "Expected '{' to open enum body");

        List<String> variants = new ArrayList<>();
        while (!stream.check(TokenType.RBRACE) && !stream.isAtEnd()) {
            variants.add(stream.consumeValue(TokenType.IDENTIFIER, "Expected enum variant name"));
            stream.match(TokenType.COMMA); // optional trailing comma
        }

        stream.consume(TokenType.RBRACE, "Expected '}' to close enum body");
        return new EnumStatement(name, variants, -1);
    }

    /**
     * Parses: {@code defer expr}
     * The {@code defer} keyword has already been consumed by the registry.
     * The expression is typically a call like {@code file.close()}.
     */
    @Contract(" -> new")
    private @NotNull Statement parseDefer() {
        return new DeferStatement(exprParser.parse(), -1);
    }

    /**
     * Parses: atomic<type> name [= initializer]
     * The 'atomic' keyword has already been consumed by the registry.
     */
    @Contract(" -> new")
    private @NotNull Statement parseAtomic() {
        stream.consume(TokenType.LT, "Expected '<' after 'atomic'");
        String type = stream.consumeValue(TokenType.IDENTIFIER, "Expected type (e.g., int, bool, string) inside atomic<...>");
        stream.consume(TokenType.GT, "Expected '>' after atomic type");

        String name = stream.consumeValue(TokenType.IDENTIFIER, "Expected variable name after atomic<" + type + ">");

        Expression initializer = null;
        if (stream.match(TokenType.EQUAL)) {
            initializer = exprParser.parse();
        }

        return new AtomicStatement(type, name, initializer, -1);
    }

    /**
     * Parses: volatile var name = expr
     */
    @Contract(" -> new")
    private @NotNull Statement parseVolatile() {
        int line = stream.peek().line();
        stream.consume(TokenType.VAR, "Expected 'var' after 'volatile'");

        VarStatement baseVar = (VarStatement) parseVar();

        return new VarStatement(
                baseVar.name(),
                baseVar.type(),
                baseVar.expression(),
                baseVar.hasGetter(),
                baseVar.hasSetter(),
                true,
                line
        );
    }

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
    private static @NotNull Statement withLine(@NotNull Statement st, int line) {
        return switch (st) {
            case VarStatement s -> new VarStatement(
                    s.name(),
                    s.type(),
                    s.expression(),
                    s.hasGetter(),
                    s.hasSetter(),
                    s.isVolatile(),
                    line
            );
            case ConstStatement      s -> new ConstStatement(s.name(),s.type(), s.value(), s.hasGetter(), line);
            case AssignStatement     s -> new AssignStatement(s.name(), s.value(), line);
            case CompoundAssignStatement s -> new CompoundAssignStatement(s.name(), s.operator(), s.value(), line);
            case ModuleStatement     s -> new ModuleStatement(s.name(), s.body(), line);
            case PrintStatement      s -> new PrintStatement(s.value(), line);
            case ExprStatement       s -> new ExprStatement(s.value(), line);
            case IfStatement         s -> new IfStatement(s.condition(), s.thenBranch(), s.elseBranch(), line);
            case WhileStatement      s -> new WhileStatement(s.condition(), s.body(), line);
            case ForStatement        s -> new ForStatement(s.init(), s.condition(), s.increment(), s.body(), line);
            case ForeachStatement    s -> new ForeachStatement(s.variable(), s.valueVariable(), s.iterable(), s.body(), line);
            case FunctionStatement   s -> new FunctionStatement(s.name(), s.params(), s.body(),s.returnType(), s.isSync(), s.isCore(), s.isOverride(), line);
            case ReturnStatement     s -> new ReturnStatement(s.value(), line);
            case ClassStatement      s -> new ClassStatement(s.name(), s.superclassName(), s.body(), line);
            case BlockStatement      s -> new BlockStatement(s.statements(), line);
            case BreakStatement      s -> new BreakStatement(line);
            case ContinueStatement   s -> new ContinueStatement(line);
            case IncrementStatement  s -> new IncrementStatement(s.name(), line);
            case DecrementStatement  s -> new DecrementStatement(s.name(), line);
            case IndexAssignStatement s -> new IndexAssignStatement(s.target(), s.index(), s.value(), line);
            case SetFieldStatement   s -> new SetFieldStatement(s.object(), s.field(), s.value(), line);
            case JavaBindStatement   s -> new JavaBindStatement(s.alias(), s.className(), s.constructorArgs(), s.hasInvolve(), line);
            case ImportStatement     s -> new ImportStatement(s.library(), s.path(), line);
            case TryCatchStatement   s -> new TryCatchStatement(s.tryBody(), s.catchVar(), s.catchBody(), s.finallyBody(), line);
            case ThrowStatement      s -> new ThrowStatement(s.value(), line);
            case SwitchStatement     s -> new SwitchStatement(s.subject(), s.cases(), s.defaultBody(), line);
            case EnumStatement       s -> new EnumStatement(s.name(), s.variants(), line);
            case DeferStatement      s -> new DeferStatement(s.value(), line);
            case AtomicStatement s -> new AtomicStatement(s.type(),s.name(), s.initializer(),line);
        };
    }
}