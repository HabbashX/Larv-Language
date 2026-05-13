package com.habbashx.larv.lexer;

/**
 * Exhaustive set of syntactic categories recognised by the Larv {@link Lexer}.
 *
 * <p>Every token produced during lexing is tagged with exactly one of these
 * values, allowing the parser to make decisions without inspecting raw string
 * values.</p>
 *
 * <h2>Organisation</h2>
 * <ul>
 *   <li><b>Literals</b>  — {@link #NUMBER}, {@link #STRING}, {@link #IDENTIFIER},
 *       {@link #NIL}, {@link #TRUE}, {@link #FALSE}</li>
 *   <li><b>Keywords</b>  — {@link #VAR}, {@link #CONST}, {@link #PRINT}, {@link #IF},
 *       {@link #ELSE}, {@link #WHILE}, {@link #FOR}, {@link #FUNC}, {@link #RETURN},
 *       {@link #BREAK}, {@link #CONTINUE}, {@link #CLASS}, {@link #NEW}, {@link #THIS},
 *       {@link #INCLUDE}, {@link #FROM}, {@link #INVOLVE}, {@link #IMPORT}</li>
 *   <li><b>Arithmetic operators</b> — {@link #PLUS}, {@link #MINUS}, {@link #STAR},
 *       {@link #SLASH}, {@link #PLUS_PLUS}, {@link #MINUS_MINUS}</li>
 *   <li><b>Relational / equality operators</b> — {@link #EQUAL}, {@link #EQEQ},
 *       {@link #NOTEQ}, {@link #LT}, {@link #GT}, {@link #LTE}, {@link #GTE}</li>
 *   <li><b>Delimiters</b> — {@link #LPAREN}, {@link #RPAREN}, {@link #LBRACE},
 *       {@link #RBRACE}, {@link #LBRACKET}, {@link #RBRACKET}, {@link #COMMA},
 *       {@link #SEMICOLON}, {@link #COLON}, {@link #DOT}</li>
 *   <li><b>Sentinel</b>  — {@link #EOF}</li>
 * </ul>
 */
public enum TokenType {

    // ── Literals ──────────────────────────────────────────────────────────────

    /** A numeric literal, e.g. {@code 42} or {@code 3.14}. */
    NUMBER,

    /** A double-quoted string literal, e.g. {@code "hello"}. */
    STRING,

    /**
     * A user-defined name — variable, function, class, or parameter.
     * Also used for unrecognised words that aren't reserved keywords.
     */
    IDENTIFIER,

    // ── Keywords ──────────────────────────────────────────────────────────────

    /** {@code var} — mutable variable declaration. */
    VAR,

    /** {@code const} — immutable constant declaration. */
    CONST,

    /**
     * {@code string} — custom type keyword (reserved; not yet fully used
     * by the runtime but lexed as a distinct token).
     */
    STRING_KEYWORD,

    /** {@code print} — built-in print statement keyword. */
    PRINT,

    /** {@code if} — conditional branch keyword. */
    IF,

    /** {@code else} — alternate branch of an {@code if} statement. */
    ELSE,

    /** {@code while} — condition-controlled loop keyword. */
    WHILE,

    /** {@code for} — count-controlled loop keyword. */
    FOR,

    /** {@code func} — function declaration keyword. */
    FUNC,

    /** {@code return} — return a value from a function. */
    RETURN,

    // ── Arithmetic operators ──────────────────────────────────────────────────

    /** {@code +} — addition or string concatenation. */
    PLUS,

    /** {@code -} — subtraction or unary negation. */
    MINUS,

    /** {@code *} — multiplication. */
    STAR,

    /** {@code /} — division. */
    SLASH,

    /** {@code ++} — post-increment shorthand. */
    PLUS_PLUS,

    /** {@code --} — post-decrement shorthand. */
    MINUS_MINUS,

    // ── Relational / equality operators ───────────────────────────────────────

    /** {@code =} — assignment. */
    EQUAL,

    /** {@code ==} — equality comparison. */
    EQEQ,

    /** {@code !=} — inequality comparison. */
    NOTEQ,

    /** {@code <} — less-than comparison. */
    LT,

    /** {@code >} — greater-than comparison. */
    GT,

    /** {@code <=} — less-than-or-equal comparison. */
    LTE,

    /** {@code >=} — greater-than-or-equal comparison. */
    GTE,

    // ── Delimiters ────────────────────────────────────────────────────────────

    /** {@code (} — open parenthesis. */
    LPAREN,

    /** {@code )} — close parenthesis. */
    RPAREN,

    /** <code>{</code> — open brace (block or class body). */
    LBRACE,

    /** <code>}</code> — close brace. */
    RBRACE,

    /** {@code ,} — argument or element separator. */
    COMMA,

    /** {@code ;} — optional statement terminator. */
    SEMICOLON,

    /** {@code [} — open square bracket (array literal or index). */
    LBRACKET,

    /** {@code ]} — close square bracket. */
    RBRACKET,

    /** {@code :} — colon (reserved for future map/type syntax). */
    COLON,

    // ── Control flow ──────────────────────────────────────────────────────────

    /** {@code break} — exit the nearest enclosing loop. */
    BREAK,

    /** {@code continue} — skip to the next iteration of the nearest loop. */
    CONTINUE,

    // ── Object-oriented ───────────────────────────────────────────────────────

    /** {@code class} — class declaration keyword. */
    CLASS,

    // ── FFI keywords ─────────────────────────────────────────────────────────

    /**
     * {@code include} — binds a fully-qualified Java class to a short alias.
     * <p>Syntax: {@code include <Alias> from "<FQCN>"}</p>
     */
    INCLUDE,

    /**
     * {@code from} — specifies the fully-qualified Java class name in an
     * {@code include} statement.
     */
    FROM,

    /**
     * {@code involve} — supplies constructor arguments for an instance
     * binding in an {@code include} statement.
     * <p>Syntax: {@code include <Alias> from "<FQCN>" involve { "<arg1>", ... }}</p>
     */
    INVOLVE,

    /**
     * {@code import} — loads a built-in standard library or a Larv source file.
     * <p>Examples: {@code import math}, {@code import "com.example.MyFile"}</p>
     */
    IMPORT,

    // ── Literals (continued) ─────────────────────────────────────────────────

    /** {@code nil} — the null / absent value. */
    NIL,

    /** {@code true} — boolean true literal. */
    TRUE,

    /** {@code false} — boolean false literal. */
    FALSE,

    // ── Misc ─────────────────────────────────────────────────────────────────

    /** {@code .} — member-access operator. */
    DOT,

    /** {@code new} — object instantiation keyword. */
    NEW,

    /** {@code this} — reference to the current object instance. */
    THIS,

    /** End-of-file sentinel — always the last token in the stream. */
    EOF
}
