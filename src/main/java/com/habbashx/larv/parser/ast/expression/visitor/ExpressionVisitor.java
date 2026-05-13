package com.habbashx.larv.parser.ast.expression.visitor;

import com.habbashx.larv.parser.ast.expression.*;

/**
 * Visitor interface for evaluating Larv expression AST nodes.
 *
 * <p>Implementations receive one method per expression type and return the
 * runtime value produced by evaluating that expression.  The concrete
 * implementation is {@link com.habbashx.larv.runtime.ExpressionEvaluator}.</p>
 *
 * <p>Note: not all {@link Expression}
 * subtypes are listed here — the evaluator uses a {@code switch} expression
 * for full exhaustiveness; this interface covers the most common visitable
 * nodes.</p>
 */
public interface ExpressionVisitor {

    /**
     * Evaluates a numeric literal.
     *
     * @param expr the number expression node
     * @return a {@code Double} equal to {@code expr.value()}
     */
    Object visitNumber(NumberExpression expr);

    /**
     * Evaluates a string literal.
     *
     * @param expr the string expression node
     * @return the {@code String} value of the literal
     */
    Object visitString(StringExpression expr);

    /**
     * Resolves a variable reference in the current environment.
     *
     * @param expr the variable expression node
     * @return the current value bound to the variable's name
     */
    Object visitVar(VarExpression expr);

    /**
     * Evaluates a binary infix operation.
     *
     * @param expr the binary expression node (operator + two operands)
     * @return the result of applying the operator to the two operand values
     */
    Object visitBinary(BinaryExpression expr);

    /**
     * Evaluates a function or method call.
     *
     * @param expr the call expression node
     * @return the return value of the called function or method
     */
    Object visitCall(CallExpression expr);

    /**
     * Instantiates a user-defined class.
     *
     * @param expr the new-expression node
     * @return the newly created {@link com.habbashx.larv.runtime.LarvObject}
     */
    Object visitNew(NewExpression expr);

    /**
     * Resolves the {@code this} reference to the current object.
     *
     * @param expr the this-expression node
     * @return the current {@link com.habbashx.larv.runtime.LarvObject} instance
     */
    Object visitThis(ThisExpression expr);

    /**
     * Reads a field from an object.
     *
     * @param expr the get-expression node (object + field name)
     * @return the value stored in the named field
     */
    Object visitGet(GetExpression expr);

    /**
     * Writes a value to an object field and returns {@code null}.
     *
     * @param expr the set-expression node (object + field name + new value)
     * @return {@code null} (side-effect only)
     */
    Object visitSet(SetExpression expr);
}
