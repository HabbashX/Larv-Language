package com.habbashx.larv.runtime;


import com.habbashx.larv.signal.BreakSignal;
import com.habbashx.larv.signal.ContinueSignal;

import java.util.List;
import com.habbashx.larv.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the shared execution mechanics for all loop constructs.
 *
 * <p>{@code LoopExecutor} separates loop control-flow concerns ({@code break},
 * {@code continue}, condition re-evaluation) from the statement executor's
 * visit methods, keeping each component focused on a single responsibility.</p>
 *
 * <h2>Signal protocol</h2>
 * <p>Both loop types use a signal-via-exception protocol:
 * <ul>
 *   <li>{@link BreakSignal}    — caught here; exits the loop immediately.</li>
 *   <li>{@link ContinueSignal} — caught here; skips the rest of the body and
 *       runs the post-step before re-testing the condition.</li>
 * </ul>
 * Any other exception propagates up unchanged.</p>
 */
public class LoopExecutor {

    /**
     * Executes a block of statements in their own scope.
     * Provided by {@link StatementExecutor#runBlock}.
     */
    @FunctionalInterface
    public interface BlockRunner {
        /**
         * @param body the list of statements to run as a scoped block
         */
        void run(List<Statement> body);
    }

    /**
     * A boolean-yielding guard evaluated before each loop iteration.
     */
    @FunctionalInterface
    public interface Condition {
        /**
         * @return {@code true} while the loop should continue
         */
        boolean test();
    }

    /**
     * An action executed after each loop body (the "increment" step).
     * Used only by {@code for} loops; {@code while} loops pass a no-op.
     */
    @FunctionalInterface
    public interface PostStep {
        /** Runs the post-iteration action. */
        void run();
    }

    private final BlockRunner blockRunner;

    /**
     * Constructs a {@code LoopExecutor} wired to the given block runner.
     *
     * @param blockRunner strategy for executing a statement block in a new scope
     */
    public LoopExecutor(BlockRunner blockRunner) {
        this.blockRunner = blockRunner;
    }

    /**
     * Runs a {@code while} loop until the condition becomes falsy or a
     * {@code break} is encountered.
     *
     * @param condition the loop guard
     * @param body      the loop body
     */
    public void runWhile(Condition condition, List<Statement> body) {
        runLoop(condition, body, () -> {});
    }

    /**
     * Runs a {@code for} loop until the condition becomes falsy or a
     * {@code break} is encountered, executing {@code postStep} after each
     * body execution.
     *
     * @param condition the loop guard
     * @param body      the loop body
     * @param postStep  executed after each body (e.g. {@code i++})
     */
    public void runFor(Condition condition, List<Statement> body, PostStep postStep) {
        runLoop(condition, body, postStep);
    }

    // ── Core loop ─────────────────────────────────────────────────────────────

    /**
     * The shared loop kernel.
     *
     * <ol>
     *   <li>Evaluates {@code condition}; exits when falsy.</li>
     *   <li>Runs {@code blockRunner.run(body)} inside a scoped block.</li>
     *   <li>Catches {@link ContinueSignal} — resumes at {@code postStep}.</li>
     *   <li>Catches {@link BreakSignal}    — exits the loop entirely.</li>
     *   <li>Runs {@code postStep}.</li>
     * </ol>
     *
     * @param condition the loop guard
     * @param body      the loop body
     * @param postStep  the post-iteration step (no-op for {@code while})
     */
    private void runLoop(@NotNull Condition condition, List<Statement> body, PostStep postStep) {
        while (condition.test()) {
            try {
                blockRunner.run(body);
            } catch (ContinueSignal ignored) {
                // skip to post-step and re-evaluate condition
            } catch (BreakSignal ignored) {
                break;
            }
            postStep.run();
        }
    }
}
