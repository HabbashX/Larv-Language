package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.stdlib.LarvStdlibLoader;
import com.habbashx.larv.compiler.util.LocalVarTable;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.ast.statement.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Top-level Larv compiler that orchestrates the full compilation pipeline.
 *
 * <p>This class only contains the entry-point {@link #compile(List)} method and
 * the module-compilation stub.  All heavy lifting is delegated to the
 * specialised sub-compilers in the inheritance chain:</p>
 *
 * <pre>
 * LarvCompiler
 *   └─ ClassCompiler          – class &amp; function declarations, getter/setter generation
 *        └─ StatementCompiler – every Statement subtype, defer semantics
 *             └─ ExpressionCompiler – every Expression subtype
 *                  └─ CallCompiler  – call routing &amp; method-invocation bytecode
 *                       └─ TypeInferenceCompiler – type evaluation &amp; return-type inference
 *                            └─ AbstractLarvCompiler – shared state &amp; utility methods
 * </pre>
 *
 * <h2>Compilation phases</h2>
 * <ol>
 *   <li><b>Pre-pass</b>: scan statements and populate {@link #topLevelFunctions},
 *       {@link #larvClasses}, {@link #moduleNames}, and {@link #importedLibs}.</li>
 *   <li><b>Main class</b>: emit the top-level class, compile top-level functions as
 *       static methods, then compile the implicit {@code main} method body.</li>
 *   <li><b>Larv classes</b>: delegate to {@link ClassCompiler#compileLarvClass}.</li>
 *   <li><b>Modules</b>: delegate to {@link #compileLarvModule} (stub).</li>
 * </ol>
 */
public class LarvCompiler extends ClassCompiler {

    /**
     * @param mainClassName dot-separated name of the class being compiled
     *                      (e.g. {@code com.example.Main})
     */
    public LarvCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    /**
     * Compiles a list of top-level statements and returns one
     * {@link CompiledClass} per generated JVM class.
     */
    public List<CompiledClass> compile(@NotNull List<Statement> statements) {

        for (Statement s : statements) {
            if      (s instanceof FunctionStatement fn)  topLevelFunctions.put(fn.name(), fn.params().size());
            else if (s instanceof ClassStatement cs)     larvClasses.put(cs.name(), cs);
            else if (s instanceof ModuleStatement ms)    moduleNames.put(ms.name(), mainInternalName + "$mod$" + ms.name());
            else if (s instanceof ImportStatement is && is.library() != null) importedLibs.add(is.library());
        }

        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classWriter.visit(V21, ACC_PUBLIC | ACC_SUPER, mainInternalName, null, "java/lang/Object", null);
        classWriter.visitSource(mainClassName + ".larv", null);
        emitDefaultConstructor(classWriter, "java/lang/Object");

        for (Statement s : statements) {
            if (s instanceof FunctionStatement fn) compileFunction(fn);
        }

        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        methodVisitor.visitCode();
        locals = new LocalVarTable(1);
        localVarTypes.clear();
        constLocals.clear();
        constLocalLabels.clear();
        varLocalLabels.clear();
        typeEnv.clear();
        typeEnv.push(new HashMap<>());

        List<Statement> mainBody = statements.stream()
                .filter(s -> !(s instanceof FunctionStatement) && !(s instanceof ClassStatement))
                .toList();
        compileStatementsWithDefer(mainBody);

        Label mainEnd = new Label();
        methodVisitor.visitLabel(mainEnd);
        emitConstLocalVariables(methodVisitor, mainEnd);
        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
        classWriter.visitEnd();
        output.add(new CompiledClass(mainClassName, classWriter.toByteArray()));

        for (ClassStatement cs : larvClasses.values()) {
            compileLarvClass(cs);
        }

        statements.stream()
                .filter(s -> s instanceof ModuleStatement)
                .map(s -> (ModuleStatement) s)
                .forEach(this::compileLarvModule);

        return output;
    }


    private void compileLarvModule(ModuleStatement ms) {
    }
}