package com.habbashx.larv.lexer;

/**
 * Exhaustive set of syntactic categories recognised by the Larv {@link Lexer}.
 * (Additions: TRY, CATCH, FINALLY, THROW, SWITCH, CASE, DEFAULT, ENUM, RAW_STRING,
 *             SYNC, CORE, OVERRIDE)
 */
public enum TokenType {

    NUMBER,
    STRING,
    RAW_STRING,
    IDENTIFIER,

    VAR,
    CONST,
    STRING_KEYWORD,
    PRINT,
    IF,
    ELSE,
    WHILE,
    FOR,
    IN,
    FUNC,
    RETURN,
    BREAK,
    CONTINUE,
    CLASS,
    NEW,
    THIS,
    INCLUDE,
    FROM,
    INVOLVE,
    IMPORT,
    MODULE,
    AS,
    NIL,
    TRUE,
    FALSE,

    TRY,
    CATCH,
    FINALLY,
    THROW,

    SWITCH,
    CASE,
    DEFAULT,

    ENUM,
    ATOMIC,
    VOLATILE,

    SYNC,
    CORE,
    OVERRIDE,

    GET,
    SET,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    PLUS_PLUS,
    MINUS_MINUS,

    EQUAL,
    PLUS_EQUAL,
    MINUS_EQUAL,
    STAR_EQUAL,
    SLASH_EQUAL,
    EQEQ,
    NOTEQ,
    LT,
    GT,
    LTE,
    GTE,

    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    COMMA,
    SEMICOLON,
    LBRACKET,
    RBRACKET,
    COLON,
    DOT,

    AND,
    OR,
    BANG,
    QUESTION,
    DEFER,
    ARROW,
    EOF
}
