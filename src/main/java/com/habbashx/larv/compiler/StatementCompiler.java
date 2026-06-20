package com.habbashx.larv.compiler;

import com.habbashx.larv.compiler.exception.CompileException;
import com.habbashx.larv.parser.ast.expression.Expression;
import com.habbashx.larv.parser.ast.statement.*;
import com.habbashx.larv.parser.ast.statement.SwitchStatement.SwitchCase;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Compiles every {@link Statement} subtype to JVM bytecode.
 *
 * <p>The large {@code compileStatementsWithDefer} method implements Go-style
 * {@code defer} semantics: deferred expressions are collected in declaration
 * order, then emitted inside a {@code finally} block (in LIFO order) so they
 * run even when an exception is thrown.</p>
 */
public abstract class StatementCompiler extends ExpressionCompiler {

    protected StatementCompiler(@NotNull String mainClassName) {
        super(mainClassName);
    }

    @Override
    public void compileStatement(@NotNull Statement st) {
        if (debugMode) {
            debugLog("compileStatement  line=" + st.line()
                    + "  type=" + st.getClass().getSimpleName());
            emitLineNumber(st.line());
        }
        try {
            switch (st) {
                case VarStatement s             -> compileVar(s);
                case ConstStatement s           -> compileConst(s);
                case AssignStatement s          -> compileAssign(s);
                case CompoundAssignStatement s  -> compileCompoundAssign(s);
                case PrintStatement s           -> compilePrint(s);
                case ExprStatement s            -> { compileExpression(s.value());if (!isVoidCallExpression(s.value())) methodVisitor.visitInsn(POP);}
                case IfStatement s              -> compileIf(s);
                case WhileStatement s           -> compileWhile(s);
                case ForStatement s             -> compileFor(s);
                case ForeachStatement s         -> compileForeach(s);
                case ReturnStatement s          -> compileReturn(s);
                case BreakStatement s           -> compileBreak(s);
                case ContinueStatement s        -> compileContinue(s);
                case BlockStatement s           -> compileBlock(s);
                case IncrementStatement s       -> compileIncrement(s);
                case DecrementStatement s       -> compileDecrement(s);
                case TryCatchStatement s        -> compileTryCatch(s);
                case ThrowStatement s           -> compileThrow(s);
                case SwitchStatement s          -> compileSwitch(s);
                case SetFieldStatement s        -> compileSetField(s);
                case IndexAssignStatement s     -> compileIndexAssign(s);
                case EnumStatement s            -> compileEnum(s);
                case FunctionStatement ignored  -> { /* handled by ClassCompiler */ }
                case ClassStatement ignored     -> { /* handled by ClassCompiler */ }
                case DeferStatement ignored     -> { /* collected by compileStatementsWithDefer */ }
                case ImportStatement s          -> compileImport(s);
                case ModuleStatement s          -> compileModuleInit(s);
                case JavaBindStatement s        -> compileJavaBind(s);
                case AtomicStatement s          -> compileAtomic(s);
                default -> throw new CompileException(
                        "Unknown statement type: " + st.getClass().getSimpleName(), st.line());
            }
        } catch (Exception e) {
            if (debugMode) {
                debugLog("EXCEPTION in compileStatement"
                        + "  type=" + st.getClass().getSimpleName()
                        + "  line=" + st.line()
                        + "  exception=" + e.getClass().getSimpleName()
                        + "  message=" + e.getMessage());
                e.printStackTrace(System.err);
            }
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles a list of statements that may contain {@link DeferStatement}
     * nodes.
     *
     * <p>Deferred expressions are collected in declaration order, then emitted
     * inside a {@code finally} block so they run even on exception — in LIFO
     * order (last defer runs first), matching Go semantics.</p>
     */
    @Override
    public void compileStatementsWithDefer(@NotNull List<Statement> stmts) {
        List<Expression> deferred = new ArrayList<>();
        for (Statement st : stmts) {
            if (st instanceof DeferStatement ds) deferred.add(ds.value());
        }

        if (deferred.isEmpty()) {
            for (Statement st : stmts) compileStatement(st);
            return;
        }

        for (Statement st : stmts) {
            if      (st instanceof VarStatement vs)       compileVar(vs);
            else if (st instanceof ConstStatement cs)     compileConst(cs);
            else if (st instanceof JavaBindStatement jbs) compileJavaBind(jbs);
            else if (st instanceof EnumStatement es)      compileEnum(es);
            else if (st instanceof AtomicStatement as)    compileAtomic(as);
        }

        Label tryStart       = new Label();
        Label tryEnd         = new Label();
        Label finallyHandler = new Label();
        Label afterFinally   = new Label();

        methodVisitor.visitTryCatchBlock(tryStart, tryEnd, finallyHandler, null);

        methodVisitor.visitLabel(tryStart);
        for (Statement st : stmts) {
            if (st instanceof DeferStatement
                    || st instanceof VarStatement
                    || st instanceof ConstStatement
                    || st instanceof JavaBindStatement
                    || st instanceof EnumStatement
                    || st instanceof AtomicStatement) continue;
            compileStatement(st);
        }
        methodVisitor.visitLabel(tryEnd);

        for (int i = deferred.size() - 1; i >= 0; i--) {
            compileExpression(deferred.get(i));
            methodVisitor.visitInsn(POP);
        }
        methodVisitor.visitJumpInsn(GOTO, afterFinally);

        methodVisitor.visitLabel(finallyHandler);
        int savedEx = locals.allocTemp();
        methodVisitor.visitVarInsn(ASTORE, savedEx);
        for (int i = deferred.size() - 1; i >= 0; i--) {
            compileExpression(deferred.get(i));
            methodVisitor.visitInsn(POP);
        }
        methodVisitor.visitVarInsn(ALOAD, savedEx);
        methodVisitor.visitInsn(ATHROW);

        methodVisitor.visitLabel(afterFinally);
    }

    protected void compileVar(@NotNull VarStatement s) {
        if (s.isVolatile() && currentClassStatement == null)
            throw new CompileException("the 'volatile' modifier is only allowed on class field", s.line());

        String declaredType = s.type();
        String type;
        if (s.expression() != null) {
            String inferredType = evaluateExpressionType(s.expression());
            debugLog("  var  name=" + s.name() + "  declared=" + declaredType + "  inferred=" + inferredType + "  line=" + s.line());
            if (!declaredType.equals("any")) {
                if (!isTypeCompatible(declaredType, inferredType)) {
                    debugLog("  TYPE MISMATCH  var=" + s.name() + "  declared=" + declaredType + "  actual=" + inferredType);
                    throw new CompileException(
                            "Type Error: Cannot assign value of type '" + inferredType +
                                    "' to variable '" + s.name() + "' of type '" + declaredType + "'",
                            s.line());
                }
                type = declaredType;
            } else {
                type = inferredType;
            }
            compileExpressionAs(s.expression(), type);
        } else {
            type = declaredType.equals("any") ? "any" : declaredType;
            debugLog("  var  name=" + s.name() + "  type=" + type + "  (no initializer → null)  line=" + s.line());
            methodVisitor.visitInsn(ACONST_NULL);
        }

        int slot = locals.define(s.name());
        defineLocalType(s.name(), type);
        localVarTypes.put(s.name(), type);
        debugLog("  var  name=" + s.name() + "  resolvedType=" + type + "  slot=" + slot);

        Label varStart = new Label();
        methodVisitor.visitLabel(varStart);
        varLocalLabels.put(s.name(), varStart);
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    protected void compileConst(@NotNull ConstStatement s) {
        String declaredType = s.type();

        Label constStart = new Label();
        methodVisitor.visitLabel(constStart);

        String type;
        if (s.value() != null) {
            String exprType = evaluateExpressionType(s.value());
            debugLog("  const  name=" + s.name() + "  declared=" + declaredType + "  inferred=" + exprType + "  line=" + s.line());
            if (!declaredType.equals("any") && !isTypeCompatible(declaredType, exprType)) {
                debugLog("  TYPE MISMATCH  const=" + s.name() + "  declared=" + declaredType + "  actual=" + exprType);
                throw new CompileException(
                        "Type Error: Cannot assign value of type '" + exprType +
                                "' to constant '" + s.name() + "' of type '" + declaredType + "'",
                        s.line());
            }
            type = declaredType.equals("any") ? exprType : declaredType;
            compileExpressionAs(s.value(), type);
        } else {
            type = declaredType;
            debugLog("  const  name=" + s.name() + "  type=" + type + "  (no initializer → null)  line=" + s.line());
            methodVisitor.visitInsn(ACONST_NULL);
        }

        defineLocalType(s.name(), type);
        int slot = locals.define(s.name());
        localVarTypes.put(s.name(), type);
        constLocals.add(s.name());
        constLocalLabels.put(s.name(), constStart);
        debugLog("  const  name=" + s.name() + "  resolvedType=" + type + "  slot=" + slot);
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    protected void compileAssign(@NotNull AssignStatement s) {
        if (constLocals.contains(s.name())) {
            throw new CompileException("Cannot reassign constant '" + s.name() + "'", s.line());
        }

        if (currentClassStatement != null) {
            ClassStatement currentClass = larvClasses.get(currentClassStatement.name());
            if (currentClass != null && hasField(currentClass, s.name())) {
                String fieldType = getFieldType(currentClass, s.name());
                String exprType  = evaluateExpressionType(s.value());
                if (!isTypeCompatible(fieldType, exprType)) {
                    throw new CompileException(
                            "Type Error: Cannot assign type '" + exprType +
                                    "' to field '" + s.name() + "' of type '" + fieldType + "'",
                            s.line());
                }
                methodVisitor.visitVarInsn(ALOAD, 0);
                String fieldDesc = isPrimitive(fieldType) ? primitiveDescriptor(fieldType) : JAVA_OBJECT;
                if (isPrimitive(fieldType)) compileExpressionPrimitive(s.value(), fieldType);
                else                        compileExpression(s.value());
                methodVisitor.visitFieldInsn(PUTFIELD, currentClassStatement.name(), s.name(), fieldDesc);
                return;
            }
        }

        String localType = getLocalType(s.name());
        String exprType  = evaluateExpressionType(s.value());
        if (localType != null && !isTypeCompatible(localType, exprType)) {
            throw new CompileException(
                    "Type Error: Cannot assign value of type '" + exprType +
                            "' to variable '" + s.name() + "' of type '" + localType + "'",
                    s.line());
        }
        compileExpression(s.value());
        int slot = locals.get(s.name());
        if (slot < 0) slot = locals.define(s.name());
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    protected void compileCompoundAssign(@NotNull CompoundAssignStatement s) {
        if (constLocals.contains(s.name())) {
            throw new CompileException("Cannot reassign constant '" + s.name() + "'", s.line());
        }
        int slot = locals.get(s.name());
        if (slot < 0) throw new CompileException("Undefined variable: " + s.name(), s.line());
        switch (s.operator()) {
            case "+" -> {
                methodVisitor.visitVarInsn(ALOAD, slot);
                compileExpression(s.value());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "add",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            }
            case "-" -> {
                methodVisitor.visitVarInsn(ALOAD, slot);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(s.value());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitInsn(DSUB);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            case "*" -> {
                methodVisitor.visitVarInsn(ALOAD, slot);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(s.value());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitInsn(DMUL);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            case "/" -> {
                methodVisitor.visitVarInsn(ALOAD, slot);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                compileExpression(s.value());
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "divide", "(DD)D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
            default -> throw new CompileException("Unknown compound op: " + s.operator(), s.line());
        }
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    private void compilePrint(@NotNull PrintStatement s) {
        compileExpression(s.value());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "print",
                "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        methodVisitor.visitInsn(POP);
    }

    private void compileIf(@NotNull IfStatement s) {
        debugLog("  if  line=" + s.line() + "  hasElse=" + !s.elseBranch().isEmpty());
        Label elseLabel = new Label(), endLabel = new Label();
        compileExpression(s.condition());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
        methodVisitor.visitJumpInsn(IFEQ, elseLabel);
        locals.pushScope();
        compileStatementsWithDefer(s.thenBranch());
        locals.popScope();
        methodVisitor.visitJumpInsn(GOTO, endLabel);
        methodVisitor.visitLabel(elseLabel);
        if (!s.elseBranch().isEmpty()) {
            locals.pushScope();
            compileStatementsWithDefer(s.elseBranch());
            locals.popScope();
        }
        methodVisitor.visitLabel(endLabel);
    }

    private void compileWhile(@NotNull WhileStatement s) {
        debugLog("  while  line=" + s.line());
        Label start = new Label(), end = new Label();
        loopStack.push(new Label[]{start, end});
        methodVisitor.visitLabel(start);
        compileExpression(s.condition());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
        methodVisitor.visitJumpInsn(IFEQ, end);
        locals.pushScope();
        compileStatementsWithDefer(s.body());
        locals.popScope();
        methodVisitor.visitJumpInsn(GOTO, start);
        methodVisitor.visitLabel(end);
        loopStack.pop();
    }

    private void compileFor(@NotNull ForStatement s) {
        debugLog("  for  line=" + s.line() + "  hasInit=" + (s.init() != null) + "  hasCond=" + (s.condition() != null) + "  hasIncr=" + (s.increment() != null));
        Label start = new Label(), cont = new Label(), end = new Label();
        loopStack.push(new Label[]{cont, end});
        locals.pushScope();
        if (s.init() != null) compileStatement(s.init());
        methodVisitor.visitLabel(start);
        if (s.condition() != null) {
            compileExpression(s.condition());
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
            methodVisitor.visitJumpInsn(IFEQ, end);
        }
        locals.pushScope();
        compileStatementsWithDefer(s.body());
        locals.popScope();
        methodVisitor.visitLabel(cont);
        if (s.increment() != null) compileStatement(s.increment());
        methodVisitor.visitJumpInsn(GOTO, start);
        methodVisitor.visitLabel(end);
        locals.popScope();
        loopStack.pop();
    }

    private void compileForeach(@NotNull ForeachStatement s) {
        Label start = new Label(), end = new Label();
        loopStack.push(new Label[]{start, end});

        compileExpression(s.iterable());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toIterable",
                "(Ljava/lang/Object;)Ljava/lang/Iterable;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/lang/Iterable", "iterator",
                "()Ljava/util/Iterator;", true);
        int iterSlot = locals.allocTemp();
        methodVisitor.visitVarInsn(ASTORE, iterSlot);

        methodVisitor.visitLabel(start);
        methodVisitor.visitVarInsn(ALOAD, iterSlot);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        methodVisitor.visitJumpInsn(IFEQ, end);

        methodVisitor.visitVarInsn(ALOAD, iterSlot);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next",
                "()Ljava/lang/Object;", true);
        int entrySlot = locals.allocTemp();
        methodVisitor.visitVarInsn(ASTORE, entrySlot);

        locals.pushScope();
        methodVisitor.visitVarInsn(ALOAD, entrySlot);
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "mapEntryKey",
                "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ASTORE, locals.define(s.variable()));

        if (s.valueVariable() != null) {
            methodVisitor.visitVarInsn(ALOAD, entrySlot);
            methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "mapEntryValue",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            methodVisitor.visitVarInsn(ASTORE, locals.define(s.valueVariable()));
        }

        compileStatementsWithDefer(s.body());
        locals.popScope();
        methodVisitor.visitJumpInsn(GOTO, start);
        methodVisitor.visitLabel(end);
        loopStack.pop();
    }

    private void compileBreak(BreakStatement s) {
        if (loopStack.isEmpty()) throw new CompileException("break outside loop", s.line());
        methodVisitor.visitJumpInsn(GOTO, loopStack.peek()[1]);
    }

    private void compileContinue(ContinueStatement s) {
        if (loopStack.isEmpty()) throw new CompileException("continue outside loop", s.line());
        methodVisitor.visitJumpInsn(GOTO, loopStack.peek()[0]);
    }

    private void compileBlock(@NotNull BlockStatement s) {
        locals.pushScope();
        compileStatementsWithDefer(s.statements());
        locals.popScope();
    }

    protected void compileReturn(@NotNull ReturnStatement s) {
        boolean isVoid = "V".equals(currentReturnType);
        debugLog("  return  line=" + s.line() + "  hasValue=" + (s.value() != null) + "  isVoid=" + isVoid + "  returnType=" + currentReturnType);
        if (s.value() != null) {
            compileExpression(s.value());
            if (!isVoid && !"java/lang/Object".equals(currentReturnType)) {
                String exprType     = evaluateExpressionType(s.value());
                String exprInternal = getInternalType(exprType);
                if (!currentReturnType.equals(exprInternal)) {
                    methodVisitor.visitTypeInsn(CHECKCAST, currentReturnType);
                }
            }
        } else {
            methodVisitor.visitInsn(ACONST_NULL);
        }
        methodVisitor.visitInsn(isVoid ? RETURN : ARETURN);
    }

    private void compileIncrement(@NotNull IncrementStatement s) {
        int slot = locals.get(s.name());
        if (slot < 0) throw new CompileException("Undefined variable: " + s.name(), s.line());
        methodVisitor.visitVarInsn(ALOAD, slot);
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
        methodVisitor.visitLdcInsn(1.0);
        methodVisitor.visitInsn(DADD);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    private void compileDecrement(@NotNull DecrementStatement s) {
        int slot = locals.get(s.name());
        if (slot < 0) throw new CompileException("Undefined variable: " + s.name(), s.line());
        methodVisitor.visitVarInsn(ALOAD, slot);
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
        methodVisitor.visitLdcInsn(1.0);
        methodVisitor.visitInsn(DSUB);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    private void compileTryCatch(@NotNull TryCatchStatement s) {
        Label tryStart   = new Label(), tryEnd = new Label();
        Label catchStart = new Label(), afterAll = new Label();
        boolean hasCatch   = !s.catchBody().isEmpty();
        boolean hasFinally = !s.finallyBody().isEmpty();
        debugLog("  try-catch  line=" + s.line() + "  hasCatch=" + hasCatch + "  hasFinally=" + hasFinally + "  catchVar=" + s.catchVar());

        if (hasCatch) {
            methodVisitor.visitTryCatchBlock(tryStart, tryEnd, catchStart, "java/lang/Throwable");
        }
        Label finallyHandler = null;
        if (hasFinally) {
            finallyHandler = new Label();
            methodVisitor.visitTryCatchBlock(tryStart, tryEnd, finallyHandler, null);
            if (hasCatch) methodVisitor.visitTryCatchBlock(catchStart, afterAll, finallyHandler, null);
        }

        methodVisitor.visitLabel(tryStart);
        locals.pushScope();
        compileStatementsWithDefer(s.tryBody());
        locals.popScope();
        methodVisitor.visitLabel(tryEnd);
        if (hasFinally) { locals.pushScope(); compileStatementsWithDefer(s.finallyBody()); locals.popScope(); }
        methodVisitor.visitJumpInsn(GOTO, afterAll);

        if (hasCatch) {
            methodVisitor.visitLabel(catchStart);
            String varName = s.catchVar() != null ? s.catchVar() : "__ex__";
            int exSlot = locals.define(varName);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage",
                    "()Ljava/lang/String;", false);
            methodVisitor.visitVarInsn(ASTORE, exSlot);
            locals.pushScope();
            compileStatementsWithDefer(s.catchBody());
            locals.popScope();
            if (hasFinally) { locals.pushScope(); compileStatementsWithDefer(s.finallyBody()); locals.popScope(); }
            methodVisitor.visitJumpInsn(GOTO, afterAll);
        }

        if (hasFinally) {
            methodVisitor.visitLabel(finallyHandler);
            int savedEx = locals.allocTemp();
            methodVisitor.visitVarInsn(ASTORE, savedEx);
            locals.pushScope();
            compileStatementsWithDefer(s.finallyBody());
            locals.popScope();
            methodVisitor.visitVarInsn(ALOAD, savedEx);
            methodVisitor.visitInsn(ATHROW);
        }
        methodVisitor.visitLabel(afterAll);
    }

    private void compileThrow(@NotNull ThrowStatement s) {
        methodVisitor.visitTypeInsn(NEW, LARV_EX);
        methodVisitor.visitInsn(DUP);
        compileExpression(s.value());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "stringify",
                "(Ljava/lang/Object;)Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, LARV_EX, "<init>", "(Ljava/lang/String;)V", false);
        methodVisitor.visitInsn(ATHROW);
    }

    private void compileSwitch(@NotNull SwitchStatement s) {
        Label endLabel = new Label();
        loopStack.push(new Label[]{endLabel, endLabel});

        compileExpression(s.subject());
        int switchSlot = locals.allocTemp();
        methodVisitor.visitVarInsn(ASTORE, switchSlot);

        for (SwitchCase c : s.cases()) {
            Label nextCase = new Label();
            if (!c.values().isEmpty()) {
                Label matchedLabel = new Label();
                for (Expression val : c.values()) {
                    methodVisitor.visitVarInsn(ALOAD, switchSlot);
                    compileExpression(val);
                    methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "equalEqual",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                    methodVisitor.visitJumpInsn(IFNE, matchedLabel);
                }
                methodVisitor.visitJumpInsn(GOTO, nextCase);
                methodVisitor.visitLabel(matchedLabel);
            }
            locals.pushScope();
            compileStatementsWithDefer(c.body());
            locals.popScope();
            methodVisitor.visitJumpInsn(GOTO, endLabel);
            methodVisitor.visitLabel(nextCase);
        }

        if (!s.defaultBody().isEmpty()) {
            locals.pushScope();
            compileStatementsWithDefer(s.defaultBody());
            locals.popScope();
        }
        methodVisitor.visitLabel(endLabel);
        loopStack.pop();
    }

    private void compileSetField(@NotNull SetFieldStatement s) {
        compileExpression(s.object());
        methodVisitor.visitLdcInsn(s.field());
        compileExpression(s.value());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "setField",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false);
    }

    private void compileIndexAssign(@NotNull IndexAssignStatement s) {
        compileExpression(s.target());
        compileExpression(s.index());
        compileExpression(s.value());
        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "arraySet",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false);
    }

    protected void compileEnum(@NotNull EnumStatement s) {
        methodVisitor.visitTypeInsn(NEW, LARV_OBJ);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitLdcInsn(s.name());
        methodVisitor.visitMethodInsn(INVOKESPECIAL, LARV_OBJ, "<init>", "(Ljava/lang/String;)V", false);
        int enumSlot = locals.allocTemp();
        methodVisitor.visitVarInsn(ASTORE, enumSlot);

        List<String> variants = s.variants();
        for (int i = 0; i < variants.size(); i++) {
            methodVisitor.visitVarInsn(ALOAD, enumSlot);
            methodVisitor.visitLdcInsn(variants.get(i));
            pushDouble(i);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, LARV_OBJ, "set",
                    "(Ljava/lang/String;Ljava/lang/Object;)V", false);
        }
        methodVisitor.visitVarInsn(ALOAD, enumSlot);
        methodVisitor.visitVarInsn(ASTORE, locals.define(s.name()));
    }

    protected void compileImport(@NotNull ImportStatement s) {
        if (!s.isFileImport()) return;
        String path      = s.path();
        String javaPath  = path.replace('/', '.');
        int lastDot      = javaPath.lastIndexOf('.');
        String simpleName = (lastDot == -1) ? javaPath : javaPath.substring(lastDot + 1);
        typeRegistry.put(simpleName, javaPath);
    }

    @Contract(pure = true)
    protected void compileModuleInit(ModuleStatement s) {}

    protected void compileJavaBind(@NotNull JavaBindStatement s) {
        methodVisitor.visitLdcInsn(s.className());
        List<String> args = s.constructorArgs();
        int size = args == null ? 0 : args.size();

        if (size <= 5) methodVisitor.visitInsn(ICONST_0 + size);
        else           methodVisitor.visitIntInsn(BIPUSH, size);

        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < size; i++) {
            methodVisitor.visitInsn(DUP);
            if (i <= 5) methodVisitor.visitInsn(ICONST_0 + i);
            else        methodVisitor.visitIntInsn(BIPUSH, i);
            methodVisitor.visitLdcInsn(args.get(i));
            methodVisitor.visitInsn(AASTORE);
        }

        methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "createJavaObject",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        int slot = locals.define(s.alias());
        methodVisitor.visitVarInsn(ASTORE, slot);
    }

    protected void compileAtomic(@NotNull AtomicStatement as) {
        String internalName = switch (as.type()) {
            case "int"  -> ATOMIC_INT;
            case "long" -> ATOMIC_LONG;
            case "bool" -> ATOMIC_BOOLEAN;
            default     -> ATOMIC_REFERENCE;
        };

        methodVisitor.visitTypeInsn(NEW, internalName);
        methodVisitor.visitInsn(DUP);

        if (as.initializer() != null) {
            compileExpression(as.initializer());
            String constructorDesc = switch (as.type()) {
                case "int" -> {
                    methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                    methodVisitor.visitInsn(D2I);
                    yield "(I)V";
                }
                case "long" -> {
                    methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "toDouble", "(Ljava/lang/Object;)D", false);
                    methodVisitor.visitInsn(D2L);
                    yield "(J)V";
                }
                case "bool" -> {
                    methodVisitor.visitMethodInsn(INVOKESTATIC, RUNTIME, "isTruthy", "(Ljava/lang/Object;)Z", false);
                    yield "(Z)V";
                }
                default -> "(Ljava/lang/Object;)V";
            };
            methodVisitor.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", constructorDesc, false);
        } else {
            methodVisitor.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "()V", false);
        }

        int idx = locals.define(as.name());
        methodVisitor.visitVarInsn(ASTORE, idx);
    }

    public void visitImport(@NotNull ImportStatement st) {
        String path = st.path();
        if (path.contains("/")) {
            String className = path.substring(path.lastIndexOf('/') + 1);
            typeRegistry.put(className, path.replace('/', '.'));
        }
    }
}