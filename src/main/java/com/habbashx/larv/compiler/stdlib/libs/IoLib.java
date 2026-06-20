package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class IoLib implements LarvStdlib {

    @Contract(pure = true)
    @Override
    public @NotNull String name() { return "io"; }

    @Override
    public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("readFile",    "readFile"),
                Map.entry("writeFile",   "writeFile"),
                Map.entry("appendFile",  "appendFile"),
                Map.entry("readLines",   "readLines"),
                Map.entry("readBytes",   "readBytes"),
                Map.entry("deleteFile",  "deleteFile"),
                Map.entry("fileExists",  "fileExists"),
                Map.entry("isDir",       "isDir"),
                Map.entry("listDir",     "listDir"),
                Map.entry("makeDir",     "makeDir"),
                Map.entry("copyFile",    "copyFile"),
                Map.entry("moveFile",    "moveFile"),
                Map.entry("fileSize",    "fileSize"),
                Map.entry("cwd",         "cwd"),
                Map.entry("absPath",     "absPath"),
                Map.entry("openWriter",  "openWriter"),
                Map.entry("writeLine",   "writeLine"),
                Map.entry("closeWriter", "closeWriter"),
                Map.entry("openReader",  "openReader"),
                Map.entry("readLine",    "readLine"),
                Map.entry("closeReader", "closeReader")
        );
    }

    private static final Map<String, BufferedWriter> WRITERS = new LinkedHashMap<>();
    private static final Map<String, BufferedReader> READERS = new LinkedHashMap<>();

    public static @NotNull @Unmodifiable Object readFile(Object path) {
        try { return Files.readString(Path.of(str(path, "readFile")), StandardCharsets.UTF_8); }
        catch (IOException e) { throw new LarvRuntimeException("readFile(): " + e.getMessage()); }
    }

    public static @Nullable Object writeFile(Object path, Object content) {
        try { Files.writeString(Path.of(str(path, "writeFile")), str(content, "writeFile"), StandardCharsets.UTF_8); return null; }
        catch (IOException e) { throw new LarvRuntimeException("writeFile(): " + e.getMessage()); }
    }

    public static @Nullable Object appendFile(Object path, Object content) {
        try {
            Files.writeString(Path.of(str(path, "appendFile")), str(content, "appendFile"),
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("appendFile(): " + e.getMessage()); }
    }

    public static @NotNull Object readLines(Object path) {
        try {
            List<Object> result = new ArrayList<>();
            Files.readAllLines(Path.of(str(path, "readLines")), StandardCharsets.UTF_8).forEach(result::add);
            return result;
        } catch (IOException e) { throw new LarvRuntimeException("readLines(): " + e.getMessage()); }
    }

    public static @NotNull Object readBytes(Object path) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(str(path, "readBytes")));
            List<Object> result = new ArrayList<>(bytes.length);
            for (byte b : bytes) result.add((double)(b & 0xFF));
            return result;
        } catch (IOException e) { throw new LarvRuntimeException("readBytes(): " + e.getMessage()); }
    }

    public static @NotNull @Unmodifiable Object deleteFile(Object path) {
        try { return Files.deleteIfExists(Path.of(str(path, "deleteFile"))); }
        catch (IOException e) { throw new LarvRuntimeException("deleteFile(): " + e.getMessage()); }
    }

    public static @NotNull @Unmodifiable Object fileExists(Object path) { return Files.exists(Path.of(str(path, "fileExists"))); }

    public static @NotNull @Unmodifiable Object isDir(Object path) { return Files.isDirectory(Path.of(str(path, "isDir"))); }

    public static @NotNull Object listDir(Object path) {
        try {
            List<Object> result = new ArrayList<>();
            Files.list(Path.of(str(path, "listDir"))).forEach(p -> result.add(p.toString()));
            return result;
        } catch (IOException e) { throw new LarvRuntimeException("listDir(): " + e.getMessage()); }
    }

    public static @Nullable Object makeDir(Object path) {
        try { Files.createDirectories(Path.of(str(path, "makeDir"))); return null; }
        catch (IOException e) { throw new LarvRuntimeException("makeDir(): " + e.getMessage()); }
    }

    public static @Nullable Object copyFile(Object src, Object dst) {
        try { Files.copy(Path.of(str(src, "copyFile")), Path.of(str(dst, "copyFile")), StandardCopyOption.REPLACE_EXISTING); return null; }
        catch (IOException e) { throw new LarvRuntimeException("copyFile(): " + e.getMessage()); }
    }

    public static @Nullable Object moveFile(Object src, Object dst) {
        try { Files.move(Path.of(str(src, "moveFile")), Path.of(str(dst, "moveFile")), StandardCopyOption.REPLACE_EXISTING); return null; }
        catch (IOException e) { throw new LarvRuntimeException("moveFile(): " + e.getMessage()); }
    }

    public static Object fileSize(Object path) {
        try { return (double) Files.size(Path.of(str(path, "fileSize"))); }
        catch (IOException e) { throw new LarvRuntimeException("fileSize(): " + e.getMessage()); }
    }

    public static @Unmodifiable Object cwd()  { return System.getProperty("user.dir"); }

    public static @NotNull @Unmodifiable Object absPath(Object path) { return Path.of(str(path, "absPath")).toAbsolutePath().toString(); }


    public static @Nullable Object openWriter(Object path) {
        String p = str(path, "openWriter");
        try { WRITERS.put(p, new BufferedWriter(new FileWriter(p))); return null; }
        catch (IOException e) { throw new LarvRuntimeException("openWriter(): " + e.getMessage()); }
    }

    public static @Nullable Object writeLine(Object path, Object line) {
        String p = str(path, "writeLine");
        BufferedWriter w = WRITERS.get(p);
        if (w == null) throw new LarvRuntimeException("writeLine(): no open writer for: " + p);
        try { w.write(String.valueOf(line)); w.newLine(); return null; }
        catch (IOException e) { throw new LarvRuntimeException("writeLine(): " + e.getMessage()); }
    }

    public static @Nullable Object closeWriter(Object path) {
        String p = str(path, "closeWriter");
        BufferedWriter w = WRITERS.remove(p);
        if (w == null) throw new LarvRuntimeException("closeWriter(): no open writer for: " + p);
        try { w.close(); return null; }
        catch (IOException e) { throw new LarvRuntimeException("closeWriter(): " + e.getMessage()); }
    }

    public static @Nullable Object openReader(Object path) {
        String p = str(path, "openReader");
        try { READERS.put(p, new BufferedReader(new FileReader(p))); return null; }
        catch (IOException e) { throw new LarvRuntimeException("openReader(): " + e.getMessage()); }
    }

    public static @Unmodifiable Object readLine(Object path) {
        String p = str(path, "readLine");
        BufferedReader r = READERS.get(p);
        if (r == null) throw new LarvRuntimeException("readLine(): no open reader for: " + p);
        try { return r.readLine(); }
        catch (IOException e) { throw new LarvRuntimeException("readLine(): " + e.getMessage()); }
    }

    public static @Nullable Object closeReader(Object path) {
        String p = str(path, "closeReader");
        BufferedReader r = READERS.remove(p);
        if (r == null) throw new LarvRuntimeException("closeReader(): no open reader for: " + p);
        try { r.close(); return null; }
        catch (IOException e) { throw new LarvRuntimeException("closeReader(): " + e.getMessage()); }
    }

    @Contract("null, _ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s)) throw new LarvRuntimeException(fn + "(): argument must be a string");
        return s;
    }
}