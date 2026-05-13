package com.habbashx.larv.parser.ast.expression;

/**
 * Sealed root interface for all expression AST nodes in the Larv language.
 *
 * <p>Every node that can appear on the right-hand side of an assignment,
 * as a function argument, inside a condition, etc. implements this interface.</p>
 *
 * <h2>Permitted subtypes</h2>
 * <ul>
 *   <li>{@link ArrayExpression}    — array literal {@code [1, 2, 3]}</li>
 *   <li>{@link AssignExpression}   — variable assignment {@code x = 5}</li>
 *   <li>{@link BinaryExpression}   — infix operation {@code a + b}</li>
 *   <li>{@link BooleanExpression}  — boolean literal {@code true} / {@code false}</li>
 *   <li>{@link CallExpression}     — function or method call {@code foo(arg)}</li>
 *   <li>{@link ClassRefExpression} — bare class name used as a value</li>
 *   <li>{@link GetExpression}      — field access {@code obj.field}</li>
 *   <li>{@link GroupExpression}    — parenthesised expression {@code (expr)}</li>
 *   <li>{@link IndexExpression}    — array index access {@code arr[i]}</li>
 *   <li>{@link JavaCallExpression} — Java FFI call {@code Alias.method(args)}</li>
 *   <li>{@link LiteralExpression}  — generic literal wrapper (e.g. {@code nil})</li>
 *   <li>{@link NewExpression}      — object construction {@code new Foo(args)}</li>
 *   <li>{@link NumberExpression}   — numeric literal {@code 42} or {@code 3.14}</li>
 *   <li>{@link SetExpression}      — field assignment {@code obj.field = val}</li>
 *   <li>{@link StringExpression}   — string literal {@code "hello"}</li>
 *   <li>{@link ThisExpression}     — {@code this} reference</li>
 *   <li>{@link UnaryExpression}    — unary operation {@code -expr}</li>
 *   <li>{@link VarExpression}      — variable reference {@code x}</li>
 * </ul>
 *
 * <p>The sealed hierarchy enables exhaustive pattern matching in the evaluator
 * without relying on {@code instanceof} chains.</p>
 */
public sealed interface Expression
        permits ArrayExpression, AssignExpression, BinaryExpression, BooleanExpression, CallExpression, ClassRefExpression, GetExpression, GroupExpression, IndexExpression, JavaCallExpression, LiteralExpression, NewExpression, NumberExpression, SetExpression, StringExpression, ThisExpression, UnaryExpression, VarExpression {
}
