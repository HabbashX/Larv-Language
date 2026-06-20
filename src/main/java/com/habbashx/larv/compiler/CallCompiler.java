package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.compiler.stdlib.LarvStdlibLoader;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.statement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.habbashx.larv.compiler.util.LarvCompilerUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Handles all call-expression routing and method-invocation bytecode emission.
 *
 * <p>The dispatch logic is: native builtins → top-level functions → stdlib →
 * module methods → Larv class methods (with accessor short-circuit) →
 * LarvRuntime dynamic dispatch.</p>
 *
 * <p>This class sits between {@link TypeInferenceCompiler} and
 * {@link ExpressionCompiler} in the inheritance chain so that
 * {@link ExpressionCompiler} can delegate {@code CallExpression} nodes here
 * without circular dependencies.</p>
 */
public abstract class CallCompiler extends TypeInferenceCompiler {

    protected CallCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    protected void compileCall(@NotNull CallExpression e) {
        Expression caller = e.caller();
        String callerDesc = caller instanceof VarExpression(String n) ? n : caller.getClass().getSimpleName();
        debugLog("    compileCall  caller=" + callerDesc + "  args=" + e.arguments().size());

        if (caller instanceof VarExpression(String name)) {
            if ("List".equals(name)) { debugLog("    → route: builtin List()"); callList(e, methodVisitor, typeRegistry); return; }
            if ("Map".equals(name))  { debugLog("    → route: builtin Map()");  callMap(e,  methodVisitor, typeRegistry); return; }
            if ("Set".equals(name))  { debugLog("    → route: builtin Set()");  callSet(e,  methodVisitor, typeRegistry); return; }
        }

        if (caller instanceof VarExpression(String name) && topLevelFunctions.containsKey(name)) {
            debugLog("    → route: top-level static call  name=" + name);
            compileStaticCall(name, e.arguments());
            return;
        }

        if (caller instanceof VarExpression(String name)) {
            NativeCall nc = NATIVE_FUNCTIONS.get(name);
            if (nc != null) { debugLog("    → route: native call  name=" + name); compileNativeCall(nc, e.arguments()); return; }

            if ("range".equals(name)) { debugLog("    → route: range()"); compileRangeCall(e.arguments()); return; }

            String[] stdlibEntry = LarvStdlibLoader.REGISTRY.get(name);
            if (stdlibEntry != null && importedLibs.contains(stdlibEntry[0])) {
                debugLog("    → route: stdlib call  name=" + name + "  lib=" + stdlibEntry[0]);
                compileStdlibCall(stdlibEntry[2], stdlibEntry[1], e.arguments());
                return;
            }
        }

        if (caller instanceof GetExpression(Expression object, String field)
                && object instanceof VarExpression(String libName)) {
            if (importedLibs.contains(libName)) {
                String qualKey = libName + "." + field;
                String[] entry = LarvStdlibLoader.REGISTRY.get(qualKey);
                if (entry == null) entry = LarvStdlibLoader.REGISTRY.get(field);
                if (entry != null && entry[0].equals(libName)) {
                    debugLog("    → route: qualified stdlib call  lib=" + libName + "  method=" + field);
                    compileStdlibCall(entry[2], entry[1], e.arguments());
                    return;
                }
            }
            if (moduleNames.containsKey(libName)) {
                debugLog("    → route: module method call  module=" + libName + "  method=" + field);
                compileModuleMethodCall(libName, field, e.arguments());
                return;
            }
        }

        if (caller instanceof GetExpression ge) {
            debugLog("    → route: method call  field=" + ge.field());
            compileMethodCall(ge, e.arguments());
            return;
        }

        if (caller instanceof VarExpression(String name) && currentClassStatement != null) {
            boolean isClassMethod = currentClassStatement.body().stream()
                    .anyMatch(s -> s instanceof FunctionStatement fs && fs.name().equals(name));
            if (isClassMethod) {
                debugLog("    → route: virtual call on this  name=" + name + "  class=" + currentClassStatement.name());
                methodVisitor.visitVarInsn(ALOAD, 0);
                for (Expression arg : e.arguments()) compileExpression(arg);
                String argDesc = JAVA_OBJECT.repeat(e.arguments().size());
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, currentClassStatement.name(), name,
                        "(" + argDesc + ")Ljava/lang/Object;", false);
                return;
            }
        }

