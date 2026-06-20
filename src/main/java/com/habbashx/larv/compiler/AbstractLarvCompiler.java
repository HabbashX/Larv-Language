package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.runtime.LarvMethods;
import com.habbashx.larv.compiler.util.LocalVarTable;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.statement.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Base class for all Larv compiler components.
 *
 * <p>Holds all shared mutable state (class/method visitors, local variable
 * tables, scope stacks, type registries) and provides protected utility
 * methods used by every compiler sub-component.  Subclasses must never
 * store their own copies of this state; they always read and write through
 * the fields declared here.</p>
 */
public abstract class AbstractLarvCompiler {

    protected static final String RUNTIME    = "com/habbashx/larv/compiler/runtime/LarvCompilerRuntime";
    protected static final String LARV_OBJ   = "com/habbashx/larv/compiler/runtime/LarvObject";
    protected static final String LARV_EX    = "com/habbashx/larv/compiler/runtime/LarvRuntimeException";
    protected static final String JAVA_OBJECT = "Ljava/lang/Object;";

    protected static final String ATOMIC_INT       = "java/util/concurrent/atomic/AtomicInteger";
    protected static final String ATOMIC_LONG      = "java/util/concurrent/atomic/AtomicLong";
    protected static final String ATOMIC_BOOLEAN   = "java/util/concurrent/atomic/AtomicBoolean";
    protected static final String ATOMIC_REFERENCE = "java/util/concurrent/atomic/AtomicReference";

    /** Thin value-object that pairs a runtime method name with its JVM descriptor. */
    protected record NativeCall(String method, String descriptor) {}

