package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.util.LocalVarTable;
import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.ast.statement.FunctionStatement.Parameter;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * Responsible for compiling Larv class and function declarations into
 * separate JVM class files and method bodies.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Emitting fields, static initialisers, and constructors for Larv classes.</li>
 *   <li>Compiling each method body ({@link #compileMethodBody}).</li>
 *   <li>Generating compiler-synthesised getter and setter methods.</li>
 *   <li>Compiling top-level functions as static methods on the main class.</li>
 * </ul>
 * </p>
 */
public abstract class ClassCompiler extends StatementCompiler {

    protected ClassCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    /**
     * Compiles a complete Larv class declaration to a separate JVM class file
     * and appends it to {@link #output}.
     */
    protected void compileLarvClass(@NotNull ClassStatement classStmt) {
        validateInterfaceCompliance(classStmt);
        this.currentClassStatement = classStmt;
        String internalClassName = classStmt.name();
        String superInternalName = classStmt.superclassName() != null
                ? classStmt.superclassName()
                : LARV_OBJ;
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(V21, ACC_PUBLIC | ACC_SUPER, internalClassName, null, superInternalName, null);

            for (Statement stmt : classStmt.body()) {
                if (stmt instanceof ConstStatement constStmt) {
                    String constDesc = isPrimitive(constStmt.type())
                            ? primitiveDescriptor(constStmt.type())
                            : getJvmDescriptor(constStmt.type());
                    cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                            constStmt.name(), constDesc, null, null).visitEnd();
                } else if (stmt instanceof VarStatement varStmt) {
                    int flags = ACC_PUBLIC;
                    if (varStmt.isVolatile()) flags |= ACC_VOLATILE;
                    String varDesc = isPrimitive(varStmt.type())
                            ? primitiveDescriptor(varStmt.type())
                            : getJvmDescriptor(varStmt.type());
                    cw.visitField(flags, varStmt.name(), varDesc, null, null).visitEnd();
                }
            }

            boolean hasConsts = classStmt.body().stream()
                    .anyMatch(s -> s instanceof ConstStatement cs && cs.value() != null);
            if (hasConsts) {
                MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.visitCode();
                MethodVisitor savedMv = this.methodVisitor;
                this.methodVisitor = clinit;
                for (Statement stmt : classStmt.body()) {
                    if (stmt instanceof ConstStatement constStmt && constStmt.value() != null) {
                        String constType = constStmt.type();
                        String constDesc = isPrimitive(constType)
                                ? primitiveDescriptor(constType)
                                : getJvmDescriptor(constType);
                        if (isPrimitive(constType)) compileExpressionPrimitive(constStmt.value(), constType);
                        else                        compileExpression(constStmt.value());
                        clinit.visitFieldInsn(PUTSTATIC, internalClassName, constStmt.name(), constDesc);
                    }
                }
                clinit.visitInsn(RETURN);
                clinit.visitMaxs(0, 0);
                clinit.visitEnd();
                this.methodVisitor = savedMv;
            }

            FunctionStatement initFunc = classStmt.body().stream()
                    .filter(s -> s instanceof FunctionStatement fs && fs.name().equals("constructor"))
                    .map(s -> (FunctionStatement) s)
                    .findFirst().orElse(null);

            StringBuilder ctorDescBuilder = new StringBuilder("(");
            if (initFunc != null) ctorDescBuilder.append(JAVA_OBJECT.repeat(initFunc.params().size()));
            ctorDescBuilder.append(")V");
            String finalCtorDesc = ctorDescBuilder.toString();

            if (!finalCtorDesc.equals("()V")) {
                MethodVisitor noArgCtor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
                noArgCtor.visitCode();
                noArgCtor.visitVarInsn(ALOAD, 0);
                if (classStmt.superclassName() != null) {
                    noArgCtor.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
                    noArgCtor.visitVarInsn(ALOAD, 0);
                    noArgCtor.visitLdcInsn(internalClassName);
                    noArgCtor.visitMethodInsn(INVOKEVIRTUAL, LARV_OBJ, "setClassName", "(Ljava/lang/String;)V", false);
                } else {
                    noArgCtor.visitLdcInsn(internalClassName);
                    noArgCtor.visitMethodInsn(INVOKESPECIAL, LARV_OBJ, "<init>", "(Ljava/lang/String;)V", false);
                }
                noArgCtor.visitInsn(RETURN);
                noArgCtor.visitMaxs(0, 0);
                noArgCtor.visitEnd();
            }

            MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", finalCtorDesc, null, null);
            ctor.visitCode();

            MethodVisitor previousMv   = this.methodVisitor;
            LocalVarTable prevLocals   = this.locals;
            this.methodVisitor = ctor;
            this.locals        = new LocalVarTable(1);
            this.inConstructor = true;

            if (initFunc != null) {
                for (Parameter param : initFunc.params()) this.locals.define(param.name());
            }

            ctor.visitVarInsn(ALOAD, 0);
            if (classStmt.superclassName() != null) {
                ctor.visitMethodInsn(INVOKESPECIAL, superInternalName, "<init>", "()V", false);
                ctor.visitVarInsn(ALOAD, 0);
                ctor.visitLdcInsn(internalClassName);
                ctor.visitMethodInsn(INVOKEVIRTUAL, LARV_OBJ, "setClassName", "(Ljava/lang/String;)V", false);
            } else {
                ctor.visitLdcInsn(internalClassName);
                ctor.visitMethodInsn(INVOKESPECIAL, LARV_OBJ, "<init>", "(Ljava/lang/String;)V", false);
            }

            for (Statement stmt : classStmt.body()) {
                if (stmt instanceof VarStatement varStmt && varStmt.expression() != null) {
                    ctor.visitVarInsn(ALOAD, 0);
                    String varType = varStmt.type();
                    if (isPrimitive(varType)) compileExpressionPrimitive(varStmt.expression(), varType);
                    else                      compileExpressionAs(varStmt.expression(), varType);
                    String varDesc = isPrimitive(varType) ? primitiveDescriptor(varType) : JAVA_OBJECT;
                    ctor.visitFieldInsn(PUTFIELD, internalClassName, varStmt.name(), varDesc);
                }
            }

            if (initFunc != null) compileStatementsWithDefer(initFunc.body());

            ctor.visitInsn(RETURN);
            ctor.visitMaxs(0, 0);
            ctor.visitEnd();

            this.methodVisitor = previousMv;
            this.locals        = prevLocals;
            this.inConstructor = false;

            for (Statement stmt : classStmt.body()) {
                if (stmt instanceof FunctionStatement funcStmt && !funcStmt.name().equals("constructor")) {
                    if (funcStmt.isAsync()) {
                        compileAsyncFunction(internalClassName, funcStmt, cw, false);
                        continue;
                    }
                    int flags = ACC_PUBLIC;
                    if (funcStmt.isSync()) flags |= ACC_SYNCHRONIZED;
                    if (funcStmt.isCore()) flags |= ACC_FINAL;
                    compileMethodBody(internalClassName, funcStmt, cw, flags);
                }
            }

            for (Statement stmt : classStmt.body()) {
                if (stmt instanceof VarStatement varStmt) {
                    if (varStmt.hasGetter()) generateGetter(cw, internalClassName, varStmt.name());
                    if (varStmt.hasSetter()) generateSetter(cw, internalClassName, varStmt.name());
                } else if (stmt instanceof ConstStatement constStmt) {
                    if (constStmt.hasGetter()) generateGetter(cw, internalClassName, constStmt.name());
                }
            }

            cw.visitEnd();
            output.add(new CompiledClass(internalClassName, cw.toByteArray()));
        } finally {
            this.currentClassStatement = null;
        }
    }
    /**
     * Compiles a top-level Larv function as a {@code public static} method on
     * the main class.
     */
    protected void compileFunction(@NotNull FunctionStatement fn) {
        if (fn.isAsync()) {
            compileAsyncFunction(mainInternalName, fn, classWriter, true);
            return;
        }
        int flags = ACC_PUBLIC | ACC_STATIC;
        if (fn.isSync()) flags |= ACC_SYNCHRONIZED;
        compileMethodBody(mainInternalName, fn, classWriter, flags);
    }

    /**
     * Compiles an {@code async} function into:
     * <ol>
     *   <li>a public body method {@code <name>$async$body} that always returns
     *       {@code Object} (a {@code null} value is returned when the body has
     *       no explicit return), and</li>
     *   <li>a public wrapper method {@code <name>} that packs the arguments into
     *       an {@code Object[]}, hands them to {@link LarvCompilerRuntime#runAsync}
     *       and immediately returns the {@link java.util.concurrent.CompletableFuture}.
     *   </li>
     * </ol>
     * Callers then apply {@code await} to the future to obtain the result.
     */
    protected void compileAsyncFunction(@NotNull String ownerClass, @NotNull FunctionStatement fn,
                                        @NotNull ClassWriter cw, boolean isStatic) {
        compileAsyncBody(ownerClass, fn, cw, isStatic);

        int flags = ACC_PUBLIC | (isStatic ? ACC_STATIC : 0);
        String desc = "(" + JAVA_OBJECT.repeat(fn.params().size()) + ")Ljava/lang/Object;";
        MethodVisitor mv = cw.visitMethod(flags, fn.name(), desc, null, null);
        mv.visitCode();

        MethodVisitor prevMv     = this.methodVisitor;
        LocalVarTable prevLocals = this.locals;

        this.methodVisitor = mv;
        this.locals        = new LocalVarTable(isStatic ? 0 : 1);
        this.localVarTypes.clear();
        this.constLocals.clear();
        this.constLocalLabels.clear();
        this.varLocalLabels.clear();
        this.typeEnv.clear();
        this.typeEnv.push(new HashMap<>());

        for (Parameter p : fn.params()) {
            this.locals.define(p.name());
            this.localVarTypes.put(p.name(), "any");
        }

        mv.visitLdcInsn(Type.getObjectType(ownerClass));
        if (isStatic) mv.visitInsn(ACONST_NULL);
        else          mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(fn.name() + "$async$body");

        pushInt(mv, fn.params().size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < fn.params().size(); i++) {
            mv.visitInsn(DUP);
            pushInt(mv, i);
            mv.visitVarInsn(ALOAD, this.locals.get(fn.params().get(i).name()));
            mv.visitInsn(AASTORE);
        }
        int argsSlot = this.locals.define("$async$args");
        mv.visitVarInsn(ASTORE, argsSlot);
        mv.visitVarInsn(ALOAD, argsSlot);

        mv.visitMethodInsn(INVOKESTATIC, RUNTIME, "runAsync",
                "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        this.methodVisitor = prevMv;
        this.locals        = prevLocals;
    }

    /**
     * Compiles the body of an {@code async} function as a public method named
     * {@code <name>$async$body}, forcing a {@code Ljava/lang/Object;} return so
     * the value can be transported through the async executor.
     */
    private void compileAsyncBody(@NotNull String ownerClass, @NotNull FunctionStatement fn,
                                  @NotNull ClassWriter cw, boolean isStatic) {
        int flags = ACC_PUBLIC | (isStatic ? ACC_STATIC : 0);
        if (fn.isSync()) flags |= ACC_SYNCHRONIZED;
        String desc = "(" + JAVA_OBJECT.repeat(fn.params().size()) + ")Ljava/lang/Object;";
        MethodVisitor mv = cw.visitMethod(flags, fn.name() + "$async$body", desc, null, null);
        mv.visitCode();

        MethodVisitor prevMv = this.methodVisitor;
        LocalVarTable prevLocals = this.locals;
        String prevReturnType = this.currentReturnType;

        this.methodVisitor = mv;
        this.locals        = new LocalVarTable(isStatic ? 0 : 1);
        this.localVarTypes.clear();
        this.constLocals.clear();
        this.constLocalLabels.clear();
        this.varLocalLabels.clear();
        this.typeEnv.clear();
        this.typeEnv.push(new HashMap<>());
        this.currentReturnType = "java/lang/Object";

        for (Parameter p : fn.params()) {
            this.locals.define(p.name());
            this.defineLocalType(p.name(), p.type());
            this.localVarTypes.put(p.name(), "any");
        }

        try {
            compileStatementsWithDefer(fn.body());
        } catch (Exception e) {
            if (debugMode) {
                debugLog("EXCEPTION in compileAsyncBody  name=" + fn.name()
                        + "  line=" + fn.line()
                        + "  exception=" + e.getClass().getSimpleName()
                        + "  message=" + e.getMessage());
                e.printStackTrace(System.err);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);
        emitConstLocalVariables(mv, methodEnd);

        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        this.currentReturnType = prevReturnType;
        this.methodVisitor     = prevMv;
        this.locals            = prevLocals;
    }

    protected static void pushInt(@NotNull MethodVisitor mv, int value) {
        if      (value >= -1 && value <= 5)                 mv.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE  && value <= Byte.MAX_VALUE)  mv.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) mv.visitIntInsn(SIPUSH, value);
        else                                                 mv.visitLdcInsn(value);
    }

    /**
     * Verifies that {@code clazz} implements every method required by its
     * declared interfaces (transitively, including inherited interfaces).
     * The check accounts for methods inherited from ancestor classes.
     */
    private void validateInterfaceCompliance(@NotNull ClassStatement clazz) {
        if (clazz.interfaces().isEmpty()) return;
        Set<String> implemented = classMethodKeys(clazz);
        Set<String> required    = new HashSet<>();
        for (String iface : clazz.interfaces()) addInterfaceMethodKeys(iface, required);
        for (String key : required) {
            if (!implemented.contains(key)) {
                throw new CompileException("Class '" + clazz.name() + "' does not implement method '" + key + "' required by its interface(s)", -1);
            }
        }
    }

    /** Collects {@code name#arity} keys for every method in {@code clazz} and its ancestor classes. */
    private @NotNull Set<String> classMethodKeys(@NotNull ClassStatement clazz) {
        Set<String> keys = new HashSet<>();
        for (ClassStatement cur = clazz; cur != null;
             cur = cur.superclassName() != null ? larvClasses.get(cur.superclassName()) : null) {
            for (Statement s : cur.body()) {
                if (s instanceof FunctionStatement fn) keys.add(fn.name() + "#" + fn.params().size());
            }
        }
        return keys;
    }

    /** Collects {@code name#arity} keys required by {@code iface} and its parent interfaces. */
    private void addInterfaceMethodKeys(String iface, @NotNull Set<String> out) {
        InterfaceStatement decl = larvInterfaces.get(iface);
        if (decl == null) throw new CompileException("Undefined interface '" + iface + "'", -1);
        if (decl.superinterfaceName() != null) addInterfaceMethodKeys(decl.superinterfaceName(), out);
        for (FunctionStatement m : decl.methods()) out.add(m.name() + "#" + m.params().size());
    }

    /**
     * Compiles the body of {@code fn} into {@code cw} with the given access
     * flags, correctly determining the JVM return descriptor from the declared
     * or inferred return type.
     */
    protected void compileMethodBody(String ownerClass, @NotNull FunctionStatement fn,
                                     @NotNull ClassWriter cw, int accessFlags) {
        boolean hasReturn      = containsReturn(fn.body());
        boolean hasReturnValue = containsReturnWithValue(fn.body());

        String declaredReturn = fn.returnType();
        boolean hasExplicitReturnType = declaredReturn != null
                && !declaredReturn.equals("void")
                && !declaredReturn.equals("any");

        String finalReturnType;
        if      (hasExplicitReturnType) finalReturnType = declaredReturn;
        else if (hasReturnValue)        finalReturnType = "any";
        else                            finalReturnType = "void";

        boolean isVoid    = finalReturnType.equals("void");
        boolean isDynamic = finalReturnType.equals("any");

        String jvmReturnDesc;
        String internalReturnType;
        if (isVoid) {
            jvmReturnDesc      = "V";
            internalReturnType = "void";
            currentReturnType  = "V";
        } else if (isDynamic) {
            jvmReturnDesc      = "Ljava/lang/Object;";
            internalReturnType = "java/lang/Object";
            currentReturnType  = "java/lang/Object";
        } else {
            jvmReturnDesc      = "Ljava/lang/Object;";
            internalReturnType = getInternalType(finalReturnType);
            currentReturnType  = "java/lang/Object";
        }

        String desc = "(" + JAVA_OBJECT.repeat(fn.params().size()) + ")" + jvmReturnDesc;

        MethodVisitor mv = cw.visitMethod(accessFlags, fn.name(), desc, null, null);
        mv.visitCode();

        debugLog("begin compileFunction  name=" + fn.name()
                + "  params=" + fn.params().size()
                + "  returnType=" + finalReturnType
                + "  line=" + fn.line());

        MethodVisitor prevMv     = this.methodVisitor;
        LocalVarTable prevLocals = this.locals;

        this.methodVisitor = mv;
        int startSlot = (accessFlags & ACC_STATIC) != 0 ? 0 : 1;
        this.locals    = new LocalVarTable(startSlot);
        this.localVarTypes.clear();
        this.constLocals.clear();
        this.constLocalLabels.clear();
        this.varLocalLabels.clear();
        this.typeEnv.clear();
        this.typeEnv.push(new HashMap<>());

        for (Parameter p : fn.params()) {
            this.locals.define(p.name());
            this.defineLocalType(p.name(), p.type());
            this.localVarTypes.put(p.name(), "any");
        }

        try {
            compileStatementsWithDefer(fn.body());
        } catch (Exception e) {
            if (debugMode) {
                debugLog("EXCEPTION in compileFunction  name=" + fn.name()
                        + "  line=" + fn.line()
                        + "  exception=" + e.getClass().getSimpleName()
                        + "  message=" + e.getMessage());
                e.printStackTrace(System.err);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }

        Label methodEnd = new Label();
        mv.visitLabel(methodEnd);
        emitConstLocalVariables(mv, methodEnd);

        debugLog("end compileFunction  name=" + fn.name());

        if (isVoid) {
            mv.visitInsn(RETURN);
        } else {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        this.methodVisitor = prevMv;
        this.locals        = prevLocals;
    }

    protected void generateGetter(@NotNull ClassWriter cw, String ownerClass,
                                  @NotNull String fieldName) {
        String fieldType  = getFieldType(currentClassStatement, fieldName);
        boolean primitive = isPrimitive(fieldType);
        boolean isStatic  = isConstField(currentClassStatement, fieldName);
        String fieldDesc  = primitive ? primitiveDescriptor(fieldType) : getJvmDescriptor(fieldType);
        int    returnOp   = areturnOpcode(fieldType);
        String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        int methodFlags = ACC_PUBLIC | (isStatic ? ACC_STATIC : 0);
        MethodVisitor mv = cw.visitMethod(methodFlags, methodName, "()" + fieldDesc, null, null);
        mv.visitCode();

        if (isStatic) {
            mv.visitFieldInsn(GETSTATIC, ownerClass, fieldName, fieldDesc);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, ownerClass, fieldName, fieldDesc);
        }
        mv.visitInsn(returnOp);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    protected void generateSetter(@NotNull ClassWriter cw, String ownerClass,
                                  @NotNull String fieldName) {
        String fieldType  = getFieldType(currentClassStatement, fieldName);
        boolean primitive = isPrimitive(fieldType);
        String fieldDesc  = primitive ? primitiveDescriptor(fieldType) : getJvmDescriptor(fieldType);
        String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName,
                "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);

        if (primitive) {
            switch (fieldType) {
                case "int"     -> { mv.visitTypeInsn(CHECKCAST, "java/lang/Integer"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue",     "()I", false); }
                case "long"    -> { mv.visitTypeInsn(CHECKCAST, "java/lang/Long");    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long",    "longValue",    "()J", false); }
                case "float"   -> { mv.visitTypeInsn(CHECKCAST, "java/lang/Float");   mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float",   "floatValue",   "()F", false); }
                case "double"  -> { mv.visitTypeInsn(CHECKCAST, "java/lang/Double");  mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double",  "doubleValue",  "()D", false); }
                case "boolean" -> { mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false); }
            }
        } else if (!fieldType.equals("any")) {
            mv.visitTypeInsn(CHECKCAST, getInternalType(fieldType));
        }

        mv.visitFieldInsn(PUTFIELD, ownerClass, fieldName, fieldDesc);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }
}