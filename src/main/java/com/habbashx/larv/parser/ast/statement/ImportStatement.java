package com.habbashx.larv.parser.ast.statement;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * AST node for two kinds of import:
 *
 *   import math                        → stdlib import (library = "math", path = null)
 *   import "com.habbashx.Testing"      → file import   (library = null,  path = "com.habbashx.Testing")
 *
 * Exactly one of {@link #library} or {@link #path} is non-null.
 */
public record ImportStatement(String library, String path, int line) implements Statement {

    /** Stdlib import — e.g. import math */
    @Contract("_ -> new")
    public static @NotNull ImportStatement ofLibrary(String library) {
        return new ImportStatement(library, null, -1);
    }

    /** File import — e.g. import "com.habbashx.Testing" */
    @Contract("_ -> new")
    public static @NotNull ImportStatement ofPath(String path) {
        return new ImportStatement(null, path, -1);
    }

    public boolean isFileImport() { return path != null; }
}