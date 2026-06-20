package com.habbashx.larv.compiler.util;

import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.statement.BlockStatement;
import com.habbashx.larv.parser.ast.statement.ConstStatement;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.parser.ast.statement.VarStatement;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

public class LarvCompilerTypeChecker {

    private final Map<String, String> environment = new HashMap<>();

    public void check(@NotNull Statement statement) {
        switch (statement) {
            case VarStatement varStmt -> checkVar(varStmt);
            case ConstStatement constStmt -> checkConst(constStmt);
            case BlockStatement block -> block.statements().forEach(this::check);
            default -> {}
        }
    }

    private void checkVar(@NotNull VarStatement stmt) {
        if (stmt.expression() != null) {
            String expressionType = evaluateExpressionType(stmt.expression());

            if (!stmt.type().equals("any") && !stmt.type().equals(expressionType)) {
                throw new RuntimeException(
                        "Type Error on line " + stmt.line() +
                                ": Cannot assign value of type '" + expressionType +
                                "' to variable '" + stmt.name() + "' of type '" + stmt.type() + "'"
                );
            }
        }
        environment.put(stmt.name(), stmt.type());
    }

    private void checkConst(@NotNull ConstStatement stmt) {
        String expressionType = evaluateExpressionType(stmt.value());

        if (!stmt.type().equals("any") && !stmt.type().equals(expressionType)) {
            throw new RuntimeException(
                    "Type Error on line " + stmt.line() +
                            ": Cannot assign value of type '" + expressionType +
                            "' to constant '" + stmt.name() + "' of type '" + stmt.type() + "'"
            );
        }

        environment.put(stmt.name(), stmt.type());
    }

    private String evaluateExpressionType(@NotNull Expression expr) {
        return switch (expr) {
            case StringExpression e  -> "string";
            case NumberExpression e  -> "double";
            case BooleanExpression e -> "bool";

            case LiteralExpression e -> {
                Object val = e.value();
                if (val instanceof Double)  yield "double";
                if (val instanceof Boolean) yield "bool";
                if (val == null)            yield "any";
                yield "string";
            }

            case VarExpression e -> {
                if (!environment.containsKey(e.name())) {
                    throw new RuntimeException("Type Error: Unknown variable '" + e.name() + "'");
                }
                yield environment.get(e.name());
            }

            case ArrayExpression e -> "List";

            case NewExpression e -> e.className();

            case CallExpression e -> {
                if (e.caller() instanceof VarExpression(String calleeName)) {
                    if ("Map".equals(calleeName))  yield "Map";
                    if ("List".equals(calleeName)) yield "List";
                    if ("Set".equals(calleeName))  yield "Set";
                }
                yield "any";
            }

            default -> "any";
        };
    }
}
