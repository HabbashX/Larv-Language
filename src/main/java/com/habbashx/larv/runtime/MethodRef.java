package com.habbashx.larv.runtime;

import com.habbashx.larv.parser.ast.statement.FunctionStatement;

/**
 * A bound method reference — pairs a receiver object with a function
 * declaration so the method can be invoked later with the correct {@code this}.
 *
 * @param self the object instance the method is bound to
 * @param fn   the function statement that contains the method body
 */
public record MethodRef(LarvObject self , FunctionStatement fn) {
}
