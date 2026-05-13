package com.habbashx.larv.signal;

/**
 * Control-flow exception thrown by a {@code continue} statement to skip the
 * remainder of the current loop iteration.
 *
 * <p>{@link com.habbashx.larv.runtime.LoopExecutor} catches this signal,
 * skips the rest of the loop body, then runs the post-step (for {@code for}
 * loops) and re-evaluates the condition before the next iteration.</p>
 */
public class ContinueSignal extends RuntimeException {
}