    protected static final Map<String, NativeCall> NATIVE_FUNCTIONS = Map.of(
            "print",    new NativeCall("print",    "(Ljava/lang/Object;)Ljava/lang/Object;"),
            "printErr", new NativeCall("printErr", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            "len",      new NativeCall("len",      "(Ljava/lang/Object;)Ljava/lang/Object;"),
            "input",    new NativeCall("input",    "()Ljava/lang/Object;")
    );

    /**
     * Maps Larv method names to their integer IDs defined in {@link LarvMethods}.
     * Used by the fallback dispatch path in {@code compileMethodCall} to emit an
     * {@code int} method ID instead of a {@code String} name, matching the
     * {@code LarvRuntime.invokeMethod(Object, int, Object[])} signature.
     */
    protected static final Map<String, Integer> METHOD_IDS = buildMethodIds();

    @NotNull
    private static @Unmodifiable Map<String, Integer> buildMethodIds() {
        Map<String, Integer> m = new HashMap<>();
        m.put("add",         LarvMethods.ADD);
        m.put("get",         LarvMethods.GET);
        m.put("size",        LarvMethods.SIZE);
        m.put("length",      LarvMethods.LENGTH);
        m.put("remove",      LarvMethods.REMOVE);
        m.put("contains",    LarvMethods.CONTAINS);
        m.put("set",         LarvMethods.SET);
        m.put("clear",       LarvMethods.CLEAR);
        m.put("isEmpty",     LarvMethods.IS_EMPTY);
        m.put("join",        LarvMethods.JOIN);
        m.put("push",        LarvMethods.PUSH);
        m.put("pop",         LarvMethods.POP);
        m.put("reverse",     LarvMethods.REVERSE);
        m.put("upper",       LarvMethods.UPPER);
        m.put("lower",       LarvMethods.LOWER);
        m.put("trim",        LarvMethods.TRIM);
        m.put("startsWith",  LarvMethods.STARTS_WITH);
        m.put("endsWith",    LarvMethods.ENDS_WITH);
        m.put("containsStr", LarvMethods.CONTAINS_STR);
        m.put("replace",     LarvMethods.REPLACE);
        m.put("split",       LarvMethods.SPLIT);
        m.put("indexOf",     LarvMethods.INDEX_OF);
        m.put("substring",   LarvMethods.SUBSTRING);
        m.put("charAt",      LarvMethods.CHAR_AT);
        return Map.copyOf(m);
    }

    /** The dot-separated name of the class being compiled (e.g. {@code com.example.Main}). */
    protected final String mainClassName;
    /** The JVM internal name of the main class (slashes instead of dots). */
    protected final String mainInternalName;

    /** Accumulates every class produced during compilation. */
    protected final List<CompiledClass> output = new ArrayList<>();

    /** Top-level function name → parameter count (populated in the pre-pass). */
    protected final Map<String, Integer> topLevelFunctions = new HashMap<>();

    /** All Larv class declarations keyed by name. */
    protected final Map<String, ClassStatement> larvClasses = new HashMap<>();

    /** Library names referenced by {@code import} statements. */
    protected final Set<String> importedLibs = new LinkedHashSet<>();

    /** Module alias → generated JVM internal name. */
    protected final Map<String, String> moduleNames = new LinkedHashMap<>();

    // Active ASM visitors — swapped in/out as methods are compiled
    protected ClassWriter classWriter;
    protected MethodVisitor methodVisitor;
    protected LocalVarTable locals;

    /** Stack of (continueLabel, breakLabel) pairs pushed for every loop. */
    protected final Deque<Label[]> loopStack = new ArrayDeque<>();

    /** The class currently being compiled (null when compiling top-level code). */
    protected ClassStatement currentClassStatement = null;

    /** {@code true} while compiling a constructor body. */
    protected boolean inConstructor = false;

    /**
     * When {@code true}, the compiler emits extra diagnostic output:
     * <ul>
     *   <li>Each statement is logged to {@code stderr} before compilation
     *       (type, line number, and a short description).</li>
     *   <li>{@link MethodVisitor#visitLineNumber} is called for every
     *       statement so that JVM stack traces reference source lines.</li>
     *   <li>Every function entry and exit is announced on {@code stderr}.</li>
     * </ul>
     * Enable via the {@code --debug} CLI flag in {@link LarvCompilerMain}.
     */
    protected boolean debugMode = false;

    /**
     * The JVM internal return type of the method currently being compiled
     * (e.g. {@code "java/lang/Object"}, {@code "V"}, or a class name).
     */
    protected String currentReturnType = "java/lang/Object";

    protected final Deque<Map<String, String>> typeEnv       = new ArrayDeque<>();
    protected final Map<String, String>        localVarTypes = new HashMap<>();

    /** Names of local slots declared with {@code const} — writing to them is a compile error. */
    protected final Set<String>         constLocals      = new HashSet<>();
    /** Maps const-local name → label at its definition point for LocalVariableTable entries. */
    protected final Map<String, Label>  constLocalLabels = new HashMap<>();
    /**
     * Maps var-local name → label at its definition point.
     * Lets decompilers see the correct declared type for each slot, which
     * eliminates spurious casts like {@code ((User)var2)} in decompiled output.
     */
    protected final Map<String, Label>  varLocalLabels   = new HashMap<>();

    /** Maps Larv type names to fully-qualified Java class names. */
    protected final Map<String, String> typeRegistry = new HashMap<>();

    protected AbstractLarvCompiler(@NotNull String mainClassName) {
        this.mainClassName    = mainClassName;
        this.mainInternalName = mainClassName.replace('.', '/');
        initDefaultTypeRegistry();
    }

    /**
     * Prints a debug message to {@code stderr} when {@link #debugMode} is on.
     * Messages are prefixed with {@code [DEBUG]} so they are easy to grep.
     *
     * @param msg the message to print
     */
    protected void debugLog(String msg) {
        if (debugMode) System.err.println("[DEBUG] " + msg);
    }

    /**
     * Emits a {@link MethodVisitor#visitLineNumber} instruction for the given
     * source line when {@link #debugMode} is on.  Line-number entries are
     * harmless metadata that make JVM stack traces point at the correct Larv
     * source line, which is invaluable when tracking down runtime crashes.
     *
     * @param line the 1-based source line
     */
    protected void emitLineNumber(int line) {
        if (debugMode && methodVisitor != null && line > 0) {
            Label l = new Label();
            methodVisitor.visitLabel(l);
            methodVisitor.visitLineNumber(line, l);
        }
    }

    protected void initDefaultTypeRegistry() {
        typeRegistry.put("int",    "java.lang.Integer");
        typeRegistry.put("string", "java.lang.String");
        typeRegistry.put("float",  "java.lang.Float");
        typeRegistry.put("double", "java.lang.Double");
        typeRegistry.put("long",   "java.lang.Long");
        typeRegistry.put("bool",   "java.lang.Boolean");
        typeRegistry.put("any",    "java.lang.Object");
    }

    /** Returns the JVM internal name for a Larv type (e.g. {@code "java/lang/String"}). */
    protected @NotNull String getInternalType(String larvType) {
        if (larvType == null || larvType.equals("void")) return "java/lang/Void";
        if (larvType.equals("any"))                       return "java/lang/Object";
        if (larvClasses.containsKey(larvType))            return larvType;
        String javaClass = typeRegistry.getOrDefault(larvType, "java/lang/Object");
        return javaClass.replace('.', '/');
    }

    /** Returns the JVM field descriptor for a Larv type (e.g. {@code "Ljava/lang/String;"}). */
    protected @NotNull String getJvmDescriptor(String larvType) {
        if (larvType == null || larvType.equals("any")) return "Ljava/lang/Object;";
        if (larvClasses.containsKey(larvType))          return "L" + larvType + ";";
        String javaClass = typeRegistry.getOrDefault(larvType, "java/lang/Object");
        return "L" + javaClass.replace('.', '/') + ";";
    }

    /** Maps a Larv type name to its JVM field descriptor for {@code LocalVariableTable} entries. */
    protected @NotNull String larvTypeToDescriptor(@NotNull String larvType) {
        return switch (larvType) {
            case "int"     -> "I";
            case "long"    -> "J";
            case "float"   -> "F";
            case "double"  -> "D";
            case "boolean" -> "Z";
            case "string"  -> "Ljava/lang/String;";
            case "any"     -> "Ljava/lang/Object;";
            default -> {
                if (larvClasses.containsKey(larvType)) yield "L" + larvType + ";";
                String javaClass = typeRegistry.get(larvType);
                if (javaClass != null)  yield "L" + javaClass.replace('.', '/') + ";";
                yield "Ljava/lang/Object;";
            }
        };
    }

    @Contract(pure = true)
    protected boolean isPrimitive(@NotNull String larvType) {
        return switch (larvType) {
            case "int", "boolean", "float", "double", "long" -> true;
            default -> false;
        };
    }

    @Contract(pure = true)
    protected int storeOpcode(@NotNull String larvType) {
        return switch (larvType) {
            case "int", "boolean" -> ISTORE;
            case "float"          -> FSTORE;
            case "double"         -> DSTORE;
            case "long"           -> LSTORE;
            default               -> ASTORE;
        };
    }

    @Contract(pure = true)
    protected int loadOpcode(@NotNull String larvType) {
        return switch (larvType) {
            case "int", "boolean" -> ILOAD;
            case "float"          -> FLOAD;
            case "double"         -> DLOAD;
            case "long"           -> LLOAD;
            default               -> ALOAD;
        };
    }

    @Contract(pure = true)
    protected @NotNull String primitiveDescriptor(@NotNull String larvType) {
        return switch (larvType) {
            case "int"     -> "I";
            case "boolean" -> "Z";
            case "float"   -> "F";
            case "double"  -> "D";
            case "long"    -> "J";
            default        -> "Ljava/lang/Object;";
        };
    }

    @Contract(pure = true)
    protected int areturnOpcode(@NotNull String larvType) {
        return switch (larvType) {
            case "int", "boolean" -> IRETURN;
            case "float"          -> FRETURN;
            case "double"         -> DRETURN;
            case "long"           -> LRETURN;
            default               -> ARETURN;
        };
    }

    protected void emitBox(@NotNull String larvType) {
        switch (larvType) {
            case "int"     -> methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",   false);
            case "float"   -> methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float",   "valueOf", "(F)Ljava/lang/Float;",     false);
            case "double"  -> methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double",  "valueOf", "(D)Ljava/lang/Double;",    false);
            case "long"    -> methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long",    "valueOf", "(J)Ljava/lang/Long;",      false);
            case "boolean" -> methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",   false);
        }
    }

    protected void emitUnbox(@NotNull String larvType) {
        switch (larvType) {
            case "int"     -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue",     "()I", false);
            case "float"   -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float",   "floatValue",   "()F", false);
            case "double"  -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double",  "doubleValue",  "()D", false);
            case "long"    -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long",    "longValue",    "()J", false);
            case "boolean" -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        }
    }

    protected void pushInt(int value) {
        if      (value >= -1 && value <= 5)                              methodVisitor.visitInsn(ICONST_0 + value);
        else if (value >= Byte.MIN_VALUE  && value <= Byte.MAX_VALUE)    methodVisitor.visitIntInsn(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)   methodVisitor.visitIntInsn(SIPUSH, value);
        else                                                             methodVisitor.visitLdcInsn(value);
    }

    protected void pushDouble(double value) {
        if      (value == 0.0) methodVisitor.visitInsn(DCONST_0);
        else if (value == 1.0) methodVisitor.visitInsn(DCONST_1);
        else                   methodVisitor.visitLdcInsn(value);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
    }

    protected void pushBoolean(boolean value) {
        methodVisitor.visitInsn(value ? ICONST_1 : ICONST_0);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
    }

    protected void pushObjectArray(@NotNull List<Expression> args) {
        pushInt(args.size());
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < args.size(); i++) {
            methodVisitor.visitInsn(DUP);
            pushInt(i);
            compileExpression(args.get(i));
            methodVisitor.visitInsn(AASTORE);
        }
    }
    protected void pushScope() {
        locals.pushScope();
        typeEnv.push(new HashMap<>());
    }

