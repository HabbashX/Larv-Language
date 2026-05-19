package com.habbashx.larv.parser.ast.statement;

import java.util.List;

/**
 * AST node for an {@code enum} declaration.
 *
 * <p>Syntax:</p>
 * <pre>
 *   enum Direction { NORTH, SOUTH, EAST, WEST }
 * </pre>
 *
 * <p>At runtime the enum is stored as a {@link com.habbashx.larv.runtime.LarvObject}
 * in the current environment under {@link #name()}.  Each variant becomes a
 * field on that object holding its ordinal index (0, 1, 2, …).  A special
 * {@code "__enum__"} field holds the enum's own name for display purposes.</p>
 *
 * <p>Variants are accessed with dot notation:</p>
 * <pre>
 *   enum Color { RED, GREEN, BLUE }
 *   print(Color.RED)    // 0
 *   print(Color.GREEN)  // 1
 * </pre>
 *
 * @param name     the enum type name (e.g. {@code "Direction"})
 * @param variants the ordered list of variant identifiers
 * @param line     the 1-based source line of the {@code enum} keyword
 */
public record EnumStatement(String name, List<String> variants, int line) implements Statement {}