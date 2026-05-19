package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex standard library — import "regex"
 *
 *   regexMatch(str, pattern)           → boolean  true if str fully matches pattern
 *   regexTest(str, pattern)            → boolean  true if pattern found anywhere in str
 *   regexFind(str, pattern)            → string   first match, or nil if not found
 *   regexFindAll(str, pattern)         → array    all non-overlapping matches
 *   regexReplace(str, pattern, repl)   → string   replace first match
 *   regexReplaceAll(str, pattern, repl)→ string   replace all matches
 *   regexSplit(str, pattern)           → array    split str by pattern
 *   regexGroup(str, pattern, group)    → string   capture group by index (1-based)
 *   regexGroups(str, pattern)          → array    all capture groups of first match
 */
@Native("Regular Expression Library [RegEx]")
public class NativeRegexLibrary implements NativeLibrary {

    private final ExecutionContext context;

    public NativeRegexLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("regexMatch",      this::regexMatch);
        context.registerNative("regexTest",       this::regexTest);
        context.registerNative("regexFind",       this::regexFind);
        context.registerNative("regexFindAll",    this::regexFindAll);
        context.registerNative("regexReplace",    this::regexReplace);
        context.registerNative("regexReplaceAll", this::regexReplaceAll);
        context.registerNative("regexSplit",      this::regexSplit);
        context.registerNative("regexGroup",      this::regexGroup);
        context.registerNative("regexGroups",     this::regexGroups);
    }

    private String strArg(@NotNull List<Object> args, int i, String fn) {
        if (args.size() <= i || !(args.get(i) instanceof String s))
            throw new LarvError(fn + "(): argument " + (i + 1) + " must be a string", -1, LarvError.Kind.RUNTIME);
        return s;
    }

    private @NotNull Pattern compile(String pattern, String fn) {
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new LarvError(fn + "(): invalid regex pattern: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object regexMatch(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexMatch");
        String pattern = strArg(args, 1, "regexMatch");
        return compile(pattern, "regexMatch").matcher(input).matches();
    }

    private @NotNull @Unmodifiable Object regexTest(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexTest");
        String pattern = strArg(args, 1, "regexTest");
        return compile(pattern, "regexTest").matcher(input).find();
    }

    private @Nullable @Unmodifiable Object regexFind(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexFind");
        String pattern = strArg(args, 1, "regexFind");
        Matcher m = compile(pattern, "regexFind").matcher(input);
        return m.find() ? m.group() : null;
    }

    private @NotNull Object regexFindAll(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexFindAll");
        String pattern = strArg(args, 1, "regexFindAll");
        Matcher m = compile(pattern, "regexFindAll").matcher(input);
        List<Object> results = new ArrayList<>();
        while (m.find()) results.add(m.group());
        return results;
    }

    private @Unmodifiable Object regexReplace(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexReplace");
        String pattern = strArg(args, 1, "regexReplace");
        String repl    = strArg(args, 2, "regexReplace");
        return compile(pattern, "regexReplace").matcher(input).replaceFirst(repl);
    }

    private @Unmodifiable Object regexReplaceAll(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexReplaceAll");
        String pattern = strArg(args, 1, "regexReplaceAll");
        String repl    = strArg(args, 2, "regexReplaceAll");
        return compile(pattern, "regexReplaceAll").matcher(input).replaceAll(repl);
    }

    private @NotNull Object regexSplit(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexSplit");
        String pattern = strArg(args, 1, "regexSplit");
        String[] parts = compile(pattern, "regexSplit").split(input);
        List<Object> out = new ArrayList<>();
        for (String p : parts) out.add(p);
        return out;
    }

    private @Nullable @Unmodifiable Object regexGroup(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexGroup");
        String pattern = strArg(args, 1, "regexGroup");
        if (args.size() < 3 || !(args.get(2) instanceof Double d))
            throw new LarvError("regexGroup(): argument 3 must be a group index number", -1, LarvError.Kind.RUNTIME);
        int group = d.intValue();
        Matcher m = compile(pattern, "regexGroup").matcher(input);
        if (!m.find()) return null;
        try { return m.group(group); }
        catch (IndexOutOfBoundsException e) {
            throw new LarvError("regexGroup(): group " + group + " does not exist in pattern", -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull Object regexGroups(@NotNull List<Object> args) {
        String input   = strArg(args, 0, "regexGroups");
        String pattern = strArg(args, 1, "regexGroups");
        Matcher m = compile(pattern, "regexGroups").matcher(input);
        List<Object> out = new ArrayList<>();
        if (!m.find()) return out;
        for (int i = 1; i <= m.groupCount(); i++) {
            out.add(m.group(i));
        }
        return out;
    }
}