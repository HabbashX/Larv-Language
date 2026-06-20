package com.habbashx.larv.runtime.stdlib;


import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List standard library — import list
 *   listNew()              → array    create empty list
 *   listAdd(list, val)     → nil      append val to list
 *   listAddAt(list,i,val)  → nil      insert val at index i
 *   listRemove(list, i)    → nil      remove element at index i
 *   listGet(list, i)       → any      get element at index i
 *   listSet(list, i, val)  → nil      set element at index i
 *   listSize(list)         → number   number of elements
 *   listContains(list,val) → boolean  true if list contains val
 *   listIndexOf(list,val)  → number   first index of val, -1 if missing
 *   listSlice(list,from,to)→ array    sub-list [from, to)
 *   listReverse(list)      → array    new reversed list
 *   listSort(list)         → array    new sorted list (numbers or strings)
 *   listConcat(a, b)       → array    new list = a + b
 *   listFlat(list)         → array    flatten one level of nesting
 *   listUnique(list)       → array    remove duplicates (order preserved)
 *   listFill(val, n)       → array    new list of n copies of val
 *   listClear(list)        → nil      remove all elements
 *   listIsEmpty(list)      → boolean  true if size == 0
 *   listFirst(list)        → any      first element
 *   listLast(list)         → any      last element
 *   listPop(list)          → any      remove and return last element
 *   listShuffle(list)      → nil      shuffle in place
 */
