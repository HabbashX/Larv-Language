package com.habbashx.larv.parser.ast.statement.visitor;

import com.habbashx.larv.parser.ast.statement.*;

/**
 * Visitor interface for executing Larv statement AST nodes.
 *
 * <p>Each method corresponds to one statement form in the grammar.
 * The concrete implementation is
 * {@link com.habbashx.larv.runtime.StatementExecutor}, which uses a
 * class-keyed registry instead of direct dispatch but still fulfils
 * this contract.</p>
 *
 * <p>All methods return {@code void} — statements produce side effects,
 * not values.  Return values from {@code return} statements are propagated
 * via {@link com.habbashx.larv.signal.ReturnSignal}.</p>
 */
public interface StatementVisitor {

    /** Declares a mutable variable ({@code var name = expr}). */
    void visitLet(LetStatement st);

    /** Declares an immutable constant ({@code const NAME = expr}). */
    void visitConst(ConstStatement st);

    /** Updates an existing variable ({@code name = expr}). */
    void visitAssign(AssignStatement st);

    /** Evaluates an expression for its side effects only. */
    void visitExpr(ExprStatement st);

    /** Evaluates and prints an expression ({@code print expr}). */
    void visitPrint(PrintStatement st);

    /** Executes one of two branches depending on a condition. */
    void visitIf(IfStatement st);

    /** Runs a loop body as long as a condition is truthy. */
    void visitWhile(WhileStatement st);

    /** Runs a traditional {@code for} loop with init / condition / increment. */
    void visitFor(ForStatement st);

    /** Exits the nearest enclosing loop (throws {@link com.habbashx.larv.signal.BreakSignal}). */
    void visitBreak(BreakStatement st);

    /** Skips the rest of the current iteration (throws {@link com.habbashx.larv.signal.ContinueSignal}). */
    void visitContinue(ContinueStatement st);

    /** Returns a value from the current function (throws {@link com.habbashx.larv.signal.ReturnSignal}). */
    void visitReturn(ReturnStatement st);

    /** Registers a user-defined function in the execution context. */
    void visitFunction(FunctionStatement st);

    /** Registers a user-defined class in the execution context. */
    void visitClass(ClassStatement st);

    /** Binds a Java class or instance to a short alias for FFI use. */
    void visitJavaBind(JavaBindStatement st);
}
