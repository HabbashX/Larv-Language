package com.habbashx.larv.lexer;

import com.habbashx.larv.error.LarvError;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw Larv source text into a flat list of {@link Token} objects.
 *
 * <h2>Usage</h2>
 * <pre>
 *   List&lt;Token&gt; tokens = new Lexer(source).tokenize();
 * </pre>
 *
 * <h2>Scanning rules</h2>
 * <ul>
 *   <li><b>Whitespace</b> — spaces, tabs, and carriage returns are silently skipped.</li>
 *   <li><b>Newlines</b>   — increment the line counter (used for error reporting).</li>
 *   <li><b>Comments</b>   — {@code //} line comments are consumed up to the next newline.</li>
 *   <li><b>Numbers</b>    — integer and floating-point literals ({@code 42}, {@code 3.14}).</li>
 *   <li><b>Strings</b>    — double-quoted literals with backslash escape sequences
 *                           ({@code \"}, {@code \\}, {@code \n}, {@code \t}, {@code \r}).
 *                           A {@code "} that appears inside balanced parentheses is NOT
 *                           treated as the closing delimiter — this allows Java FFI argument
 *                           strings like {@code "java.io.FileWriter(\"file.txt\")"} without
 *                           extra escaping.</li>
 *   <li><b>Identifiers</b> — sequences starting with a letter or underscore, followed by
 *                            letters, digits, or underscores. Matched against the keyword
 *                            table and classified accordingly.</li>
 *   <li><b>Operators / punctuation</b> — single- and double-character symbols.</li>
 * </ul>
 *
 * <p>Any unrecognised character throws a {@link LarvError} of kind
 * {@link LarvError.Kind#LEXER} with the exact line and column.</p>
 */
public class Lexer {

    /** The complete source code being scanned. */
    private final String      source;

    /** Accumulated token list, built incrementally by {@link #tokenize()}. */
    private final List<Token> tokens = new ArrayList<>();

    /** Index into {@link #source} where the current token started. */
    private int start       = 0;

    /** Index into {@link #source} of the character currently being examined. */
    private int current     = 0;

    /** Current 1-based source line (incremented on every {@code '\n'}). */
    private int line        = 1;

    /**
     * Index of the first character on the current line.
     * Used together with {@link #start} to compute the 1-based column number.
     */
    private int lineStart   = 0;

    /**
     * Constructs a lexer for the given source text.
     *
     * @param source the full Larv source code to scan; must not be {@code null}
     */
    public Lexer(String source) {
        this.source = source;
    }

    /**
     * Scans the entire source and returns the token list.
     *
     * <p>An {@link TokenType#EOF} sentinel is appended at the end so the
     * parser never has to bounds-check the token list.</p>
     *
     * @return an unmodifiable view of the token list (in source order)
     * @throws LarvError if an unexpected character or unterminated string is found
     */
    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, col()));
        return tokens;
    }

    /**
     * Returns the 1-based column number of the current token start.
     *
     * @return column offset from the beginning of the current line
     */
    private int col() {
        return start - lineStart + 1;
    }

    /**
     * Scans one token starting at {@link #current}.
     *
     * <p>Dispatches on the first character consumed from {@link #source}.
     * Multi-character tokens (e.g. {@code ==}, {@code ++}) peek ahead with
     * {@link #match(char)} before emitting.</p>
     *
     * @throws LarvError on an unrecognised character
     */
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

            case '+' -> { if (match('+')) addToken(TokenType.PLUS_PLUS);   else addToken(TokenType.PLUS); }
            case '-' -> { if (match('-')) addToken(TokenType.MINUS_MINUS); else addToken(TokenType.MINUS); }
            case '*' -> addToken(TokenType.STAR);
            case '/' -> {
                if (match('/')) { while (!isAtEnd() && peek() != '\n') advance(); }
                else addToken(TokenType.SLASH);
            }
            case '=' -> { if (match('=')) addToken(TokenType.EQEQ);  else addToken(TokenType.EQUAL); }
            case '!' -> {
                if (match('=')) addToken(TokenType.NOTEQ);
                else throw new LarvError(
                        "Unexpected character '!' — did you mean '!='?", line, col(), LarvError.Kind.LEXER);
            }
            case '<' -> { if (match('=')) addToken(TokenType.LTE); else addToken(TokenType.LT); }
            case '>' -> { if (match('=')) addToken(TokenType.GTE); else addToken(TokenType.GT); }

            case '"' -> string();

            case '\n' -> { line++; lineStart = current; }   // reset column counter
            case ' ', '\r', '\t' -> {}

            default -> {
                if (isDigit(c))      number();
                else if (isAlpha(c)) identifier();
                else throw new LarvError(
                            "Unexpected character '" + c + "'", line, col(), LarvError.Kind.LEXER);
            }
        }
    }

    /**
     * Scans a string literal, starting after the opening {@code "} (already consumed).
     *
     * <h3>String content rules</h3>
     * <ul>
     *   <li>Normal characters are appended to the value as-is.</li>
     *   <li>A {@code "} inside balanced parentheses is treated as part of the string
     *       value — this allows FFI argument strings like
     *       {@code "java.io.FileWriter(\"example.txt\")"} without extra escaping.</li>
     *   <li>Standard backslash escapes are supported:
     *       {@code \"}, {@code \\}, {@code \n}, {@code \t}, {@code \r}.</li>
     * </ul>
     *
     * @throws LarvError if the string is not closed before end-of-file
     */
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
                advance(); // consume backslash
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
        advance();
        addToken(TokenType.STRING, sb.toString());
    }

    /**
     * Scans a numeric literal (integer or floating-point).
     *
     * <p>Advances past all consecutive digits, and if a {@code '.'} is followed
     * by another digit, also consumes the decimal fraction part.</p>
     */
    private void number() {
        while (!isAtEnd() && isDigit(peek())) advance();
        if (!isAtEnd() && peek() == '.' && current + 1 < source.length()
                && isDigit(source.charAt(current + 1))) {
            advance();
            while (!isAtEnd() && isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, source.substring(start, current));
    }

    /**
     * Scans an identifier or keyword token.
     *
     * <p>Advances past all alphanumeric/underscore characters, extracts the
     * text, and classifies it as a keyword {@link TokenType} or falls back
     * to {@link TokenType#IDENTIFIER} if the text is not reserved.</p>
     */
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
            case "nil"      -> TokenType.NIL;
            case "true"     -> TokenType.TRUE;
            case "false"    -> TokenType.FALSE;
            default         -> TokenType.IDENTIFIER;
        };
        addToken(type, text);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /** Returns {@code true} if {@link #current} is past the end of {@link #source}. */
    private boolean isAtEnd()  { return current >= source.length(); }

    /**
     * Consumes and returns the current character, advancing {@link #current}.
     *
     * @return the character at the old {@link #current} position
     */
    private char    advance()  { return source.charAt(current++); }

    /**
     * Returns the current character without consuming it.
     *
     * @return the character at {@link #current}, or {@code '\0'} at end-of-file
     */
    private char    peek()     { return isAtEnd() ? '\0' : source.charAt(current); }

    /**
     * Conditionally advances if the current character equals {@code expected}.
     *
     * @param expected the character to match
     * @return {@code true} if the character matched and {@link #current} advanced
     */
    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    /**
     * Emits a token whose value is the raw source text from {@link #start} to
     * {@link #current}.
     *
     * @param type the token category
     */
    private void addToken(TokenType type) {
        addToken(type, source.substring(start, current));
    }

    /**
     * Emits a token with an explicit value (used when the canonical value
     * differs from or is more convenient than the raw source slice).
     *
     * @param type  the token category
     * @param value the token value
     */
    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, line, col()));
    }

    /** Returns {@code true} if {@code c} is an ASCII digit {@code 0–9}. */
    private boolean isDigit(char c)        { return c >= '0' && c <= '9'; }

    /** Returns {@code true} if {@code c} is a letter or underscore. */
    private boolean isAlpha(char c)        { return Character.isLetter(c) || c == '_'; }

    /** Returns {@code true} if {@code c} is a letter, digit, or underscore. */
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
}
