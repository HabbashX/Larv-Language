package com.habbashx.larv.signal;

/**
 * Control-flow exception thrown by a {@code break} statement to exit the
 * nearest enclosing loop.
 *
 * <p>{@link com.habbashx.larv.runtime.LoopExecutor} catches this signal and
 * terminates the loop iteration without further processing.</p>
 */
public class BreakSignal extends RuntimeException {
}
