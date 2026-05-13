package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * IO standard library for Larv.
 *
 * Available functions:
 *
 *   readFile(path)              → string   — read entire file as a string
 *   writeFile(path, content)    → nil      — write string to file (overwrite)
 *   appendFile(path, content)   → nil      — append string to file
 *   readLines(path)             → array    — read file as array of lines
 *   readBytes(path)             → array    — read file as array of byte numbers
 *   writeBytes(path, array)     → nil      — write array of byte numbers to file
 *   deleteFile(path)            → boolean  — delete a file, returns true if deleted
 *   fileExists(path)            → boolean  — check if a file exists
 *   isDir(path)                 → boolean  — check if path is a directory
 *   listDir(path)               → array    — list entries in a directory
 *   makeDir(path)               → nil      — create directory (and parents)
 *   copyFile(src, dst)          → nil      — copy a file
 *   moveFile(src, dst)          → nil      — move/rename a file
 *   fileSize(path)              → number   — file size in bytes
 *   cwd()                       → string   — current working directory
 *   absPath(path)               → string   — resolve to absolute path
 */
public class NativeIoLibrary implements NativeLibrary {

    private final ExecutionContext context;

    public NativeIoLibrary(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public void registerAll() {
        context.registerNative("readFile",   this::readFile);
        context.registerNative("writeFile",  this::writeFile);
        context.registerNative("appendFile", this::appendFile);
        context.registerNative("readLines",  this::readLines);
        context.registerNative("readBytes",  this::readBytes);
        context.registerNative("writeBytes", this::writeBytes);
        context.registerNative("deleteFile", this::deleteFile);
        context.registerNative("fileExists", this::fileExists);
        context.registerNative("isDir",      this::isDir);
        context.registerNative("listDir",    this::listDir);
        context.registerNative("makeDir",    this::makeDir);
        context.registerNative("copyFile",   this::copyFile);
        context.registerNative("moveFile",   this::moveFile);
        context.registerNative("fileSize",   this::fileSize);
        context.registerNative("cwd",        this::cwd);
        context.registerNative("absPath",    this::absPath);
    }


    private @NotNull Path pathArg(@NotNull List<Object> args, int index, String fnName) {
        if (args.size() <= index || !(args.get(index) instanceof String s))
            throw new LarvError(fnName + "() expects a string path as argument " + (index + 1), -1, LarvError.Kind.RUNTIME);
        return Path.of(s);
    }

    private String stringArg(@NotNull List<Object> args, int index, String fnName) {
        if (args.size() <= index || !(args.get(index) instanceof String s))
            throw new LarvError(fnName + "() expects a string as argument " + (index + 1), -1, LarvError.Kind.RUNTIME);
        return s;
    }


    private @NotNull @Unmodifiable Object readFile(@NotNull List<Object> args) {
        try {
            return Files.readString(pathArg(args, 0, "readFile"));
        } catch (IOException e) {
            throw new LarvError("readFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object writeFile(@NotNull List<Object> args) {
        try {
            Files.writeString(pathArg(args, 0, "writeFile"), stringArg(args, 1, "writeFile"));
            return null;
        } catch (IOException e) {
            throw new LarvError("writeFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object appendFile(@NotNull List<Object> args) {
        try {
            Files.writeString(
                    pathArg(args, 0, "appendFile"),
                    stringArg(args, 1, "appendFile"),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
            return null;
        } catch (IOException e) {
            throw new LarvError("appendFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    @Contract("_ -> new")
    private @NotNull Object readLines(@NotNull List<Object> args) {
        try {
            List<String> lines = Files.readAllLines(pathArg(args, 0, "readLines"));
            return new ArrayList<>(lines);
        } catch (IOException e) {
            throw new LarvError("readLines() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull Object readBytes(@NotNull List<Object> args) {
        try {
            byte[] bytes = Files.readAllBytes(pathArg(args, 0, "readBytes"));
            List<Object> result = new ArrayList<>(bytes.length);
            for (byte b : bytes) result.add((double) (b & 0xFF));
            return result;
        } catch (IOException e) {
            throw new LarvError("readBytes() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object writeBytes(@NotNull List<Object> args) {
        Path path = pathArg(args, 0, "writeBytes");
        if (!(args.get(1) instanceof List<?> list))
            throw new LarvError("writeBytes() expects an array as second argument", -1, LarvError.Kind.RUNTIME);
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = ((Double) list.get(i)).byteValue();
        }
        try {
            Files.write(path, bytes);
            return null;
        } catch (IOException e) {
            throw new LarvError("writeBytes() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object deleteFile(@NotNull List<Object> args) {
        try {
            return Files.deleteIfExists(pathArg(args, 0, "deleteFile"));
        } catch (IOException e) {
            throw new LarvError("deleteFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @NotNull @Unmodifiable Object fileExists(@NotNull List<Object> args) {
        return Files.exists(pathArg(args, 0, "fileExists"));
    }

    private @NotNull @Unmodifiable Object isDir(@NotNull List<Object> args) {
        return Files.isDirectory(pathArg(args, 0, "isDir"));
    }

    private @NotNull Object listDir(@NotNull List<Object> args) {
        try (var stream = Files.list(pathArg(args, 0, "listDir"))) {
            List<Object> result = new ArrayList<>();
            stream.forEach(p -> result.add(p.toString()));
            return result;
        } catch (IOException e) {
            throw new LarvError("listDir() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object makeDir(@NotNull List<Object> args) {
        try {
            Files.createDirectories(pathArg(args, 0, "makeDir"));
            return null;
        } catch (IOException e) {
            throw new LarvError("makeDir() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object copyFile(@NotNull List<Object> args) {
        try {
            Files.copy(pathArg(args, 0, "copyFile"), pathArg(args, 1, "copyFile"), StandardCopyOption.REPLACE_EXISTING);
            return null;
        } catch (IOException e) {
            throw new LarvError("copyFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object moveFile(@NotNull List<Object> args) {
        try {
            Files.move(pathArg(args, 0, "moveFile"), pathArg(args, 1, "moveFile"), StandardCopyOption.REPLACE_EXISTING);
            return null;
        } catch (IOException e) {
            throw new LarvError("moveFile() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private Object fileSize(@NotNull List<Object> args) {
        try {
            return (double) Files.size(pathArg(args, 0, "fileSize"));
        } catch (IOException e) {
            throw new LarvError("fileSize() failed: " + e.getMessage(), -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Unmodifiable Object cwd(List<Object> args) {
        return System.getProperty("user.dir");
    }

    private @NotNull @Unmodifiable Object absPath(@NotNull List<Object> args) {
        return pathArg(args, 0, "absPath").toAbsolutePath().toString();
    }
}