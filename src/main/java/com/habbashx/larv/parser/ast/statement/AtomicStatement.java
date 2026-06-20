package com.habbashx.larv.parser.ast.statement;

import com.habbashx.larv.parser.ast.expression.Expression;
public record AtomicStatement(String type, String name, Expression initializer, int line) implements Statement {
}