    protected void popScope() {
        locals.popScope();
        typeEnv.pop();
    }

    protected void defineLocalType(String name, String type) {
        if (typeEnv.isEmpty()) typeEnv.push(new HashMap<>());
        assert typeEnv.peek() != null;
        typeEnv.peek().put(name, type != null ? type : "any");
    }

    protected @Nullable String getLocalType(String name) {
        for (Map<String, String> scope : typeEnv) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    protected String getFieldType(@NotNull ClassStatement cs, String fieldName) {
        for (Statement st : cs.body()) {
            if (st instanceof VarStatement vs   && vs.name().equals(fieldName)) return vs.type();
            if (st instanceof ConstStatement c  && c.name().equals(fieldName))  return c.type();
        }
        return "any";
    }

    protected boolean isConstField(@NotNull ClassStatement cs, String fieldName) {
        for (Statement st : cs.body()) {
            if (st instanceof ConstStatement c && c.name().equals(fieldName)) return true;
        }
        return false;
    }

    protected boolean hasField(@NotNull ClassStatement cs, String fieldName) {
        for (Statement st : cs.body()) {
            if (st instanceof VarStatement vs  && vs.name().equals(fieldName))  return true;
            if (st instanceof ConstStatement c && c.name().equals(fieldName))   return true;
        }
        return false;
    }

    protected boolean isTypeCompatible(@NotNull String declared, String actual) {
        if (declared.equals("any") || actual == null || actual.equals("any")) return true;
        if (declared.equals(actual))                        return true;
        Set<String> numbers = Set.of("int", "double", "float", "long");
        if (numbers.contains(declared) && numbers.contains(actual)) return true;
        // Collection types are only compatible with themselves (or "any" handled above)
        Set<String> collections = Set.of("Map", "List", "Set");
        if (collections.contains(declared) || collections.contains(actual)) return false;
        return false;
    }

    /**
     * Emits {@code LocalVariableTable} entries for all {@code const} and {@code var}
     * locals defined in the current method scope.  Must be called after the method
     * body is compiled but <em>before</em> {@code visitMaxs}/{@code visitEnd}.
     * {@code endLabel} must already be visited (emitted) before this call.
     */
    protected void emitConstLocalVariables(@NotNull MethodVisitor mv, @NotNull Label endLabel) {
        for (Map.Entry<String, Label> entry : constLocalLabels.entrySet()) {
            String name  = entry.getKey();
            Label  start = entry.getValue();
            int    slot  = locals.get(name);
            if (slot < 0) continue;
            String larvType = localVarTypes.getOrDefault(name, "any");
            mv.visitLocalVariable(name, larvTypeToDescriptor(larvType), null, start, endLabel, slot);
        }
        for (Map.Entry<String, Label> entry : varLocalLabels.entrySet()) {
            String name  = entry.getKey();
            Label  start = entry.getValue();
            int    slot  = locals.get(name);
            if (slot < 0) continue;
            String larvType = localVarTypes.getOrDefault(name, "any");
            mv.visitLocalVariable(name, larvTypeToDescriptor(larvType), null, start, endLabel, slot);
        }
        constLocals.clear();
        constLocalLabels.clear();
        varLocalLabels.clear();
    }

    protected void emitDefaultConstructor(@NotNull ClassWriter cw, String superClass) {
        MethodVisitor ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V", false);
        ctor.visitInsn(RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
    }

    /** Compile a single expression, leaving its value on the operand stack. */
    protected abstract void compileExpression(@NotNull Expression expr);

    /** Compile a single statement. */
    protected abstract void compileStatement(@NotNull Statement st);

    /** Compile a list of statements, honouring any {@code defer} declarations. */
    protected abstract void compileStatementsWithDefer(@NotNull List<Statement> stmts);
}