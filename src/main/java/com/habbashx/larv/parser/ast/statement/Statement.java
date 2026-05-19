package com.habbashx.larv.parser.ast.statement;

/**
 * Sealed root interface for all statement AST nodes in the Larv language.
 *
 * <h2>New permitted subtypes</h2>
 * <ul>
 *   <li>{@link TryCatchStatement} — {@code try { } catch (e) { } finally { }}</li>
 *   <li>{@link ThrowStatement}    — {@code throw expr}</li>
 *   <li>{@link SwitchStatement}   — {@code switch expr { case … default … }}</li>
 *   <li>{@link EnumStatement}     — {@code enum Name { A, B, C }}</li>
 * </ul>
 */
public sealed interface Statement permits
        AssignStatement,
        BlockStatement,
        BreakStatement,
        ClassStatement,
        CompoundAssignStatement,
        ConstStatement,
        ContinueStatement,
        DecrementStatement,
        EnumStatement,
        ExprStatement,
        ForStatement,
        ForeachStatement,
        FunctionStatement,
        IfStatement,
        ImportStatement,
        IncrementStatement,
        IndexAssignStatement,
        JavaBindStatement,
        LetStatement,
        ModuleStatement,
        PrintStatement,
        ReturnStatement,
        SetFieldStatement,
        SwitchStatement,
        ThrowStatement,
        TryCatchStatement,
        WhileStatement {

    /**
     * Returns the 1-based source line where this statement begins.
     *
     * @return the source line number, or {@code -1} if unknown
     */
    int line();
}