        String callerName = caller instanceof VarExpression(String name) ? name : "unknown";
        debugLog("    → route: UNRESOLVED  caller=" + callerName + "  (will throw CompileException)");
        throw new CompileException("Undefined function or unhandled call: " + callerName, -1);
    }

    protected void compileStaticCall(String name, @NotNull List<Expression> args) {
        for (Expression arg : args) compileExpression(arg);
        String desc = "(" + JAVA_OBJECT.repeat(args.size()) + ")Ljava/lang/Object;";
        methodVisitor.visitMethodInsn(INVOKESTATIC, mainInternalName, name, desc, false);
    }

    protected void compileNativeCall(NativeCall nc, @NotNull List<Expression> args) {
        for (Expression arg : args) compileExpression(arg);
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, nc.method(), nc.descriptor(), false);
    }

    protected void compileRangeCall(@NotNull List<Expression> args) {
        for (Expression arg : args) compileExpression(arg);
        String desc = args.size() == 1
                ? "(Ljava/lang/Object;)Ljava/lang/Object;"
                : "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "range", desc, false);
    }

    protected void compileStdlibCall(String clazz, String method, @NotNull List<Expression> args) {
        for (Expression arg : args) compileExpression(arg);
        String desc = "(" + JAVA_OBJECT.repeat(args.size()) + ")Ljava/lang/Object;";
        methodVisitor.visitMethodInsn(INVOKESTATIC, clazz, method, desc, false);
    }

    protected void compileModuleMethodCall(String moduleName, String method, @NotNull List<Expression> args) {
        String modClass = moduleNames.get(moduleName);
        for (Expression arg : args) compileExpression(arg);
        String desc = "(" + JAVA_OBJECT.repeat(args.size()) + ")Ljava/lang/Object;";
        methodVisitor.visitMethodInsn(INVOKESTATIC, modClass, method, desc, false);
    }

    protected void compileMethodCall(@NotNull GetExpression ge, @NotNull List<Expression> args) {
        String objectType = evaluateExpressionType(ge.object());
        debugLog("    compileMethodCall  field=" + ge.field() + "  objectType=" + objectType + "  args=" + args.size());

        if (larvClasses.containsKey(objectType)) {
            ClassStatement cs = larvClasses.get(objectType);
            FunctionStatement method = cs.body().stream()
                    .filter(s -> s instanceof FunctionStatement fs && fs.name().equals(ge.field()))
                    .map(s -> (FunctionStatement) s)
                    .findFirst().orElse(null);

            if (method != null) {
                debugLog("    → INVOKEVIRTUAL  class=" + objectType + "  method=" + ge.field());
                compileExpression(ge.object());
                if (!castIsRedundant(ge.object(), objectType)) {
                    methodVisitor.visitTypeInsn(CHECKCAST, objectType);
                }
                for (Expression arg : args) compileExpression(arg);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, objectType, ge.field(),
                        buildMethodDescriptor(method), false);
                return;
            }

            String accessorResult = compileAccessorCall(ge, args, cs, objectType);
            if (accessorResult != null) {
                debugLog("    → accessor(" + accessorResult + ")  class=" + objectType + "  field=" + ge.field());
                return;
            }
        }

        Integer methodId = METHOD_IDS.get(ge.field());
        debugLog("    → LarvRuntime.invokeMethod  field=" + ge.field() + "  methodId=" + (methodId != null ? methodId : "null (string key)"));
        compileExpression(ge.object());
        if (methodId != null) {
            pushInt(methodId);
            pushObjectArray(args);
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "invokeMethod",
                    "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;", false);
        } else {
            methodVisitor.visitLdcInsn(ge.field());
            pushObjectArray(args);
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "invokeMethod",
                    "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        }
    }

    /**
     * Checks whether {@code ge.field()} matches a compiler-generated getter or
     * setter on {@code cs}.  If it does, emits the {@code INVOKEVIRTUAL} and
     * returns a non-null sentinel; returns {@code null} on no match.
     */
    protected @Nullable String compileAccessorCall(
            @NotNull GetExpression ge,
            @NotNull List<Expression> args,
            @NotNull ClassStatement cs,
            @NotNull String objectType) {

        String calledName = ge.field();

        for (Statement st : cs.body()) {
            String fieldName;
            String fieldType;
            boolean hasGetter;
            boolean hasSetter;

            if (st instanceof VarStatement vs) {
                fieldName = vs.name();
                fieldType = vs.type() != null ? vs.type() : "any";
                hasGetter = vs.hasGetter();
                hasSetter = vs.hasSetter();
            } else if (st instanceof ConstStatement constSt) {
                fieldName = constSt.name();
                fieldType = constSt.type() != null ? constSt.type() : "any";
                hasGetter = constSt.hasGetter();
                hasSetter = false;
            } else {
                continue;
            }

            String expectedGetter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            boolean primitive = isPrimitive(fieldType);
            String  fieldDesc = primitive ? primitiveDescriptor(fieldType) : getJvmDescriptor(fieldType);

            if (hasGetter && calledName.equals(expectedGetter) && args.isEmpty()) {
                compileExpression(ge.object());
                if (!castIsRedundant(ge.object(), objectType)) {
                    methodVisitor.visitTypeInsn(CHECKCAST, objectType);
                }
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, objectType, calledName, "()" + fieldDesc, false);
                if (primitive) emitBox(fieldType);
                return "getter";
            }

            if (hasSetter && calledName.equals(expectedSetter) && args.size() == 1) {
                compileExpression(ge.object());
                if (!castIsRedundant(ge.object(), objectType)) {
                    methodVisitor.visitTypeInsn(CHECKCAST, objectType);
                }
                compileExpression(args.getFirst());
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, objectType, calledName,
                        "(Ljava/lang/Object;)V", false);
                return "setter";
            }
        }
        return null;
    }

    /**
     * Returns {@code true} when {@code expr} is a call to a known Larv class
     * method whose compiled JVM descriptor ends with {@code )V} (void return),
     * or to a generated setter.  Used by the statement compiler to decide
     * whether to emit a POP after an expression statement.
     */
    @Contract("null -> false")
    protected boolean isVoidCallExpression(Expression expr) {
        if (!(expr instanceof CallExpression(Expression caller, List<Expression> arguments))) return false;
        if (!(caller instanceof GetExpression(Expression object, String calledName)))         return false;
        String objectType = evaluateExpressionType(object);
        if (!larvClasses.containsKey(objectType)) return false;
        ClassStatement cs = larvClasses.get(objectType);

        FunctionStatement method = cs.body().stream()
                .filter(s -> s instanceof FunctionStatement fs && fs.name().equals(calledName))
                .map(s -> (FunctionStatement) s)
                .findFirst().orElse(null);
        if (method != null) return buildMethodDescriptor(method).endsWith(")V");

        for (Statement st : cs.body()) {
            String fieldName = st instanceof VarStatement vs ? vs.name()
                    : st instanceof ConstStatement c ? c.name() : null;
            if (fieldName == null) continue;
            boolean hasSetter = st instanceof VarStatement vs2 && vs2.hasSetter();
            if (hasSetter) {
                String expectedSetter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                if (calledName.equals(expectedSetter) && arguments.size() == 1) return true;
            }
        }
        return false;
    }
    /**
     * Reconstructs the JVM method descriptor for a {@link FunctionStatement}
     * using the exact same logic as {@link ClassCompiler#compileMethodBody},
     * so that {@code INVOKEVIRTUAL} call sites always match the compiled
     * method signature.
     */
    protected @NotNull String buildMethodDescriptor(@NotNull FunctionStatement fn) {
        String declaredReturn = fn.returnType();
        boolean hasExplicitReturnType =
                declaredReturn != null &&
                        !declaredReturn.equals("void") &&
                        !declaredReturn.equals("any");

        String finalReturnType;
        if (hasExplicitReturnType) {
            finalReturnType = declaredReturn;
        } else if (containsReturnWithValue(fn.body())) {
            finalReturnType = "any";
        } else {
            finalReturnType = "void";
        }
        String jvmReturnDesc;
        if (finalReturnType.equals("void")) jvmReturnDesc = "V"; else jvmReturnDesc = "Ljava/lang/Object;";
        return "(" + JAVA_OBJECT.repeat(fn.params().size()) + ")" + jvmReturnDesc;
    }

    protected abstract boolean castIsRedundant(@NotNull Expression object, @NotNull String targetType);
}