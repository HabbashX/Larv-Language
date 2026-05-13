package com.habbashx.larv.parser;

import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.parser.stream.TokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for parsing a flat token list into an AST.
 *
 * This class is intentionally thin — it owns no grammar rules of its own.
 * Its only job is to wire the components together and expose the public API.
 *
 * <pre>
 * Architecture overview
 * ──────────────────────
 *
 *   Parser
 *     │
 *     ├── TokenStream        (cursor movement: peek, advance, match, consume)
 *     │
 *     ├── ArgumentParser     (comma-separated arg/param lists, used by both parsers)
 *     │
 *     ├── ExpressionParser   (Pratt chain: assignment → equality → … → primary)
 *     │
 *     └── StatementParser    (one method per keyword; delegates expressions up)
 * </pre>
 */
public final class Parser {

    private final StatementParser statementParser;
    private final TokenStream stream;

    public Parser(List<Token> tokens) {
        stream = new TokenStream(tokens);
        ArgumentParser argParser = new ArgumentParser(stream);
        ExpressionParser exprParser = new ExpressionParser(stream, argParser);
        statementParser = new StatementParser(stream, exprParser, argParser);
    }

    /** Parses all top-level statements until {@code EOF} and returns the AST. */
    public @NotNull List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        while (!stream.isAtEnd()) {
            statements.add(statementParser.parse());
        }
        return statements;
    }
}