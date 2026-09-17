package com.habbashx.larv.compiler;

import org.objectweb.asm.ClassWriter;

/**
 * ASM {@link ClassWriter} that tolerates references to Larv classes which do
 * not exist on the classpath yet.
 *
 * <p>With {@code COMPUTE_FRAMES}, ASM computes stack-map frames by loading
 * the classes involved in each branch merge ({@code getCommonSuperClass})
 * via {@code Class.forName}.  Any method whose control flow merges two
 * distinct <em>generated</em> Larv types on the stack — e.g. a variable that
 * holds a {@code NumberNode} on one path and a {@code BinaryOpNode} on
 * another — used to crash compilation with
 * {@code TypeNotPresentException: Type BinaryOpNode not present}, which made
 * every multi-class file uncompilable in {@code --run} / compiled mode.</p>
 *
 * <p>This override reproduces ASM's default algorithm when both classes are
 * loadable and falls back to {@code java/lang/Object} otherwise.
 * {@code Object} is always a sound common supertype (just less precise), so
 * the emitted frames always verify.</p>
 */
public class LarvClassWriter extends ClassWriter {

    public LarvClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) return type1;
        if (type1.equals("java/lang/Object") || type2.equals("java/lang/Object")) {
            return "java/lang/Object";
        }
        // Array descriptors are not loadable via dotted names — Object is safe.
        if (type1.charAt(0) == '[' || type2.charAt(0) == '[') {
            return "java/lang/Object";
        }
        try {
            ClassLoader loader = getClassLoader();
            Class<?> c1 = Class.forName(type1.replace('/', '.'), false, loader);
            Class<?> c2 = Class.forName(type2.replace('/', '.'), false, loader);
            if (c1.isAssignableFrom(c2)) return type1;
            if (c2.isAssignableFrom(c1)) return type2;
            if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
            do {
                c1 = c1.getSuperclass();
            } while (c1 != null && !c1.isAssignableFrom(c2));
            if (c1 == null) return "java/lang/Object";
            return c1.getName().replace('.', '/');
        } catch (Exception | LinkageError e) {
            // Generated Larv classes are not on the classpath yet.
            return "java/lang/Object";
        }
    }
}
