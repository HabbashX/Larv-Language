package com.habbashx.larv.parser;

/**
 * Named precedence levels for the Pratt expression parser.
 *
 * <p>Higher values bind more tightly. The parser climbs from the lowest level
 * ({@link #NONE}) up to the highest ({@link #POSTFIX}) by passing the current
 * minimum precedence into {@link ExpressionParser#parse(int)}.</p>
 *
 * <pre>
 * Level   Name        Operators
 * ──────────────────────────────────────
 *   0     NONE        (floor — no operator)
 *   1     ASSIGNMENT  =
 *   2     COALESCE    ??
 *   3     TERNARY     ? ,
 *   4     POSTFIX     () . [] ?.  (call, get, index, safe-get)
 * </pre>
 */
public final class Precedence {

    private Precedence() {}

    public static final int NONE       = 0;
    public static final int ASSIGNMENT = 1;
    public static final int COALESCE   = 2;  // ?? (binds loosest after `=`, like C#)
    public static final int TERNARY    = 3;  // ? ,
    public static final int LOGICAL_OR = 4;  // ||
    public static final int LOGICAL_AND= 5;  // &&
    public static final int EQUALITY   = 6;  // == !=
    public static final int COMPARISON = 7;  // < > <= >=
    public static final int TERM       = 8;  // + -
    public static final int FACTOR     = 9;  // * /
    public static final int UNARY      = 10; // - !
    public static final int POSTFIX    = 11; // () . [] ?.

}
