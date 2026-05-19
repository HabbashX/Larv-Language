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
 *   2     EQUALITY    == !=
 *   3     COMPARISON  < > <= >=
 *   4     TERM        + -
 *   5     POSTFIX     () . []   (call, get, index)
 * </pre>
 */
public final class Precedence {

    private Precedence() {}

    public static final int NONE       = 0;
    public static final int ASSIGNMENT = 1;
    public static final int TERNARY    = 2;  // ? ,
    public static final int LOGICAL_OR = 3;  // ||
    public static final int LOGICAL_AND= 4;  // &&
    public static final int EQUALITY   = 5;  // == !=
    public static final int COMPARISON = 6;  // < > <= >=
    public static final int TERM       = 7;  // + -
    public static final int FACTOR     = 8;  // * /
    public static final int UNARY      = 9;  // - !
    public static final int POSTFIX    = 10; // () . []

}
