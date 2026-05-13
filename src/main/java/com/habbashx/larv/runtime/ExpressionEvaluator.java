package com.habbashx.larv.runtime;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.parser.ast.expression.*;
import com.habbashx.larv.parser.ast.expression.visitor.ExpressionVisitor;
import com.habbashx.larv.parser.ast.statement.FunctionStatement;
import com.habbashx.larv.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator implements ExpressionVisitor {

    private final ExecutionContext context;
    private final FunctionInvoker  invoker;

    public ExpressionEvaluator(ExecutionContext context, FunctionInvoker invoker) {
        this.context = context;
        this.invoker = invoker;
    }

    public Object eval(@NotNull Expression expr) {
        return switch (expr) {
            case NumberExpression  e -> visitNumber(e);
            case StringExpression  e -> visitString(e);
            case LiteralExpression e -> e.value();
            case BooleanExpression e -> e.value();
            case VarExpression     e -> visitVar(e);
            case UnaryExpression   e -> visitUnary(e);
            case BinaryExpression  e -> visitBinary(e);
            case CallExpression    e -> visitCall(e);
            case NewExpression     e -> visitNew(e);
            case ThisExpression    e -> visitThis(e);
            case GetExpression     e -> visitGet(e);
            case SetExpression     e -> visitSet(e);
            case JavaCallExpression e -> visitJavaCall(e);
            case ArrayExpression   e -> visitArray(e);
            case IndexExpression   e -> visitIndex(e);
            case AssignExpression  e -> visitAssignExpr(e);
            default -> throw new LarvError("Unknown expression type: " + expr.getClass().getSimpleName());
        };
    }

    @Override public Object visitNumber(@NotNull NumberExpression e) { return e.value(); }
    @Override public Object visitString(@NotNull StringExpression e) { return e.value(); }
    @Override public Object visitVar(@NotNull VarExpression e) { return context.getEnvironment().get(e.name()); }

    @Override
    public Object visitBinary(@NotNull BinaryExpression e) {
        Object left  = eval(e.left());
        Object right = eval(e.right());
        return BinaryOperator.apply(e.operator(), left, right);
    }

    private Object visitUnary(@NotNull UnaryExpression e) {
        Object operand = eval(e.right());
        return switch (e.operator()) {
            case "-" -> {
                if (operand instanceof Double d)  yield -d;
                if (operand instanceof Integer i) yield (double) -i;
                throw new LarvError("Unary '-' requires a number, got: " + typeName(operand));
            }
            default -> throw new LarvError("Unknown unary operator: '" + e.operator() + "'");
        };
    }

    @Override
    public Object visitCall(@NotNull CallExpression e) {
        List<Object> args = evalAll(e.arguments());
        return switch (e.caller()) {
            case VarExpression(String name)               -> callFunction(name, args);
            case GetExpression(Expression obj, String m)  -> callMethod(obj, m, args);
            default -> throw new LarvError("Invalid call target — expected a function or method");
        };
    }

    @Override
    public Object visitNew(@NotNull NewExpression e) {
        String className = e.className();
        var clazz = context.getClass(className);
        if (clazz == null) throw new LarvError("Undefined class '" + className + "'");

        LarvObject obj     = new LarvObject();
        Map<String, FunctionStatement> methods = collectMethods(clazz.body());
        obj.set("__methods__", methods);

        Environment saved = context.getEnvironment();
        context.pushScope();
        try {
            context.getEnvironment().define("this", obj);
            FunctionStatement init = methods.get("init");
            if (init != null) invoker.invokeMethod(init, obj, evalAll(e.args()));
            return obj;
        } finally {
            context.popScope(saved);
        }
    }

    @Override public Object visitThis(ThisExpression e)   { return context.getEnvironment().get("this"); }
    @Override public Object visitGet(GetExpression e)     { return requireObject(eval(e.object()), "field access '" + e.field() + "'").get(e.field()); }
    @Override public Object visitSet(SetExpression e)     { requireObject(eval(e.object()), "field assignment '" + e.field() + "'").set(e.field(), eval(e.value())); return null; }

    private Object callFunction(String name, List<Object> args) {
        if (context.hasNative(name)) return context.invokeNative(name, args);
        FunctionStatement fn = context.getFunction(name);
        if (fn == null) throw new LarvError("Undefined function '" + name + "' — did you define it with 'func'?");
        if (fn.params().size() != args.size())
            throw new LarvError("Function '" + name + "' expects " + fn.params().size() +
                    " argument(s) but got " + args.size());
        return invoker.invokeFunction(fn, args);
    }

    private Object callMethod(Expression objExpr, String methodName, List<Object> args) {
        int line = -1, column = -1;
        if (objExpr instanceof VarExpression(String name) && context.getJavaRegistry().hasAlias(name))
            return context.getJavaRegistry().invoke(name, methodName, args);

        Object target = eval(objExpr);

        if (target instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) list;
            return ArrayMethods.dispatch(array, methodName, args);
        }

        LarvObject obj = requireObject(target, "method call '" + methodName + "'");
        FunctionStatement fn = getMethods(obj).get(methodName);
        if (fn == null) throw new LarvError("Undefined method '" + methodName + "' on object");
        return invoker.invokeMethod(fn, obj, args);
    }

    private @NotNull List<Object> evalAll(@NotNull List<Expression> exprs) {
        List<Object> values = new ArrayList<>(exprs.size());
        for (Expression e : exprs) values.add(eval(e));
        return values;
    }

    private @NotNull Map<String, FunctionStatement> collectMethods(@NotNull List<Statement> body) {
        Map<String, FunctionStatement> methods = new HashMap<>();
        for (Statement s : body) if (s instanceof FunctionStatement fn) methods.put(fn.name(), fn);
        return methods;
    }

    private LarvObject requireObject(Object value, String operation) {
        if (value instanceof LarvObject lo) return lo;
        if (value == null) throw new LarvError("Attempted " + operation + " on 'nil' — the value is not an object");
        throw new LarvError("Attempted " + operation + " on a " + typeName(value) + " — expected an object");
    }

    @SuppressWarnings("unchecked")
    private Map<String, FunctionStatement> getMethods(@NotNull LarvObject obj) {
        return (Map<String, FunctionStatement>) obj.get("__methods__");
    }

    private Object visitJavaCall(@NotNull JavaCallExpression e) {
        List<Object> args = evalAll(e.arguments());
        return context.getJavaRegistry().invoke(e.alias(), e.methodName(), args);
    }

    private Object visitArray(@NotNull ArrayExpression e) {
        List<Object> list = new ArrayList<>();
        for (Expression el : e.elements()) list.add(eval(el));
        return list;
    }

    private Object visitIndex(@NotNull IndexExpression e) {
        Object target = eval(e.array());
        Object idx    = eval(e.index());
        if (!(idx instanceof Double))
            throw new LarvError("Array index must be a number, got: " + typeName(idx));
        if (target instanceof List<?> list) {
            int i = ((Double) idx).intValue();
            if (i < 0 || i >= list.size())
                throw new LarvError("Index " + i + " is out of bounds — array has " + list.size() + " element(s)");
            return list.get(i);
        }
        if (target == null) throw new LarvError("Cannot index into 'nil'");
        throw new LarvError("Cannot index into " + typeName(target) + " — expected an array");
    }

    private Object visitAssignExpr(@NotNull AssignExpression e) {
        Object value = eval(e.value());
        context.getEnvironment().assign(e.name(), value);
        return value;
    }

    private String typeName(Object v) {
        if (v == null)              return "nil";
        if (v instanceof Double)    return "number";
        if (v instanceof String)    return "string";
        if (v instanceof Boolean)   return "boolean";
        if (v instanceof List)      return "array";
        if (v instanceof LarvObject) return "object";
        return v.getClass().getSimpleName();
    }
}
