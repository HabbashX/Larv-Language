package com.habbashx.larv.error;

import com.habbashx.larv.compiler.exception.CompileException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central error-reporting façade for the Larv toolchain.
 *
 * <p>Every entry point (interpreter, compiler CLI) should route all exceptions
 * through {@link #report} instead of printing raw Java stack traces or calling
 * {@code e.getMessage()} directly.  This class translates every known exception
 * type into a rich {@link LarvError} diagnostic, falls back gracefully for
 * unexpected exceptions, and always prints to {@code System.err}.</p>
 *
 * <h2>Error hierarchy handled (in order)</h2>
 * <ol>
 *   <li>{@link LarvError} (and all subclasses: {@link CompileException},
 *       {@code ParseException}, {@code LarvRuntimeException}) — formatted
 *       directly; no wrapping needed.</li>
 *   <li>{@link StackOverflowError} — translated to a helpful runtime diagnostic.</li>
 *   <li>{@link OutOfMemoryError} — translated to a helpful runtime diagnostic.</li>
 *   <li>Any other {@link Throwable} — sanitised "internal error" message without
 *       exposing a raw Java stack trace to the user.</li>
 * </ol>
 *
 * <p>{@code LarvRuntimeException} used to extend {@link RuntimeException}
 * directly, causing it to fall into the "unexpected Java exception" bucket.
 * It now extends {@link LarvError}, so it is correctly caught by the first
 * branch and formatted with full diagnostic quality.</p>
 *
 * <h2>Example usage</h2>
 * <pre>
 *   try {
 *       interpreter.execute(ast);
 *   } catch (Throwable t) {
 *       ErrorReporter.report(t, source, "main.larv");
 *       System.exit(1);
 *   }
 * </pre>
 *
 * <h2>Error counting</h2>
 * <p>Call {@link #errorCount()} after a reporting pass to check how many
 * errors were emitted.  Use {@link #resetCounts()} between independent
 * compilation units.</p>
 */
public final class ErrorReporter {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM    = "\u001B[2m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BLUE   = "\u001B[34m";

    private static final AtomicInteger errorCount   = new AtomicInteger(0);
    private static final AtomicInteger warningCount = new AtomicInteger(0);

    /** Returns the number of errors reported since the last {@link #resetCounts()}. */
    public static int errorCount()   { return errorCount.get(); }

    /** Returns the number of warnings reported since the last {@link #resetCounts()}. */
    public static int warningCount() { return warningCount.get(); }

    /** Resets both error and warning counters to zero. */
    public static void resetCounts() {
        errorCount.set(0);
        warningCount.set(0);
    }

    private ErrorReporter() {}


    /**
     * Translates {@code t} into a Larv diagnostic and prints it to stderr.
     *
     * @param t          the thrown exception
     * @param sourceText the full source text of the file being executed/compiled
     *                   (may be {@code null} — snippet section is skipped)
     * @param sourceFile the source file name shown in the pointer (may be {@code null})
     */
    public static void report(Throwable t, String sourceText, String sourceFile) {
        t = unwrapReflective(t);

        errorCount.incrementAndGet();

        if (t instanceof LarvError le) {
            printLarvError(le, sourceText, sourceFile);

        } else if (t instanceof StackOverflowError) {
            printInternalError(
                    "stack overflow — infinite recursion detected",
                    "check for a recursive function that never reaches its base case",
                    sourceFile
            );

        } else if (t instanceof OutOfMemoryError) {
            printInternalError(
                    "out of memory — the program used too much heap space",
                    "reduce the size of your data structures or recursive depth",
                    sourceFile
            );

        } else {
            printUnexpectedError(t, sourceFile);
        }
    }

    /** Convenience overload without source text. */
    public static void report(Throwable t, String sourceFile) {
        report(t, null, sourceFile);
    }

    /** Convenience overload without any file context. */
    public static void report(Throwable t) {
        report(t, null, null);
    }

    /**
     * Reports a warning (does not increment {@link #errorCount()}).
     *
     * @param message    the warning message
     * @param sourceFile the source file name (may be {@code null})
     */
    public static void warn(String message, String sourceFile) {
        warningCount.incrementAndGet();
        StringBuilder sb = new StringBuilder("\n");
        sb.append(BOLD).append(YELLOW).append("warning").append(RESET)
                .append(BOLD).append(": ").append(message).append(RESET).append("\n");
        if (sourceFile != null && !sourceFile.isBlank()) {
            sb.append(BLUE).append(" --> ").append(RESET).append(sourceFile).append("\n");
        }
        System.err.print(sb);
    }

    /**
     * Prints a summary line after all diagnostics have been emitted, e.g.:
     * <pre>
     *   error: aborting due to 3 errors
     * </pre>
     * Only prints when there is at least one error or warning.
     */
    public static void printSummary() {
        int errs  = errorCount.get();
        int warns = warningCount.get();
        if (errs == 0 && warns == 0) return;

        StringBuilder sb = new StringBuilder("\n");
        if (errs > 0) {
            sb.append(BOLD).append(RED).append("error").append(RESET)
                    .append(": aborting due to ")
                    .append(errs).append(errs == 1 ? " error" : " errors");
            if (warns > 0) {
                sb.append("; ").append(warns).append(warns == 1 ? " warning" : " warnings").append(" emitted");
            }
        } else {
            sb.append(BOLD).append(YELLOW).append("warning").append(RESET)
                    .append(": ").append(warns).append(warns == 1 ? " warning" : " warnings").append(" emitted");
        }
        sb.append(RESET).append("\n");
        System.err.print(sb);
    }

    private static void printLarvError(LarvError le, String sourceText, String sourceFile) {
        if (sourceText != null) le.withSource(sourceText);
        if (sourceFile != null) le.withFile(sourceFile);
        if (!le.hasLocation()) attachJvmFrames(le);
        System.err.print(le.format());
        printErrorFooter(le.errorCode());
    }

    /**
     * Last-resort location recovery: when a {@link LarvError} carries no
     * source line and no Larv call-stack frames (typical for faults raised
     * deep inside compiled-mode runtime helpers), scan its JVM stack trace
     * for application frames — generated Larv classes and user Java code —
     * and record them as Larv trace frames.  Frames from the Larv toolchain
     * itself ({@code com.habbashx.larv.*}), the JDK, and common libraries are
     * skipped so the result reads as a Larv stack trace, not a Java one.
     */
    private static void attachJvmFrames(LarvError le) {
        int added = 0;
        for (StackTraceElement el : le.getStackTrace()) {
            if (added >= 24) break;
            if (!isAppFrame(el)) continue;
            String cls = el.getClassName();
            String simple = cls.substring(cls.lastIndexOf('.') + 1);
            le.addTraceFrame(simple + "." + el.getMethodName(), el.getLineNumber());
            added++;
        }
    }

    /**
     * Returns {@code true} for stack frames that belong to user code:
     * compiled Larv classes or user Java code.  Toolchain, JDK and
     * third-party frames are excluded.  Native methods (line {@code -2})
     * and unknown locations ({@code <= 0}) are excluded as well.
     */
    private static boolean isAppFrame(StackTraceElement el) {
        if (el.getLineNumber() <= 0) return false;
        String cls = el.getClassName();
        return !(cls.startsWith("java.")
                || cls.startsWith("jdk.")
                || cls.startsWith("sun.")
                || cls.startsWith("com.sun.")
                || cls.startsWith("com.habbashx.larv.")
                || cls.startsWith("org.objectweb.")
                || cls.startsWith("org.jetbrains.")
                || cls.startsWith("com.fasterxml.")
                || cls.startsWith("com.google.")
                || cls.startsWith("kotlin."));
    }

    private static void printInternalError(String msg, String hint, String sourceFile) {
        LarvError err = new LarvError(msg, -1, -1, LarvError.Kind.RUNTIME)
                .withHint(hint)
                .withFile(sourceFile);
        System.err.print(err.format());
        printErrorFooter(err.errorCode());
    }

    private static void printUnexpectedError(Throwable t, String sourceFile) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append(BOLD).append(RED).append("internal error").append(RESET).append(BOLD)
                .append(": an unexpected Java exception escaped the Larv runtime").append(RESET).append("\n");

        if (sourceFile != null && !sourceFile.isBlank()) {
            sb.append(BLUE).append(" --> ").append(RESET).append(sourceFile).append("\n");
        }
        Throwable current = t;
        int depth = 0;
        while (current != null && depth < 5) {
            String javaType = current.getClass().getName();
            String javaMsg  = current.getMessage() != null ? current.getMessage() : "(no message)";
            String prefix   = depth == 0 ? "     caused by: " : "     caused by: ";
            sb.append("\n");
            sb.append(DIM).append(prefix).append(javaType).append(RESET).append("\n");
            sb.append(DIM).append("     message:    ").append(javaMsg).append(RESET).append("\n");
            current = current.getCause();
            depth++;
        }

        sb.append("\n");
        sb.append(CYAN).append(BOLD)
                .append("     = help: ").append(RESET)
                .append("this is likely a bug in the Larv runtime — please report it at https://github.com/habbashx/larv/issues")
                .append("\n");

        sb.append(formatAppFrames(t));

        System.err.print(sb);
        printErrorFooter("E001");
    }

    /**
     * Renders the application frames of an unexpected exception's JVM stack
     * (most recent call first), so even unclassified crashes show where in
     * Larv-compiled or user Java code they originated instead of only a bare
     * exception name.  Returns an empty string when no application frames
     * exist (pure toolchain failure).
     */
    private static @NotNull String formatAppFrames(Throwable t) {
        StringBuilder sb = new StringBuilder();
        boolean headerDone = false;
        int shown = 0;
        for (StackTraceElement el : t.getStackTrace()) {
            if (shown >= 24) break;
            if (!isAppFrame(el)) continue;
            if (!headerDone) {
                sb.append(DIM).append("     application frames (most recent call first):").append(RESET).append("\n");
                headerDone = true;
            }
            String cls = el.getClassName();
            String simple = cls.substring(cls.lastIndexOf('.') + 1);
            sb.append(DIM).append("       at ").append(RESET)
                    .append(BOLD).append(simple).append(".").append(el.getMethodName()).append(RESET)
                    .append(DIM).append(" (")
                    .append(el.getFileName() != null ? el.getFileName() : "<unknown>")
                    .append(":").append(el.getLineNumber()).append(")").append(RESET)
                    .append("\n");
            shown++;
        }
        return sb.toString();
    }

    /**
     * Prints a brief "aborting due to previous error" footer, similar to rustc.
     */
    private static void printErrorFooter(String code) {
        System.err.println(
                BOLD + RED + "\naborting" + RESET +
                        " due to previous error " +
                        DIM + "[" + code + "]" + RESET
        );
    }

    /**
     * Recursively unwraps {@link java.lang.reflect.InvocationTargetException} and
     * {@link java.lang.reflect.UndeclaredThrowableException} — the reflective
     * wrappers that Java inserts when a method/constructor called via reflection
     * (or a MethodHandle) throws.  Their {@code getMessage()} returns {@code null},
     * which produces the baffling "internal error: (no message)" output.
     *
     * <p>By unwrapping here, at the top of the reporting pipeline, we guarantee
     * the real cause is what gets formatted — even if an inner catch block forgot
     * to unwrap it itself.</p>
     */
    private static @NotNull Throwable unwrapReflective(@NotNull Throwable t) {
        while ((t instanceof java.lang.reflect.InvocationTargetException ||
                t instanceof java.lang.reflect.UndeclaredThrowableException)
                && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
