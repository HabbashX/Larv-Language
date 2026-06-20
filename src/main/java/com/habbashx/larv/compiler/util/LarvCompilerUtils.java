package com.habbashx.larv.compiler.util;

import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.parser.ast.expression.CallExpression;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.ast.expression.VarExpression;
import com.habbashx.larv.parser.ast.statement.ClassStatement;
import com.habbashx.larv.parser.ast.statement.ConstStatement;
import com.habbashx.larv.parser.ast.statement.Statement;
import com.habbashx.larv.parser.ast.statement.VarStatement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Map;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;


public final class LarvCompilerUtils {


    @Contract(value = "-> fail" , pure = true)
    private LarvCompilerUtils() {
        throw new RuntimeException("you cannot use LarvCompilerUtils");
    }

    public static boolean hasField(@NotNull ClassStatement cs, String fieldName) {
        for (Statement stmt : cs.body()) {
            if (stmt instanceof VarStatement v && v.name().equals(fieldName)) return true;
            if (stmt instanceof ConstStatement c && c.name().equals(fieldName)) return true;
        }
        return false;
    }

    /**
     * Resolves a type argument expression to its Java Class at runtime.
     * Supports plain types (VarExpression) AND nested collections:
     *   List(string), Map(string, int), Set(bool), etc.
     *
     * Emits bytecode that leaves a java.lang.Class on the stack.
     */
    private static void emitTypeClass(
            @NotNull Expression typeExpr,
            @NotNull MethodVisitor methodVisitor,
            @NotNull Map<String, String> typeRegistry) {

        if (typeExpr instanceof VarExpression(String typeName)) {
            // Plain type: string, int, bool, or a user class
            String javaClass = resolveBuiltinClass(typeName);
            if (javaClass != null) {
                // Primitive-wrapper or known built-in: use ldc class literal
                methodVisitor.visitLdcInsn(Type.getType(javaClass));
            } else {
                String fullPath = typeRegistry.getOrDefault(typeName, typeName).replace('.', '/');
                methodVisitor.visitLdcInsn(Type.getObjectType(fullPath));
            }
            return;
        }

        if (typeExpr instanceof CallExpression ce
                && ce.caller() instanceof VarExpression(String collName)) {

            switch (collName) {
                case "List" -> {
                    callList(ce, methodVisitor, typeRegistry);
                    methodVisitor.visitMethodInsn(
                            org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                            "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                }
                case "Map" -> {
                    callMap(ce, methodVisitor, typeRegistry);
                    methodVisitor.visitMethodInsn(
                            org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                            "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                }
                case "Set" -> {
                    callSet(ce, methodVisitor, typeRegistry);
                    methodVisitor.visitMethodInsn(
                            org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                            "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                }
                default -> {
                    String fullPath = typeRegistry.getOrDefault(collName, collName).replace('.', '/');
                    methodVisitor.visitLdcInsn(Type.getObjectType(fullPath));
                }
            }
            return;
        }

        methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
    }

    /**
     * Maps Larv built-in type names to their Java descriptor strings.
     * Returns null for unknown (user-defined) types.
     */
    @Contract(pure = true)
    private static @Nullable String resolveBuiltinClass(@NotNull String larvType) {
        return switch (larvType) {
            case "string" -> "Ljava/lang/String;";
            case "int"    -> "Ljava/lang/Integer;";
            case "double" -> "Ljava/lang/Double;";
            case "float"  -> "Ljava/lang/Float;";
            case "long"   -> "Ljava/lang/Long;";
            case "bool"   -> "Ljava/lang/Boolean;";
            case "any"    -> "Ljava/lang/Object;";
            default       -> null;
        };
    }

    public static void callList(@NotNull CallExpression e, MethodVisitor methodVisitor, Map<String,String> typeRegistry) {
        if (e.arguments().size() == 1) {
            emitTypeClass(e.arguments().getFirst(), methodVisitor, typeRegistry);
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/habbashx/larv/compiler/runtime/LarvRuntime",
                    "createTypedList",
                    "(Ljava/lang/Class;)Ljava/util/List;",
                    false);
        } else {
            methodVisitor.visitLdcInsn(Type.getType(Object.class));
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/habbashx/larv/compiler/runtime/LarvRuntime",
                    "createTypedList",
                    "(Ljava/lang/Class;)Ljava/util/List;",
                    false);
        }
    }

    public static void callMap(@NotNull CallExpression e, MethodVisitor methodVisitor, Map<String,String> typeRegistry) {
        if (e.arguments().size() != 2) {
            throw new CompileException("Map requires 2 type arguments: Map(KeyType, ValueType)", -1);
        }

        emitTypeClass(e.arguments().get(0), methodVisitor, typeRegistry);
        emitTypeClass(e.arguments().get(1), methodVisitor, typeRegistry);

        methodVisitor.visitMethodInsn(
                INVOKESTATIC,
                "com/habbashx/larv/compiler/runtime/LarvRuntime",
                "createTypedMap",
                "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/Map;",
                false
        );
    }

    public static void callSet(@NotNull CallExpression e , MethodVisitor methodVisitor , Map<String,String> typeRegistry) {
        if (e.arguments().size() == 1) {
            emitTypeClass(e.arguments().getFirst(), methodVisitor, typeRegistry);
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/habbashx/larv/compiler/runtime/LarvRuntime",
                    "createTypedSet",
                    "(Ljava/lang/Class;)Ljava/util/Set;",
                    false);
        } else {
            methodVisitor.visitLdcInsn(Type.getType(Object.class));
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/habbashx/larv/compiler/runtime/LarvRuntime",
                    "createTypedSet",
                    "(Ljava/lang/Class;)Ljava/util/Set;",
                    false);
        }
    }
}
