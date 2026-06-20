package com.habbashx.larv.lexer;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.lexer.Token;
import com.habbashx.larv.lexer.TokenType;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw Larv source text into a flat list of {@link Token} objects.
 *
 * <h2>New in this version</h2>
 * <ul>
 *   <li><b>Raw / multi-line strings</b> — triple-quote delimited {@code """..."""}.
 *       No escape processing is done; the content is emitted verbatim as a
 *       {@link TokenType#RAW_STRING} token.</li>
 *   <li><b>New keywords</b> — {@code try}, {@code catch}, {@code finally},
 *       {@code throw}, {@code switch}, {@code case}, {@code default}, {@code enum}.</li>
 * </ul>
 */
public class Lexer {

    private final String      source;
    private final List<Token> tokens = new ArrayList<>();
    private int start     = 0;
    private int current   = 0;
    private int line      = 1;
    private int lineStart = 0;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, col()));
        return tokens;
    }

    private int col() { return start - lineStart + 1; }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(' -> addToken(TokenType.LPAREN);
            case ')' -> addToken(TokenType.RPAREN);
            case '{' -> addToken(TokenType.LBRACE);
            case '}' -> addToken(TokenType.RBRACE);
            case ',' -> addToken(TokenType.COMMA);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '[' -> addToken(TokenType.LBRACKET, "[");
            case ']' -> addToken(TokenType.RBRACKET, "]");
            case ':' -> addToken(TokenType.COLON,    ":");
            case '.' -> addToken(TokenType.DOT,      ".");

            case '+' -> { if (match('+')) addToken(TokenType.PLUS_PLUS);   else if (match('=')) addToken(TokenType.PLUS_EQUAL);  else addToken(TokenType.PLUS); }
            case '-' -> { if (match('-')) addToken(TokenType.MINUS_MINUS); else if (match('=')) addToken(TokenType.MINUS_EQUAL); else if (match('>')) addToken(TokenType.ARROW); else addToken(TokenType.MINUS); }
            case '*' -> { if (match('=')) addToken(TokenType.STAR_EQUAL);  else addToken(TokenType.STAR); }
            case '/' -> {
                if (match('/')) { while (!isAtEnd() && peek() != '\n') advance(); }
                else if (match('=')) addToken(TokenType.SLASH_EQUAL);
                else addToken(TokenType.SLASH);
            }
            case '=' -> { if (match('=')) addToken(TokenType.EQEQ);  else addToken(TokenType.EQUAL); }
            case '!' -> { if (match('=')) addToken(TokenType.NOTEQ); else addToken(TokenType.BANG); }
            case '&' -> { if (match('&')) addToken(TokenType.AND);   else throw new LarvError("Unexpected character '&' — did you mean '&&'?", line, col(), LarvError.Kind.LEXER); }
            case '|' -> { if (match('|')) addToken(TokenType.OR);    else throw new LarvError("Unexpected character '|' — did you mean '||'?", line, col(), LarvError.Kind.LEXER); }
            case '?' -> addToken(TokenType.QUESTION);
            case '<' -> { if (match('=')) addToken(TokenType.LTE); else addToken(TokenType.LT); }
            case '>' -> { if (match('=')) addToken(TokenType.GTE); else addToken(TokenType.GT); }

            case '"' -> {
                if (current + 1 < source.length()
                        && source.charAt(current) == '"'
                        && source.charAt(current + 1) == '"') {
                    current += 2;
                    rawString();
                } else {
                    string();
                }
            }

            case '\n' -> { line++; lineStart = current; }
            case ' ', '\r', '\t' -> {}

            default -> {
                if (isDigit(c))      number();
                else if (isAlpha(c)) identifier();
                else throw new LarvError(
                            "Unexpected character '" + c + "'", line, col(), LarvError.Kind.LEXER);
            }
        }
    }

    private void string() {
        int startLine = line;
        int startCol  = col();
        StringBuilder sb = new StringBuilder();
        int parenDepth = 0;

        while (!isAtEnd()) {
            char c = peek();

            if (c == '"' && parenDepth == 0) break;
            if (c == '\n') { line++; lineStart = current + 1; }

            if (c == '\\' && current + 1 < source.length()) {
                advance();
                char escaped = advance();
                switch (escaped) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 't'  -> sb.append('\t');
                    case 'r'  -> sb.append('\r');
                    default   -> { sb.append('\\'); sb.append(escaped); }
                }
                continue;
            }

            if (c == '(') parenDepth++;
            else if (c == ')' && parenDepth > 0) parenDepth--;

            sb.append(advance());
        }

        if (isAtEnd()) throw new LarvError(
                "Unterminated string — opened here", startLine, startCol, LarvError.Kind.LEXER);
        advance(); // closing "
        addToken(TokenType.STRING, sb.toString());
    }

    /**
     * Scans a triple-quoted raw string. The opening {@code """} has already been
     * consumed. Content is captured verbatim (no escape processing) until the
     * next {@code """} is encountered.
     *
     * <p>Leading newline immediately after {@code """} is stripped so that:</p>
     * <pre>
     *   const q = """
     *       SELECT *
     *       FROM users
     *   """
     * </pre>
     * produces {@code "    SELECT *\n    FROM users\n"} rather than
     * {@code "\n    SELECT *\n    FROM users\n"}.
     */
    private void rawString() {
        int startLine = line;
        int startCol  = col();
        StringBuilder sb = new StringBuilder();

        if (!isAtEnd() && peek() == '\n') {
            advance();
            line++;
            lineStart = current;
        }

        while (!isAtEnd()) {
            if (peek() == '"'
                    && current + 1 < source.length() && source.charAt(current + 1) == '"'
                    && current + 2 < source.length() && source.charAt(current + 2) == '"') {
                current += 3;
                addToken(TokenType.RAW_STRING, sb.toString());
                return;
            }
            char c = advance();
            if (c == '\n') { line++; lineStart = current; }
            sb.append(c);
        }

        throw new LarvError(
                "Unterminated raw string — opened here", startLine, startCol, LarvError.Kind.LEXER);
    }

    private void number() {
        while (!isAtEnd() && isDigit(peek())) advance();
        if (!isAtEnd() && peek() == '.' && current + 1 < source.length()
                && isDigit(source.charAt(current + 1))) {
            advance();
            while (!isAtEnd() && isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, source.substring(start, current));
    }

    private void identifier() {
        while (!isAtEnd() && isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = switch (text) {
            case "var"      -> TokenType.VAR;
            case "const"    -> TokenType.CONST;
            case "if"       -> TokenType.IF;
            case "else"     -> TokenType.ELSE;
            case "while"    -> TokenType.WHILE;
            case "for"      -> TokenType.FOR;
            case "in"       -> TokenType.IN;
            case "func"     -> TokenType.FUNC;
            case "return"   -> TokenType.RETURN;
            case "break"    -> TokenType.BREAK;
            case "continue" -> TokenType.CONTINUE;
            case "class"    -> TokenType.CLASS;
            case "new"      -> TokenType.NEW;
            case "this"     -> TokenType.THIS;
            case "include"  -> TokenType.INCLUDE;
            case "from"     -> TokenType.FROM;
            case "involve"  -> TokenType.INVOLVE;
            case "import"   -> TokenType.IMPORT;
            case "module"   -> TokenType.MODULE;
            case "as"       -> TokenType.AS;
            case "nil"      -> TokenType.NIL;
            case "true"     -> TokenType.TRUE;
            case "false"    -> TokenType.FALSE;
            case "try"      -> TokenType.TRY;
            case "catch"    -> TokenType.CATCH;
            case "finally"  -> TokenType.FINALLY;
            case "throw"    -> TokenType.THROW;
            case "switch"   -> TokenType.SWITCH;
            case "case"     -> TokenType.CASE;
            case "default"  -> TokenType.DEFAULT;
            case "enum"     -> TokenType.ENUM;
            case "get"      -> TokenType.GET;
            case "set"      -> TokenType.SET;
            case "sync"     -> TokenType.SYNC;
            case "core"     -> TokenType.CORE;
            case "override" -> TokenType.OVERRIDE;
            case "defer"    -> TokenType.DEFER;
            case "atomic" -> TokenType.ATOMIC;
            case "volatile" -> TokenType.VOLATILE;
            default         -> TokenType.IDENTIFIER;
        };
        addToken(type, text);
    }

    @Contract(pure = true) private boolean isAtEnd() { return current >= source.length(); }
    @Contract(mutates = "this") private char advance() { return source.charAt(current++); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private boolean match(char exp)   { if (isAtEnd() || source.charAt(current) != exp) return false; current++; return true; }
    private void addToken(TokenType t) { addToken(t, source.substring(start, current)); }
    private void addToken(TokenType t, String val) { tokens.add(new Token(t, val, line, col())); }
    @Contract(pure = true) private boolean isDigit(char c)       { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_'; }
    private boolean isAlphaNumeric(char c){ return isAlpha(c) || isDigit(c); }
}
