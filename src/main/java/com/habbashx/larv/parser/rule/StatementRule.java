package com.habbashx.larv.parser.rule;


import com.habbashx.larv.parser.ast.statement.Statement;

/**
 * A single parse strategy for one keyword in the Larv grammar.
 *
 * Each implementation knows how to parse exactly one statement form
 * (e.g. {@code let}, {@code if}, {@code func}) after the keyword token
 * has already been consumed by the registry.
 *
 * Being a {@link FunctionalInterface} means every rule can be expressed
 * as a concise method reference (e.g. {@code this::parseLet}) with no
 * boilerplate wrapper class needed.
 */
@FunctionalInterface
public interface StatementRule {
    Statement parse();
}
