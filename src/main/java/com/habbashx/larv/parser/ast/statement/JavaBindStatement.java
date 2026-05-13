package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * Represents:
 *   include <alias> from "<FQCN>"
 *   include <alias> from "<FQCN>" involve { "<arg1>", "<arg2>", ... }
 *
 * {@code hasInvolve} distinguishes "no involve clause" from "involve {}".
 * This matters because involve {} with zero args means call the no-arg
 * constructor (instance binding), while no involve at all means static binding.
 */
public record JavaBindStatement(String alias, String className, List<String> constructorArgs, boolean hasInvolve, int line)
        implements Statement {

    /** Static binding — no involve clause at all. */
    public JavaBindStatement(String alias, String className) {
        this(alias, className, List.of(), false, -1);
    }
}