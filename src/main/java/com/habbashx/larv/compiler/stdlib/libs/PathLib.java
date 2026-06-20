package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class PathLib implements LarvStdlib {

    @Contract(pure = true)
    @Override public @NotNull String name() { return "path"; }

    @Override public @NotNull @Unmodifiable Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("pathJoin",         "path_pathJoin"),
                Map.entry("pathAbs",          "path_pathAbs"),
                Map.entry("pathNormalize",    "path_pathNormalize"),
                Map.entry("pathRelative",     "path_pathRelative"),
                Map.entry("pathParent",       "path_pathParent"),
                Map.entry("pathFileName",     "path_pathFileName"),
                Map.entry("pathStem",         "path_pathStem"),
                Map.entry("pathExt",          "path_pathExt"),
                Map.entry("pathExists",       "path_pathExists"),
                Map.entry("pathIsFile",       "path_pathIsFile"),
                Map.entry("pathIsDir",        "path_pathIsDir"),
                Map.entry("pathIsAbsolute",   "path_pathIsAbsolute"),
                Map.entry("pathIsRelative",   "path_pathIsRelative"),
                Map.entry("pathIsHidden",     "path_pathIsHidden"),
                Map.entry("pathIsSymlink",    "path_pathIsSymlink"),
                Map.entry("pathIsEmpty",      "path_pathIsEmpty"),
                Map.entry("pathParts",        "path_pathParts"),
                Map.entry("pathDepth",        "path_pathDepth"),
                Map.entry("pathStartsWith",   "path_pathStartsWith"),
                Map.entry("pathEndsWith",     "path_pathEndsWith"),
                Map.entry("pathWithExt",      "path_pathWithExt"),
                Map.entry("pathWithName",     "path_pathWithName"),
                Map.entry("pathWithStem",     "path_pathWithStem"),
                Map.entry("pathSize",         "path_pathSize"),
                Map.entry("pathCreated",      "path_pathCreated"),
                Map.entry("pathModified",     "path_pathModified"),
                Map.entry("pathList",         "path_pathList"),
                Map.entry("pathListAll",      "path_pathListAll"),
                Map.entry("pathGlob",         "path_pathGlob"),
                Map.entry("pathWalk",         "path_pathWalk"),
                Map.entry("pathMakeDir",      "path_pathMakeDir"),
                Map.entry("pathMakeDirs",     "path_pathMakeDirs"),
                Map.entry("pathDelete",       "path_pathDelete"),
                Map.entry("pathDeleteAll",    "path_pathDeleteAll"),
                Map.entry("pathCopy",         "path_pathCopy"),
                Map.entry("pathMove",         "path_pathMove"),
                Map.entry("pathSymlink",      "path_pathSymlink"),
                Map.entry("pathResolveSymlink","path_pathResolveSymlink"),
                Map.entry("pathCwd",          "path_pathCwd"),
                Map.entry("pathHome",         "path_pathHome"),
                Map.entry("pathTemp",         "path_pathTemp"),
                Map.entry("pathSep",          "path_pathSep"),
                Map.entry("pathSame",         "path_pathSame"),
                Map.entry("pathCommon",       "path_pathCommon"),
                Map.entry("pathStat",         "path_pathStat")
        );
    }

    /**
     * Join path segments together.
     * pathJoin(base, part1, part2, ...) — accepts any number of segments as a list
     * or individual strings.
     *
     *   pathJoin("/usr", "local", "bin")   → "/usr/local/bin"
     *   pathJoin("a/b", "../c")            → "a/c"  (after normalize)
     */
    public static @NotNull @Unmodifiable Object path_pathJoin(Object a) { return path_pathJoin(new Object[]{a}); }
    public static @NotNull @Unmodifiable Object path_pathJoin(Object a, Object b) { return path_pathJoin(new Object[]{a, b}); }
    public static @NotNull @Unmodifiable Object path_pathJoin(Object a, Object b, Object c) { return path_pathJoin(new Object[]{a, b, c}); }
    public static @NotNull @Unmodifiable Object path_pathJoin(Object a, Object b, Object c, Object d) { return path_pathJoin(new Object[]{a, b, c, d}); }
    public static @NotNull @Unmodifiable Object path_pathJoin(Object @NotNull ... args) {
        if (args.length == 0) throw new LarvRuntimeException("pathJoin(): at least one argument required");
        final List<String> parts;
        if (args.length == 1 && args[0] instanceof List<?> l) {
            parts = new ArrayList<>();
            for (final Object item : l) parts.add(str(item, "pathJoin"));
        } else {
            parts = new ArrayList<>();
            for (final Object arg : args) parts.add(str(arg, "pathJoin"));
        }
        if (parts.isEmpty()) throw new LarvRuntimeException("pathJoin(): no path segments provided");
        Path result = Path.of(parts.get(0));
        for (int i = 1; i < parts.size(); i++) result = result.resolve(parts.get(i));
        return result.toString();
    }

    /** Return the absolute path, resolving against cwd. */
    public static @NotNull @Unmodifiable Object path_pathAbs(final Object p) {
        return Path.of(str(p, "pathAbs")).toAbsolutePath().toString();
    }

    /** Remove redundant `.`, `..`, and double separators without touching the filesystem. */
    public static @NotNull @Unmodifiable Object path_pathNormalize(final Object p) {
        return Path.of(str(p, "pathNormalize")).normalize().toString();
    }

    /**
     * Return {@code target} expressed relative to {@code base}.
     * pathRelative(base, target)
     *
     *   pathRelative("/usr/local", "/usr/local/bin/node")  → "bin/node"
     */
    public static @NotNull @Unmodifiable Object path_pathRelative(final Object base, final Object target) {
        return Path.of(str(base, "pathRelative"))
                .relativize(Path.of(str(target, "pathRelative")))
                .toString();
    }

    /** Parent directory, or the path itself if it has no parent. */
    public static Object path_pathParent(final Object p) {
        final Path parent = Path.of(str(p, "pathParent")).getParent();
        return parent != null ? parent.toString() : str(p, "pathParent");
    }

    /** Last component of the path including extension. */
    public static @NotNull Object path_pathFileName(final Object p) {
        final Path fn = Path.of(str(p, "pathFileName")).getFileName();
        return fn != null ? fn.toString() : "";
    }

    /** Last component without extension (the stem). */
    public static @NotNull Object path_pathStem(final Object p) {
        final String name = Path.of(str(p, "pathStem")).getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** File extension including the dot, or empty string if none. */
    public static @NotNull Object path_pathExt(final Object p) {
        final String name = Path.of(str(p, "pathExt")).getFileName().toString();
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }

    /** All path components as a list. */
    public static @NotNull Object path_pathParts(final Object p) {
        final Path path = Path.of(str(p, "pathParts"));
        final List<Object> parts = new ArrayList<>();
        // Include root if present (e.g. "/" on Unix, "C:\" on Windows)
        if (path.getRoot() != null) parts.add(path.getRoot().toString());
        for (final Path part : path) parts.add(part.toString());
        return parts;
    }

    /** Number of components in the path (not counting root). */
    public static Object path_pathDepth(final Object p) {
        return (double) Path.of(str(p, "pathDepth")).getNameCount();
    }


    public static @NotNull @Unmodifiable Object path_pathExists(final Object p) {
        return Files.exists(Path.of(str(p, "pathExists")));
    }

    public static @NotNull @Unmodifiable Object path_pathIsFile(final Object p) {
        return Files.isRegularFile(Path.of(str(p, "pathIsFile")));
    }

    public static @NotNull @Unmodifiable Object path_pathIsDir(final Object p) {
        return Files.isDirectory(Path.of(str(p, "pathIsDir")));
    }

    public static @NotNull @Unmodifiable Object path_pathIsAbsolute(final Object p) {
        return Path.of(str(p, "pathIsAbsolute")).isAbsolute();
    }

    public static Object path_pathIsRelative(final Object p) {
        return !Path.of(str(p, "pathIsRelative")).isAbsolute();
    }

    public static @NotNull @Unmodifiable Object path_pathIsHidden(final Object p) {
        try { return Files.isHidden(Path.of(str(p, "pathIsHidden"))); }
        catch (IOException e) { throw new LarvRuntimeException("pathIsHidden(): " + e.getMessage()); }
    }

    public static @NotNull @Unmodifiable Object path_pathIsSymlink(final Object p) {
        return Files.isSymbolicLink(Path.of(str(p, "pathIsSymlink")));
    }

    /** True if the path is a directory and contains no entries, or is a file with zero bytes. */
    public static @NotNull Object path_pathIsEmpty(final Object p) {
        final Path path = Path.of(str(p, "pathIsEmpty"));
        try {
            if (Files.isDirectory(path)) {
                try (final var stream = Files.list(path)) { return stream.findFirst().isEmpty(); }
            }
            return Files.size(path) == 0;
        } catch (IOException e) { throw new LarvRuntimeException("pathIsEmpty(): " + e.getMessage()); }
    }

    /** True if {@code p} starts with the given prefix path. */
    public static @NotNull @Unmodifiable Object path_pathStartsWith(final Object p, final Object prefix) {
        return Path.of(str(p, "pathStartsWith")).startsWith(Path.of(str(prefix, "pathStartsWith")));
    }

    /** True if {@code p} ends with the given suffix path. */
    public static @NotNull @Unmodifiable Object path_pathEndsWith(final Object p, final Object suffix) {
        return Path.of(str(p, "pathEndsWith")).endsWith(Path.of(str(suffix, "pathEndsWith")));
    }

    /** True if two paths refer to the same filesystem entry. */
    public static @NotNull @Unmodifiable Object path_pathSame(final Object a, final Object b) {
        try { return Files.isSameFile(Path.of(str(a, "pathSame")), Path.of(str(b, "pathSame"))); }
        catch (IOException e) { throw new LarvRuntimeException("pathSame(): " + e.getMessage()); }
    }

    /** Return the path with its extension replaced. pathWithExt("/a/b.txt", ".md") → "/a/b.md" */
    public static @NotNull Object path_pathWithExt(final Object p, final Object ext) {
        final Path path = Path.of(str(p, "pathWithExt"));
        final String newExt = str(ext, "pathWithExt");
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        final String newName = (dot > 0 ? name.substring(0, dot) : name) + newExt;
        final Path parent = path.getParent();
        return parent != null ? parent.resolve(newName).toString() : newName;
    }

    /** Return the path with its filename replaced. pathWithName("/a/b.txt", "c.md") → "/a/c.md" */
    public static Object path_pathWithName(final Object p, final Object name) {
        final Path path = Path.of(str(p, "pathWithName"));
        final Path parent = path.getParent();
        return parent != null ? parent.resolve(str(name, "pathWithName")).toString()
                : str(name, "pathWithName");
    }

    /** Return the path with its stem replaced, keeping the extension. */
    public static @NotNull Object path_pathWithStem(final Object p, final Object stem) {
        final Path path = Path.of(str(p, "pathWithStem"));
        final String oldName = path.getFileName().toString();
        final int dot = oldName.lastIndexOf('.');
        final String ext = dot > 0 ? oldName.substring(dot) : "";
        final String newName = str(stem, "pathWithStem") + ext;
        final Path parent = path.getParent();
        return parent != null ? parent.resolve(newName).toString() : newName;
    }

    /**
     * Return the longest common ancestor of two paths.
     * pathCommon("/usr/local/bin", "/usr/local/lib")  → "/usr/local"
     */
    public static @NotNull Object path_pathCommon(final Object a, final Object b) {
        final Path pa = Path.of(str(a, "pathCommon")).toAbsolutePath().normalize();
        final Path pb = Path.of(str(b, "pathCommon")).toAbsolutePath().normalize();
        Path common = pa.getRoot();
        final int len = Math.min(pa.getNameCount(), pb.getNameCount());
        for (int i = 0; i < len; i++) {
            if (!pa.getName(i).equals(pb.getName(i))) break;
            common = (common != null)
                    ? common.resolve(pa.getName(i))
                    : pa.getName(i);
        }
        return common != null ? common.toString() : "";
    }

    /** File size in bytes. */
    public static Object path_pathSize(final Object p) {
        try { return (double) Files.size(Path.of(str(p, "pathSize"))); }
        catch (IOException e) { throw new LarvRuntimeException("pathSize(): " + e.getMessage()); }
    }

    /** Creation time in milliseconds since epoch. */
    public static Object path_pathCreated(final Object p) {
        try {
            final BasicFileAttributes attrs =
                    Files.readAttributes(Path.of(str(p, "pathCreated")), BasicFileAttributes.class);
            return (double) attrs.creationTime().toMillis();
        } catch (IOException e) { throw new LarvRuntimeException("pathCreated(): " + e.getMessage()); }
    }

    /** Last modified time in milliseconds since epoch. */
    public static Object path_pathModified(final Object p) {
        try { return (double) Files.getLastModifiedTime(Path.of(str(p, "pathModified"))).toMillis(); }
        catch (IOException e) { throw new LarvRuntimeException("pathModified(): " + e.getMessage()); }
    }

    /**
     * Full stat map for a path.
     * Returns: { size, created, modified, isFile, isDir, isSymlink, isHidden }
     */
    public static @NotNull Object path_pathStat(final Object p) {
        final Path path = Path.of(str(p, "pathStat"));
        try {
            final BasicFileAttributes attrs =
                    Files.readAttributes(path, BasicFileAttributes.class);
            final Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("size",      (double) attrs.size());
            stat.put("created",   (double) attrs.creationTime().toMillis());
            stat.put("modified",  (double) attrs.lastModifiedTime().toMillis());
            stat.put("isFile",    attrs.isRegularFile());
            stat.put("isDir",     attrs.isDirectory());
            stat.put("isSymlink", attrs.isSymbolicLink());
            stat.put("isHidden",  Files.isHidden(path));
            return stat;
        } catch (IOException e) { throw new LarvRuntimeException("pathStat(): " + e.getMessage()); }
    }

    /** List immediate children of a directory. Returns list of path strings. */
    public static @NotNull Object path_pathList(final Object p) {
        try (final var stream = Files.list(Path.of(str(p, "pathList")))) {
            final List<Object> result = new ArrayList<>();
            stream.sorted().forEach(entry -> result.add(entry.toString()));
            return result;
        } catch (IOException e) { throw new LarvRuntimeException("pathList(): " + e.getMessage()); }
    }

    /** List all descendants recursively. Returns list of path strings. */
    public static @NotNull Object path_pathListAll(final Object p) {
        try (final var stream = Files.walk(Path.of(str(p, "pathListAll")))) {
            final List<Object> result = new ArrayList<>();
            stream.sorted().forEach(entry -> result.add(entry.toString()));
            return result;
        } catch (IOException e) { throw new LarvRuntimeException("pathListAll(): " + e.getMessage()); }
    }

    /**
     * List entries matching a glob pattern.
     * pathGlob("/src", "**\/*.java")  → all .java files recursively
     * pathGlob("/src", "*.txt")       → .txt files in /src only
     */
    public static @NotNull Object path_pathGlob(final Object dir, final Object pattern) {
        final Path base = Path.of(str(dir, "pathGlob"));
        final PathMatcher matcher = base.getFileSystem()
                .getPathMatcher("glob:" + str(pattern, "pathGlob"));
        final List<Object> result = new ArrayList<>();
        try (final var stream = Files.walk(base)) {
            stream.filter(p -> matcher.matches(base.relativize(p)))
                    .sorted()
                    .forEach(p -> result.add(p.toString()));
        } catch (IOException e) { throw new LarvRuntimeException("pathGlob(): " + e.getMessage()); }
        return result;
    }

    /**
     * Walk a directory tree and return a list of stat maps for every entry.
     * Each map: { path, name, size, modified, isFile, isDir, isSymlink }
     */
    public static @NotNull Object path_pathWalk(final Object p) {
        final List<Object> result = new ArrayList<>();
        try {
            Files.walkFileTree(Path.of(str(p, "pathWalk")), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    result.add(entryMap(file, attrs));
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                    result.add(entryMap(dir, attrs));
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                    return FileVisitResult.CONTINUE; // skip unreadable entries
                }
            });
        } catch (IOException e) { throw new LarvRuntimeException("pathWalk(): " + e.getMessage()); }
        return result;
    }

    /** Create a directory (fails if parent doesn't exist). */
    public static @Nullable Object path_pathMakeDir(final Object p) {
        try { Files.createDirectory(Path.of(str(p, "pathMakeDir"))); return null; }
        catch (IOException e) { throw new LarvRuntimeException("pathMakeDir(): " + e.getMessage()); }
    }

    /** Create a directory and all missing parents. */
    public static @Nullable Object path_pathMakeDirs(final Object p) {
        try { Files.createDirectories(Path.of(str(p, "pathMakeDirs"))); return null; }
        catch (IOException e) { throw new LarvRuntimeException("pathMakeDirs(): " + e.getMessage()); }
    }

    /** Delete a single file or empty directory. */
    public static @NotNull @Unmodifiable Object path_pathDelete(final Object p) {
        try { return Files.deleteIfExists(Path.of(str(p, "pathDelete"))); }
        catch (IOException e) { throw new LarvRuntimeException("pathDelete(): " + e.getMessage()); }
    }

    /** Recursively delete a directory tree or a single file. */
    public static @NotNull Object path_pathDeleteAll(final Object p) {
        final Path root = Path.of(str(p, "pathDeleteAll"));
        if (!Files.exists(root)) return false;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) { throw new LarvRuntimeException("pathDeleteAll(): " + e.getMessage()); }
    }

    /** Copy a file or directory. Overwrites destination if it exists. */
    public static @Nullable Object path_pathCopy(final Object src, final Object dst) {
        try {
            Files.copy(Path.of(str(src, "pathCopy")),
                    Path.of(str(dst, "pathCopy")),
                    StandardCopyOption.REPLACE_EXISTING);
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("pathCopy(): " + e.getMessage()); }
    }

    /** Move / rename a file or directory. */
    public static @Nullable Object path_pathMove(final Object src, final Object dst) {
        try {
            Files.move(Path.of(str(src, "pathMove")),
                    Path.of(str(dst, "pathMove")),
                    StandardCopyOption.REPLACE_EXISTING);
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("pathMove(): " + e.getMessage()); }
    }

    /** Create a symbolic link. pathSymlink(link, target) */
    public static @Nullable Object path_pathSymlink(final Object link, final Object target) {
        try {
            Files.createSymbolicLink(Path.of(str(link, "pathSymlink")),
                    Path.of(str(target, "pathSymlink")));
            return null;
        } catch (IOException e) { throw new LarvRuntimeException("pathSymlink(): " + e.getMessage()); }
    }

    /** Resolve a symbolic link to its real path. */
    public static @NotNull @Unmodifiable Object path_pathResolveSymlink(final Object p) {
        try { return Path.of(str(p, "pathResolveSymlink")).toRealPath().toString(); }
        catch (IOException e) { throw new LarvRuntimeException("pathResolveSymlink(): " + e.getMessage()); }
    }

    /** Current working directory. */
    public static @NotNull @Unmodifiable Object path_pathCwd(final Object... ignored) {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().toString();
    }

    /** Current user's home directory. */
    public static @NotNull @Unmodifiable Object path_pathHome(final Object... ignored) {
        return Path.of(System.getProperty("user.home")).toAbsolutePath().toString();
    }

    /** System default temp directory. */
    public static @NotNull @Unmodifiable Object path_pathTemp(final Object... ignored) {
        return Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().toString();
    }

    /** Platform file separator ("/" on Unix, "\" on Windows). */
    public static @Unmodifiable Object path_pathSep(final Object... ignored) {
        return FileSystems.getDefault().getSeparator();
    }

    private static @NotNull Map<String, Object> entryMap(final @NotNull Path p, final @NotNull BasicFileAttributes attrs) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("path",      p.toString());
        m.put("name",      p.getFileName() != null ? p.getFileName().toString() : "");
        m.put("size",      (double) attrs.size());
        m.put("modified",  (double) attrs.lastModifiedTime().toMillis());
        m.put("isFile",    attrs.isRegularFile());
        m.put("isDir",     attrs.isDirectory());
        m.put("isSymlink", attrs.isSymbolicLink());
        return m;
    }

    @Contract("null,_ -> fail")
    private static String str(final Object v, final String fn) {
        if (!(v instanceof String s))
            throw new LarvRuntimeException(fn + "(): expected string, got " + typeName(v));
        return s;
    }

    @SuppressWarnings("rawtypes")
    private static @NotNull String typeName(final Object v) {
        return switch (v) {
            case null -> "null";
            case String ignored -> "string";
            case Double ignored -> "number";
            case Boolean ignored -> "bool";
            case List ignored -> "list";
            case Map ignored -> "map";
            default -> v.getClass().getSimpleName();
        };
    }
}