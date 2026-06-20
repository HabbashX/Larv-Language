package com.habbashx.larv.compiler.runtime;

/**
 * Internal exception used exclusively to signal that an argument signature
 * does not match a constructor, prompting the engine to check the next overload.
 */
public class LarvTypeMismatchException extends RuntimeException {
    public LarvTypeMismatchException(String msg) { super(msg); }
}