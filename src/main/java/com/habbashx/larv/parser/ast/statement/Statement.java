package com.habbashx.larv.parser.ast.statement;

/**
 * Sealed root interface for all statement AST nodes in the Larv language.
 *
 * <p>Every construct that stands on its own as a line of code — variable
 * declarations, assignments, control flow, function/class definitions,
 * imports — implements this interface.</p>
 *
 * <p>The single required accessor {@link #line()} carries the source line
 * number so that runtime errors can always report exactly where a statement
 * started.  A value of {@code -1} means the line is unknown.</p>
 *
 * <h2>Permitted subtypes</h2>
 * <ul>
 *   <li>{@link LetStatement}          — {@code var x = expr}</li>
 *   <li>{@link ConstStatement}        — {@code const X = expr}</li>
 *   <li>{@link AssignStatement}       — {@code x = expr}</li>
 *   <li>{@link IncrementStatement}    — {@code x++}</li>
 *   <li>{@link DecrementStatement}    — {@code x--}</li>
 *   <li>{@link IndexAssignStatement}  — {@code arr[i] = expr}</li>
 *   <li>{@link PrintStatement}        — {@code print expr}</li>
 *   <li>{@link ExprStatement}         — a standalone expression</li>
 *   <li>{@link IfStatement}           — {@code if cond { } else { }}</li>
 *   <li>{@link WhileStatement}        — {@code while cond { }}</li>
 *   <li>{@link ForStatement}          — {@code for init; cond; step { }}</li>
 *   <li>{@link ForeachStatement}      — {@code for x in iterable { }}</li>
 *   <li>{@link BreakStatement}        — {@code break}</li>
 *   <li>{@link ContinueStatement}     — {@code continue}</li>
 *   <li>{@link ReturnStatement}       — {@code return expr}</li>
 *   <li>{@link FunctionStatement}     — {@code func name(params) { }}</li>
 *   <li>{@link ClassStatement}        — {@code class Name { }}</li>
 *   <li>{@link BlockStatement}        — a grouped list of statements</li>
 *   <li>{@link SetFieldStatement}     — {@code obj.field = expr}</li>
 *   <li>{@link JavaBindStatement}     — {@code include Alias from "FQCN"}</li>
 *   <li>{@link ImportStatement}       — {@code import lib} / {@code import "path"}</li>
 * </ul>
 */
public sealed interface Statement permits AssignStatement, BlockStatement, BreakStatement, ClassStatement, ConstStatement, ContinueStatement, DecrementStatement, ExprStatement, ForStatement, ForeachStatement, FunctionStatement, IfStatement, ImportStatement, IncrementStatement, IndexAssignStatement, JavaBindStatement, LetStatement, PrintStatement, ReturnStatement, SetFieldStatement, WhileStatement {

    /**
     * Returns the 1-based source line where this statement begins.
     *
     * @return the source line number, or {@code -1} if unknown
     */
    int line();
}