@SuppressWarnings("unchecked")
@Native("List Library")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeListLibrary extends NativeLibrary {


    public NativeListLibrary(ExecutionContext context) {
        super(context);
    }

    @Override
    public void registerAll() {
       getExecutionContext().registerNative("listNew",      this::listNew);
       getExecutionContext().registerNative("listAdd",      this::listAdd);
       getExecutionContext().registerNative("listAddAt",    this::listAddAt);
       getExecutionContext().registerNative("listRemove",   this::listRemove);
       getExecutionContext().registerNative("listGet",      this::listGet);
       getExecutionContext().registerNative("listSet",      this::listSet);
       getExecutionContext().registerNative("listSize",     this::listSize);
       getExecutionContext().registerNative("listContains", this::listContains);
       getExecutionContext().registerNative("listIndexOf",  this::listIndexOf);
       getExecutionContext().registerNative("listSlice",    this::listSlice);
       getExecutionContext().registerNative("listReverse",  this::listReverse);
       getExecutionContext().registerNative("listSort",     this::listSort);
       getExecutionContext().registerNative("listConcat",   this::listConcat);
       getExecutionContext().registerNative("listFlat",     this::listFlat);
       getExecutionContext().registerNative("listUnique",   this::listUnique);
       getExecutionContext().registerNative("listFill",     this::listFill);
       getExecutionContext().registerNative("listClear",    this::listClear);
       getExecutionContext().registerNative("listIsEmpty",  this::listIsEmpty);
       getExecutionContext().registerNative("listFirst",    this::listFirst);
       getExecutionContext().registerNative("listLast",     this::listLast);
       getExecutionContext().registerNative("listPop",      this::listPop);
       getExecutionContext().registerNative("listShuffle",  this::listShuffle);
    }

    private List<Object> listArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof List))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a list", -1, LarvError.Kind.RUNTIME);
        return (List<Object>) args.get(i);
    }

    private int intArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof Double d))
            throw new LarvError(fn + "(): argument " + (i+1) + " must be a number", -1, LarvError.Kind.RUNTIME);
        return ((Double) args.get(i)).intValue();
    }

    private void checkBounds(List<?> list, int i, String fn) {
        if (i < 0 || i >= list.size())
            throw new LarvError(fn + "(): index " + i + " out of bounds (size=" + list.size() + ")", -1, LarvError.Kind.RUNTIME);
    }

    @Contract(value = "_ -> new", pure = true)
    private @NotNull Object listNew(List<Object> a)     { return new ArrayList<>(); }
    private Object listSize(List<Object> a)    { return (double) listArg(a,0,"listSize").size(); }
    private @NotNull @Unmodifiable Object listIsEmpty(List<Object> a) { return listArg(a,0,"listIsEmpty").isEmpty(); }
    private @Nullable Object listClear(List<Object> a)   { listArg(a,0,"listClear").clear(); return null; }

    private @Nullable Object listAdd(List<Object> a) {
        listArg(a,0,"listAdd").add(a.size() > 1 ? a.get(1) : null);
        return null;
    }

    private @Nullable Object listAddAt(List<Object> a) {
        List<Object> list = listArg(a,0,"listAddAt");
        int i = intArg(a,1,"listAddAt");
        list.add(i, a.get(2));
        return null;
    }

    private @Nullable Object listRemove(List<Object> a) {
        List<Object> list = listArg(a,0,"listRemove");
        int i = intArg(a,1,"listRemove");
        checkBounds(list, i, "listRemove");
        list.remove(i);
        return null;
    }

    private Object listGet(List<Object> a) {
        List<Object> list = listArg(a,0,"listGet");
        int i = intArg(a,1,"listGet");
        checkBounds(list, i, "listGet");
        return list.get(i);
    }

    private @Nullable Object listSet(List<Object> a) {
        List<Object> list = listArg(a,0,"listSet");
        int i = intArg(a,1,"listSet");
        checkBounds(list, i, "listSet");
        list.set(i, a.get(2));
        return null;
    }

    private @NotNull @Unmodifiable Object listContains(List<Object> a) { return listArg(a,0,"listContains").contains(a.get(1)); }
    private Object listIndexOf(List<Object> a)  { return (double) listArg(a,0,"listIndexOf").indexOf(a.get(1)); }

    private Object listFirst(List<Object> a) {
        List<Object> list = listArg(a,0,"listFirst");
        if (list.isEmpty()) throw new LarvError("listFirst(): list is empty", -1, LarvError.Kind.RUNTIME);
        return list.getFirst();
    }

    private Object listLast(List<Object> a) {
        List<Object> list = listArg(a,0,"listLast");
        if (list.isEmpty()) throw new LarvError("listLast(): list is empty", -1, LarvError.Kind.RUNTIME);
        return list.getLast();
    }

    private Object listPop(List<Object> a) {
        List<Object> list = listArg(a,0,"listPop");
        if (list.isEmpty()) throw new LarvError("listPop(): list is empty", -1, LarvError.Kind.RUNTIME);
        return list.removeLast();
    }

    @Contract("_ -> new")
    private @NotNull Object listSlice(List<Object> a) {
        List<Object> list = listArg(a,0,"listSlice");
        int from = intArg(a,1,"listSlice"), to = intArg(a,2,"listSlice");
        if (from < 0 || to > list.size() || from > to)
            throw new LarvError("listSlice(): invalid range [" + from + ", " + to + ")", -1, LarvError.Kind.RUNTIME);
        return new ArrayList<>(list.subList(from, to));
    }

    private @NotNull Object listReverse(List<Object> a) {
        List<Object> copy = new ArrayList<>(listArg(a,0,"listReverse"));
        Collections.reverse(copy);
        return copy;
    }

    private @NotNull Object listSort(List<Object> a) {
        List<Object> copy = new ArrayList<>(listArg(a,0,"listSort"));
        copy.sort((x, y) -> {
            if (x instanceof Double dx && y instanceof Double dy) return Double.compare(dx, dy);
            return x.toString().compareTo(y.toString());
        });
        return copy;
    }

    private @NotNull Object listConcat(List<Object> a) {
        List<Object> out = new ArrayList<>(listArg(a,0,"listConcat"));
        out.addAll(listArg(a,1,"listConcat"));
        return out;
    }

    private @NotNull Object listFlat(List<Object> a) {
        List<Object> out = new ArrayList<>();
        for (Object item : listArg(a,0,"listFlat")) {
            if (item instanceof List<?> inner) out.addAll((List<Object>) inner);
            else out.add(item);
        }
        return out;
    }

    private @NotNull Object listUnique(List<Object> a) {
        List<Object> out = new ArrayList<>();
        for (Object item : listArg(a,0,"listUnique"))
            if (!out.contains(item)) out.add(item);
        return out;
    }

    private @NotNull Object listFill(@NotNull List<Object> a) {
        Object val = a.getFirst();
        int n = intArg(a,1,"listFill");
        List<Object> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(val);
        return out;
    }

    private @Nullable Object listShuffle(List<Object> a) {
        Collections.shuffle(listArg(a,0,"listShuffle"));
        return null;
    }
}