package com.habbashx.larv.compiler.classloader;

import com.habbashx.larv.compiler.CompiledClass;
import com.habbashx.larv.compiler.LarvCompilerMain;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom {@link ClassLoader} that loads compiled Larv classes from their
 * in-memory bytecode as well as from the output directory on disk.
 *
 * <p>Used by {@link LarvCompilerMain} when {@code --run} is specified to
 * immediately execute the compiled program without writing to the classpath.</p>
 *
 * <p>The runtime support classes ({@code LarvRuntime}, {@code LarvObject},
 * {@code LarvRuntimeException}) are loaded from the enclosing classloader
 * so that they share the same definition with the compiler itself.</p>
 */
public class LarvClassLoader extends ClassLoader {

    /** In-memory bytecode keyed by class name (dot-separated). */
    private final Map<String, byte[]> classes = new HashMap<>();

    /** Fallback directory for classes that weren't held in-memory. */
    private final Path classDir;

    public LarvClassLoader(@NotNull List<CompiledClass> compiled, Path classDir) {
        super(LarvClassLoader.class.getClassLoader());
        this.classDir = classDir;
        for (final CompiledClass cc : compiled) {
            classes.put(cc.name(), cc.bytecode());
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if (bytes != null) {
            return defineClass(name, bytes, 0, bytes.length);
        }

        Path classFile = classDir.resolve(name + ".class");
        if (classFile.toFile().exists()) {
            try {
                bytes = Files.readAllBytes(classFile);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException("Failed to read: " + classFile, e);
            }
        }
        throw new ClassNotFoundException(name);
    }
}