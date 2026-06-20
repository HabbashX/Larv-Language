package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.compiler.stdlib.LarvStdlibLoader;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.statement.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.habbashx.larv.compiler.util.LarvCompilerUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Compiles every kind of {@link Expression} to JVM bytecode.
 *
 * <p>Each {@code compile*} method leaves exactly one value on the operand
 * stack (or, for void-context callers, relies on the statement layer to pop).
 * Call-expression routing logic that was previously inlined in
 * {@code LarvCompiler} has been moved to {@link CallCompiler}, which this
 * class extends.</p>
 */
public abstract class ExpressionCompiler extends CallCompiler {

    protected ExpressionCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    @Override
    public void compileExpression(@NotNull Expression expr) {
        if (debugMode) {
            debugLog("  compileExpression  type=" + expr.getClass().getSimpleName()
                    + describeExpr(expr));
        }
        try {
            switch (expr) {
                case NumberExpression e   -> compileNumber(e);
                case StringExpression e   -> methodVisitor.visitLdcInsn(e.value());
                case BooleanExpression e  -> pushBoolean(e.value());
                case LiteralExpression e  -> compileLiteral(e);
                case VarExpression e      -> compileVarLoad(e);
                case BinaryExpression e   -> compileBinary(e);
                case UnaryExpression e    -> compileUnary(e);
                case LogicalExpression e  -> compileLogical(e);
                case TernaryExpression e  -> compileTernary(e);
                case GroupExpression e    -> compileExpression(e.expression());
                case AssignExpression e   -> compileAssignExpr(e);
                case CallExpression e     -> compileCall(e);
                case GetExpression e      -> compileGet(e);
                case SetExpression e      -> compileSet(e);
                case NewExpression e      -> compileNew(e);
                case ArrayExpression e    -> compileArray(e);
                case IndexExpression e    -> compileIndex(e);
                case ThisExpression e     -> methodVisitor.visitVarInsn(ALOAD, 0);
                case ClassRefExpression e -> methodVisitor.visitLdcInsn(e.name());
                case JavaCallExpression e -> throw new CompileException(
                        "Java FFI (@java) is not supported in compiled mode.", -1);
                default -> throw new CompileException(
                        "Unknown expression type: " + expr.getClass().getSimpleName(), -1);
            }
        } catch (Exception e) {
            if (debugMode) {
                debugLog("  EXCEPTION in compileExpression"
                        + "  exprType=" + expr.getClass().getSimpleName()
                        + describeExpr(expr)
                        + "  exception=" + e.getClass().getSimpleName()
                        + "  message=" + e.getMessage());
                e.printStackTrace(System.err);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a short human-readable description of {@code expr} for debug logs.
     * Covers the most common cases; falls back to an empty string for complex nodes.
     */
    private static @NotNull String describeExpr(@NotNull Expression expr) {
        return switch (expr) {
            case NumberExpression e   -> "  value=" + e.value();
            case StringExpression e   -> "  value=\"" + e.value() + "\"";
            case BooleanExpression e  -> "  value=" + e.value();
            case VarExpression e      -> "  name=" + e.name();
            case BinaryExpression e   -> "  op=" + e.operator();
            case UnaryExpression e    -> "  op=" + e.operator();
            case LogicalExpression e  -> "  op=" + e.operator();
            case GetExpression e      -> "  field=" + e.field();
            case CallExpression e     -> "  caller=" + (e.caller() instanceof VarExpression(String name) ? name : e.caller().getClass().getSimpleName()) + "  args=" + e.arguments().size();
            case NewExpression e      -> "  class=" + e.className();
            default -> "";
        };
    }

    private void compileNumber(@NotNull NumberExpression e) {
        double val = e.value();
        if (val % 1 == 0) {
            int intVal = (int) val;
            if      (intVal >= -1 && intVal <= 5)                            methodVisitor.visitInsn(ICONST_0 + intVal);
            else if (intVal >= Byte.MIN_VALUE  && intVal <= Byte.MAX_VALUE)  methodVisitor.visitIntInsn(BIPUSH, intVal);
            else if (intVal >= Short.MIN_VALUE && intVal <= Short.MAX_VALUE) methodVisitor.visitIntInsn(SIPUSH, intVal);
            else                                                             methodVisitor.visitLdcInsn(intVal);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else {
            methodVisitor.visitLdcInsn(val);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }
    }

    private void compileLiteral(@NotNull LiteralExpression e) {
        Object val = e.value();
        switch (val) {
            case null    -> methodVisitor.visitInsn(ACONST_NULL);
            case Double d  -> pushDouble(d);
            case Boolean b -> pushBoolean(b);
            default        -> methodVisitor.visitLdcInsn(val.toString());
        }
    }

    protected void compileVarLoad(@NotNull VarExpression e) {
        int slot = locals.get(e.name());
        if (slot >= 0) {
            methodVisitor.visitVarInsn(ALOAD, slot);
            return;
        }

        if (currentClassStatement != null && hasField(currentClassStatement, e.name())) {
            String fieldType = getFieldType(currentClassStatement, e.name());
            String fieldDesc = isPrimitive(fieldType) ? primitiveDescriptor(fieldType) : getJvmDescriptor(fieldType);
            boolean isStatic = isConstField(currentClassStatement, e.name());
            if (isStatic) {
                methodVisitor.visitFieldInsn(GETSTATIC, currentClassStatement.name(), e.name(), fieldDesc);
            } else {
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitFieldInsn(GETFIELD, currentClassStatement.name(), e.name(), fieldDesc);
            }
            if (isPrimitive(fieldType)) emitBox(fieldType);
            return;
        }

        methodVisitor.visitLdcInsn(e.name());
    }

    private void compileAssignExpr(@NotNull AssignExpression e) {
        if (constLocals.contains(e.name())) {
            throw new CompileException("Cannot reassign constant '" + e.name() + "'", -1);
        }
        compileExpression(e.value());
        int slot = locals.get(e.name());
        if (slot < 0) slot = locals.define(e.name());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    private void compileBinary(@NotNull BinaryExpression e) {
        switch (e.operator()) {
            case "+" -> {
                compileExpression(e.left());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(e.right());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitInsn(DADD);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            case "-" -> emitNumericOp(e, DSUB);
            case "*" -> emitNumericOp(e, DMUL);
            case "/" -> {
                compileExpression(e.left());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(e.right());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "divide", "(DD)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            case "%" -> {
                compileExpression(e.left());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(e.right());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "modulo", "(DD)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            case "==" -> emitObjCompare(e, "equalEqual");
            case "!=" -> emitObjCompare(e, "notEqual");
            case "<"  -> emitObjCompare(e, "lessThan");
            case "<=" -> emitObjCompare(e, "lessEq");
            case ">"  -> emitObjCompare(e, "greaterThan");
            case ">=" -> emitObjCompare(e, "greaterEq");
            default   -> throw new CompileException("Unknown binary operator: " + e.operator(), -1);
        }
    }

    private void emitNumericOp(@NotNull BinaryExpression e, int opcode) {
        compileExpression(e.left());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
        compileExpression(e.right());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
        methodVisitor.visitInsn(opcode);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
    }

    private void emitObjCompare(@NotNull BinaryExpression e, String runtimeMethod) {
        compileExpression(e.left());
        compileExpression(e.right());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, runtimeMethod,
                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
    }

    private void compileUnary(@NotNull UnaryExpression e) {
        if ("-".equals(e.operator())) {
            compileExpression(e.right());
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
            methodVisitor.visitInsn(DNEG);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if ("!".equals(e.operator())) {
            compileExpression(e.right());
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(IXOR);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else {
            throw new CompileException("Unknown unary operator: " + e.operator(), -1);
        }
    }

    private void compileLogical(@NotNull LogicalExpression e) {
        org.objectweb.asm.Label shortCircuit = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end          = new org.objectweb.asm.Label();
        compileExpression(e.left());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
        boolean isAnd = e.operator().equals("&&") || e.operator().equals("and");
        methodVisitor.visitJumpInsn(isAnd ? IFEQ : IFNE, shortCircuit);
        methodVisitor.visitInsn(POP);
        compileExpression(e.right());
        methodVisitor.visitJumpInsn(GOTO, end);
        methodVisitor.visitLabel(shortCircuit);
        methodVisitor.visitLabel(end);
    }

    private void compileTernary(@NotNull TernaryExpression e) {
        org.objectweb.asm.Label falseL = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end    = new org.objectweb.asm.Label();
        compileExpression(e.condition());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
        methodVisitor.visitJumpInsn(IFEQ, falseL);
        compileExpression(e.thenBranch());
        methodVisitor.visitJumpInsn(GOTO, end);
        methodVisitor.visitLabel(falseL);
        compileExpression(e.elseBranch());
        methodVisitor.visitLabel(end);
    }

    private void compileNew(@NotNull NewExpression e) {
        methodVisitor.visitTypeInsn(NEW, e.className());
        methodVisitor.visitInsn(DUP);
        for (Expression arg : e.args()) compileExpression(arg);
        String ctorDesc = "(" + JAVA_OBJECT.repeat(e.args().size()) + ")V";
        methodVisitor.visitMethodInsn(INVOKESPECIAL, e.className(), "<init>", ctorDesc, false);
    }

    protected void compileGet(@NotNull GetExpression e) {
        String objectType = evaluateExpressionType(e.object());

        if (larvClasses.containsKey(objectType)) {
            ClassStatement cs      = larvClasses.get(objectType);
            String fieldType       = getFieldType(cs, e.field());
            boolean primitive      = isPrimitive(fieldType);
            String fieldDesc       = primitive ? primitiveDescriptor(fieldType) : JAVA_OBJECT;
            boolean isStatic       = isConstField(cs, e.field());

            if (isStatic) {
                methodVisitor.visitFieldInsn(GETSTATIC, objectType, e.field(), fieldDesc);
            } else {
                compileExpression(e.object());
                if (!castIsRedundant(e.object(), objectType)) {
                    methodVisitor.visitTypeInsn(CHECKCAST, objectType);
                }
                methodVisitor.visitFieldInsn(GETFIELD, objectType, e.field(), fieldDesc);
            }
            if (primitive) emitBox(fieldType);
            return;
        }

        compileExpression(e.object());
        methodVisitor.visitLdcInsn(e.field());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "getField",
                "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
    }

    protected void compileSet(@NotNull SetExpression e) {
        String objectType = evaluateExpressionType(e.object());

        if (larvClasses.containsKey(objectType)) {
            ClassStatement cs  = larvClasses.get(objectType);
            String fieldType   = getFieldType(cs, e.field());
            boolean primitive  = isPrimitive(fieldType);
            String fieldDesc   = primitive ? primitiveDescriptor(fieldType) : JAVA_OBJECT;

            compileExpression(e.object());
            if (!castIsRedundant(e.object(), objectType)) {
                methodVisitor.visitTypeInsn(CHECKCAST, objectType);
            }
            if (primitive) {
                compileExpressionPrimitive(e.value(), fieldType);
                emitBox(fieldType);
            } else {
                compileExpression(e.value());
            }
            methodVisitor.visitFieldInsn(PUTFIELD, objectType, e.field(), fieldDesc);
            methodVisitor.visitInsn(ACONST_NULL);
            return;
        }

        compileExpression(e.object());
        methodVisitor.visitLdcInsn(e.field());
        compileExpression(e.value());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "setField",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false);
        methodVisitor.visitInsn(ACONST_NULL);
    }

    private void compileArray(@NotNull ArrayExpression e) {
        pushInt(e.elements().size());
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < e.elements().size(); i++) {
            methodVisitor.visitInsn(DUP);
            pushInt(i);
            compileExpression(e.elements().get(i));
            methodVisitor.visitInsn(AASTORE);
        }
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList",
                "([Ljava/lang/Object;)Ljava/util/List;", false);
    }

    private void compileIndex(@NotNull IndexExpression e) {
        compileExpression(e.array());
        compileExpression(e.index());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "arrayGet",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
    }

    /**
     * Compiles {@code expr} targeting {@code targetType}, leaving a boxed
     * reference on the operand stack that is always compatible with
     * {@code Ljava/lang/Object;}.
     *
     * <p>Primitive results are autoboxed immediately so the JVM verifier never
     * sees a raw primitive where an {@code Object} is expected.</p>
     */
    public void compileExpressionAs(@NotNull Expression expr, @NotNull String targetType) {
        if (expr instanceof NumberExpression(double val)) {
            switch (targetType) {
                case "int" -> { pushInt((int) val); emitBox("int"); }
                case "long" -> { methodVisitor.visitLdcInsn((long) val); emitBox("long"); }
                case "float" -> { methodVisitor.visitLdcInsn((float) val); emitBox("float"); }
                case "double" -> { methodVisitor.visitLdcInsn(val); emitBox("double"); }
                default -> compileExpression(expr);
            }
        } else if (expr instanceof BooleanExpression(boolean val)) {
            methodVisitor.visitInsn(val ? ICONST_1 : ICONST_0);
            emitBox("boolean");
        } else if (expr instanceof StringExpression(String val)) {
            methodVisitor.visitLdcInsn(val);
        } else {
            compileExpression(expr);
        }
    }

    public void compileExpressionPrimitive(Expression expr, String targetType) {
        if (expr instanceof NumberExpression(double value)) {
            switch (targetType) {
                case "int"    -> methodVisitor.visitLdcInsn((int) value);
                case "float"  -> methodVisitor.visitLdcInsn((float) value);
                case "double" -> methodVisitor.visitLdcInsn(value);
                case "long"   -> methodVisitor.visitLdcInsn((long) value);
            }
        } else if (expr instanceof BooleanExpression(boolean value)) {
            methodVisitor.visitInsn(value ? ICONST_1 : ICONST_0);
        } else {
            compileExpression(expr);
            emitUnbox(targetType);
        }
    }

    /**
     * Returns {@code true} when a {@code CHECKCAST} to {@code targetType} is
     * unnecessary and would produce a spurious cast in decompiled output.
     */
    protected boolean castIsRedundant(@NotNull Expression object, @NotNull String targetType) {
        if (object instanceof NewExpression) return true;
        if (object instanceof VarExpression(String name)) {
            String knownType = localVarTypes.getOrDefault(name, "any");
            if (knownType.equals("any")) return false;
            return getInternalType(knownType).equals(targetType);
        }
        return false;
    }
}