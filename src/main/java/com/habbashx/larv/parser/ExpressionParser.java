package com.habbashx.larv.parser;


import com.habbashx.larv.lexer.TokenType;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.exception.ParseException;

import java.util.List;

import com.habbashx.larv.parser.rule.InfixRule;
import com.habbashx.larv.parser.rule.PrefixRule;
import com.habbashx.larv.parser.stream.TokenStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Parses expressions using a <b>Pratt (top-down operator precedence) parser</b>
 * backed by two {@link EnumMap} registries.
 *
 * <ul>
 *   <li>{@link #prefixRules} — one {@link PrefixRule} per token that can
 *       <em>start</em> an expression (literals, {@code this}, {@code new},
 *       identifiers).</li>
 *   <li>{@link #infixRules}  — one {@link InfixRule} per token that can
 *       <em>extend</em> a left-hand expression (binary operators, {@code (},
 *       {@code .}, {@code [}).</li>
 * </ul>
 *
 * <p>Adding a new operator or literal requires exactly two steps:
 * <ol>
 *   <li>Write a private parse method.</li>
 *   <li>Register it in {@link #buildPrefixRules()} or {@link #buildInfixRules()}.</li>
 * </ol>
 * {@link #parse(int)} itself never needs to change.</p>
 *
 * <pre>
 * Precedence (low → high)
 * ────────────────────────
 *   ASSIGNMENT  =
 *   EQUALITY    == !=
 *   COMPARISON  < > <= >=
 *   TERM        + -
 *   POSTFIX     () . []
 * </pre>
 */
public final class ExpressionParser {

    private final TokenStream stream;
    private final ArgumentParser argParser;

    private final Map<TokenType, PrefixRule> prefixRules;
    private final Map<TokenType, InfixRule>  infixRules;
    private final Map<TokenType, Integer>    infixPrecedence;

    public ExpressionParser(TokenStream stream, ArgumentParser argParser) {
        this.stream          = stream;
        this.argParser       = argParser;
        this.prefixRules     = buildPrefixRules();
        this.infixRules      = buildInfixRules();
        this.infixPrecedence = buildInfixPrecedence();
    }

    private @NotNull Map<TokenType, PrefixRule> buildPrefixRules() {
        Map<TokenType, PrefixRule> map = new EnumMap<>(TokenType.class);

        map.put(TokenType.NUMBER, this::parseNumber);
        map.put(TokenType.STRING, this::parseString);
        map.put(TokenType.THIS, this::parseThis);
        map.put(TokenType.NEW, this::parseNew);
        map.put(TokenType.IDENTIFIER, this::parseIdentifier);
        map.put(TokenType.LPAREN, this::parseGroup);
        map.put(TokenType.NIL,        this::parseNil);
        map.put(TokenType.TRUE, () -> new BooleanExpression(true));
        map.put(TokenType.FALSE, () -> new BooleanExpression(false));
        map.put(TokenType.MINUS, this::parseUnaryMinus);
        map.put(TokenType.BANG,  this::parseLogicalNot);
        map.put(TokenType.LBRACKET,this::parseArrayLiteral);

        return map;
    }

    private @NotNull Map<TokenType, InfixRule> buildInfixRules() {
        Map<TokenType, InfixRule> map = new EnumMap<>(TokenType.class);

        map.put(TokenType.AND,      this::parseLogical);
        map.put(TokenType.OR,       this::parseLogical);
        map.put(TokenType.QUESTION, this::parseTernary);
        map.put(TokenType.EQEQ, this::parseBinary);
        map.put(TokenType.NOTEQ, this::parseBinary);
        map.put(TokenType.LT, this::parseBinary);
        map.put(TokenType.GT, this::parseBinary);
        map.put(TokenType.LTE, this::parseBinary);
        map.put(TokenType.GTE, this::parseBinary);
        map.put(TokenType.PLUS,  this::parseBinary);
        map.put(TokenType.MINUS, this::parseBinary);
        map.put(TokenType.STAR,  this::parseBinary);
        map.put(TokenType.SLASH, this::parseBinary);
        map.put(TokenType.LPAREN,   this::parseCall);
        map.put(TokenType.DOT,      this::parseGet);
        map.put(TokenType.LBRACKET, this::parseIndex);

        return map;
    }

    private @NotNull Map<TokenType, Integer> buildInfixPrecedence() {
        Map<TokenType, Integer> map = new EnumMap<>(TokenType.class);

        map.put(TokenType.EQUAL,    Precedence.ASSIGNMENT);

        map.put(TokenType.QUESTION,  Precedence.TERNARY);

        map.put(TokenType.OR,      Precedence.LOGICAL_OR);
        map.put(TokenType.AND,     Precedence.LOGICAL_AND);

        map.put(TokenType.EQEQ,    Precedence.EQUALITY);
        map.put(TokenType.NOTEQ,   Precedence.EQUALITY);

        map.put(TokenType.LT,      Precedence.COMPARISON);
        map.put(TokenType.GT,      Precedence.COMPARISON);
        map.put(TokenType.LTE,     Precedence.COMPARISON);
        map.put(TokenType.GTE,     Precedence.COMPARISON);

        map.put(TokenType.PLUS,    Precedence.TERM);
        map.put(TokenType.MINUS,   Precedence.TERM);
        map.put(TokenType.STAR,    Precedence.FACTOR);
        map.put(TokenType.SLASH,   Precedence.FACTOR);

        map.put(TokenType.LPAREN,   Precedence.POSTFIX);
        map.put(TokenType.DOT,      Precedence.POSTFIX);
        map.put(TokenType.LBRACKET, Precedence.POSTFIX);


        return map;
    }

    /** Parses a full expression starting at the lowest precedence. */
    public Expression parse() {
        return parse(Precedence.NONE);
    }

    /**
     * Core Pratt loop — parses an expression whose operators bind at least
     * as tightly as {@code minPrecedence}.
     *
     * <ol>
     *   <li>Consume the current token and fire its {@link PrefixRule}.</li>
     *   <li>While the next token's precedence exceeds {@code minPrecedence},
     *       consume it and fire its {@link InfixRule} with the current left.</li>
     * </ol>
     */
    public Expression parse(int minPrecedence) {
        stream.advance();
        PrefixRule prefix = prefixRules.get(stream.previous().tokenType());

        if (prefix == null) {
            throw new ParseException(
                    "Expected an expression, got: " + stream.previous().tokenType(),
                    stream.previous()
            );
        }

        Expression left = prefix.parse();

        while (precedenceOf(stream.peek().tokenType()) > minPrecedence) {
            stream.advance();
            InfixRule infix = infixRules.get(stream.previous().tokenType());
            left = infix.parse(left);
        }

        return left;
    }


    @Contract(" -> new")
    private @NotNull Expression parseNumber() {
        return new NumberExpression(Double.parseDouble(stream.previous().value()));
    }

    @Contract(" -> new")
    private @NotNull Expression parseString() {
        return new StringExpression(stream.previous().value());
    }

    @Contract(" -> new")
    private @NotNull Expression parseThis() {
        return new ThisExpression();
    }

    @Contract(" -> new")
    private @NotNull Expression parseIdentifier() {
        String name = stream.previous().value();

        if (stream.match(TokenType.EQUAL)) {
            Expression value = parse(Precedence.ASSIGNMENT - 1);
            return new AssignExpression(name, value);
        }

        return new VarExpression(name);
    }

    @Contract(" -> new")
    private @NotNull Expression parseNew() {
        String className = stream.consumeValue(TokenType.IDENTIFIER, "Expected class name after 'new'");
        stream.consume(TokenType.LPAREN, "Expected '(' after class name");
        List<Expression> args = argParser.parseArguments(this::parse);
        return new NewExpression(className, args);
    }

    private @NotNull Expression parseBinary(Expression left) {
        String     operator  = stream.previous().value();
        int        precedence = precedenceOf(stream.previous().tokenType());
        Expression right     = parse(precedence);
        return new BinaryExpression(left, operator, right);
    }

    private @NotNull Expression parseCall(Expression left) {
        List<Expression> args = argParser.parseArguments(this::parse);
        return new CallExpression(left, args);
    }

    @Contract("_ -> new")
    private @NotNull Expression parseGet(Expression left) {
        String field = stream.consumeValue(TokenType.IDENTIFIER, "Expected field name after '.'");

        if (stream.match(TokenType.EQUAL)) {
            Expression value = parse(Precedence.ASSIGNMENT - 1);
            return new SetExpression(left, field, value);
        }

        return new GetExpression(left, field);
    }

    @Contract(" -> new")
    private @NotNull Expression parseGroup() {
        Expression inner = parse(Precedence.NONE);
        stream.consume(TokenType.LPAREN, "Expected ')' to close grouped expression");
        return new GroupExpression(inner);
    }

    @Contract("_ -> new")
    private @NotNull Expression parseIndex(Expression left) {
        Expression index = parse();
        stream.consume(TokenType.RBRACKET, "Expected ']' after index");
        return new IndexExpression(left, index);
    }

    private int precedenceOf(TokenType type) {
        return infixPrecedence.getOrDefault(type, Precedence.NONE);
    }

    /** Parses the {@code nil} literal — produces a LiteralExpression wrapping Java null. */
    @Contract(" -> new")
    private @NotNull Expression parseNil() {
        return new LiteralExpression(null);
    }

    /**
     * Parses unary minus:  -expr
     * The MINUS token has already been consumed as the prefix token.
     * Parses at UNARY precedence so  -a * b  is read as  (-a) * b.
     */
    @Contract(" -> new")
    private @NotNull Expression parseUnaryMinus() {
        Expression operand = parse(Precedence.UNARY);
        return new UnaryExpression("-", operand);
    }

    @Contract(" -> new")
    private @NotNull Expression parseArrayLiteral() {
        List<Expression> elements = new java.util.ArrayList<>();
        if (!stream.check(TokenType.RBRACKET)) {
            do {
                elements.add(parse());
            } while (stream.match(TokenType.COMMA));
        }
        stream.consume(TokenType.RBRACKET, "Expected ']' to close array literal");
        return new ArrayExpression(elements);
    }

    /**
     * Parses a short-circuit logical expression: {@code left && right} or {@code left || right}.
     * Right side is parsed at one precedence higher so the operator is left-associative.
     */
    private @NotNull Expression parseLogical(Expression left) {
        String op = stream.previous().value();           // "&&" or "||"
        int prec  = precedenceOf(stream.previous().tokenType());
        Expression right = parse(prec);                  // left-associative
        return new LogicalExpression(left, op, right);
    }

    /**
     * Parses the ternary expression: {@code condition ? thenExpr, elseExpr}.
     * The {@code ?} has already been consumed as the infix token.
     * Comma separates the two branches (avoids conflict with function-call commas).
     */
    @Contract("_ -> new")
    private @NotNull Expression parseTernary(Expression condition) {
        Expression thenBranch = parse(Precedence.NONE);
        stream.consume(TokenType.COMMA, "Expected ',' between ternary branches — use: condition ? thenValue, elseValue");
        Expression elseBranch = parse(Precedence.TERNARY - 1);
        return new TernaryExpression(condition, thenBranch, elseBranch);
    }

    /**
     * Parses logical NOT: {@code !expr}.
     * The {@code !} has already been consumed as the prefix token.
     */
    @Contract(" -> new")
    private @NotNull Expression parseLogicalNot() {
        Expression operand = parse(Precedence.UNARY);
        return new UnaryExpression("!", operand);
    }
}