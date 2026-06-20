package com.habbashx.larv.compiler.stdlib.libs;

import com.habbashx.larv.compiler.runtime.LarvRuntimeException;
import com.habbashx.larv.compiler.stdlib.LarvStdlib;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.*;
import java.util.*;

/**
 * JDBC stdlib lib for Larv.
 *
 * ALL queries are parameterised — raw string concatenation into SQL is never done
 * with user-supplied values. dbQuery/dbExecute require a params list; if you have
 * no parameters pass an empty list [].
 *
 * Usage:
 *   import "jdbc"
 *   dbConnect("db", "org.postgresql.Driver", "jdbc:postgresql://localhost/mydb", "user", "pass")
 *   var rows = dbQuery("db", "SELECT * FROM users WHERE id = ?", [42])
 *   dbExecute("db", "DELETE FROM sessions WHERE expired = ?", [true])
 */
public final class JdbcLib implements LarvStdlib {

    private static final Map<String, Connection> CONNECTIONS = new LinkedHashMap<>();

    @Contract(pure = true)
    @Override public @NotNull String name() { return "jdbc"; }

    @Override
    @NotNull
    @Unmodifiable
    public Map<String, String> functions() {
        return Map.ofEntries(
                Map.entry("dbConnect",  "jdbc_dbConnect"),
                Map.entry("dbClose",    "jdbc_dbClose"),
                Map.entry("dbIsOpen",   "jdbc_dbIsOpen"),
                Map.entry("dbQuery",    "jdbc_dbQuery"),
                Map.entry("dbExecute",  "jdbc_dbExecute"),
                Map.entry("dbInsert",   "jdbc_dbInsert"),
                Map.entry("dbUpdate",   "jdbc_dbUpdate"),
                Map.entry("dbDelete",   "jdbc_dbDelete"),
                Map.entry("dbBegin",    "jdbc_dbBegin"),
                Map.entry("dbCommit",   "jdbc_dbCommit"),
                Map.entry("dbRollback", "jdbc_dbRollback"),
                Map.entry("dbTables",   "jdbc_dbTables"),
                Map.entry("dbColumns",  "jdbc_dbColumns")
        );
    }

    /**
     * dbConnect(alias, driverClass, jdbcUrl, user, password)
     *
     *   dbConnect("pg",     "org.postgresql.Driver",                        "jdbc:postgresql://host/db",         "user", "pass")
     *   dbConnect("mysql",  "com.mysql.cj.jdbc.Driver",                     "jdbc:mysql://host/db",              "user", "pass")
     *   dbConnect("sqlite", "org.sqlite.JDBC",                              "jdbc:sqlite:mydb.db",               "",     "")
     *   dbConnect("mssql",  "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://host;database=db", "user", "pass")
     *   dbConnect("oracle", "oracle.jdbc.OracleDriver",                     "jdbc:oracle:thin:@host:1521:sid",   "user", "pass")
     *   dbConnect("h2",     "org.h2.Driver",                                "jdbc:h2:mem:testdb",                "sa",   "")
     */
    public static @Nullable Object jdbc_dbConnect(Object alias, Object driver, Object url, Object user, Object pass) {
        String a = str(alias, "dbConnect");
        String d = str(driver, "dbConnect");
        try {
            Class.forName(d);
        } catch (ClassNotFoundException e) {
            throw new LarvRuntimeException(
                    "dbConnect(): driver '" + d + "' not found on classpath. Add the driver JAR.");
        }
        try {
            Connection existing = CONNECTIONS.get(a);
            if (existing != null) { try { existing.close(); } catch (SQLException ignored) {} }
            Connection c = DriverManager.getConnection(
                    str(url, "dbConnect"), str(user, "dbConnect"), str(pass, "dbConnect"));
            CONNECTIONS.put(a, c);
            return null;
        } catch (SQLException e) {
            throw new LarvRuntimeException("dbConnect(): " + e.getMessage());
        }
    }

    public static @Nullable Object jdbc_dbClose(Object alias) {
        String a = str(alias, "dbClose");
        try { conn(a).close(); CONNECTIONS.remove(a); return null; }
        catch (SQLException e) { throw new LarvRuntimeException("dbClose(): " + e.getMessage()); }
    }

    public static @NotNull Object jdbc_dbIsOpen(Object alias) {
        Connection c = CONNECTIONS.get(str(alias, "dbIsOpen"));
        try { return c != null && !c.isClosed(); }
        catch (SQLException e) { return false; }
    }


    /**
     * Run a SELECT with parameters. Always use ? placeholders for values — never
     * concatenate user input into the sql string.
     *
     * dbQuery(alias, sql, params) → list of row maps
     *
     *   var rows = dbQuery("db", "SELECT * FROM users WHERE role = ? AND active = ?", ["admin", true])
     *   var all  = dbQuery("db", "SELECT * FROM products", [])
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Object jdbc_dbQuery(Object alias, Object sql, Object params) {
        List<Object> p = params instanceof List<?> l ? (List<Object>) l : List.of();
        try (PreparedStatement ps = conn(str(alias, "dbQuery")).prepareStatement(str(sql, "dbQuery"))) {
            bindParams(ps, p);
            try (ResultSet rs = ps.executeQuery()) { return resultSetToList(rs); }
        } catch (SQLException e) { throw new LarvRuntimeException("dbQuery(): " + e.getMessage()); }
    }

    /**
     * Run a DDL/DML statement with parameters. Returns affected row count.
     *
     * dbExecute(alias, sql, params) → number
     *
     *   dbExecute("db", "UPDATE users SET active = ? WHERE last_login < ?", [false, cutoffDate])
     *   dbExecute("db", "CREATE TABLE logs (id SERIAL PRIMARY KEY, msg TEXT)", [])
     */
    @SuppressWarnings("unchecked")
    public static Object jdbc_dbExecute(Object alias, Object sql, Object params) {
        List<Object> p = params instanceof List<?> l ? (List<Object>) l : List.of();
        try (PreparedStatement ps = conn(str(alias, "dbExecute")).prepareStatement(str(sql, "dbExecute"))) {
            bindParams(ps, p);
            return (double) ps.executeUpdate();
        } catch (SQLException e) { throw new LarvRuntimeException("dbExecute(): " + e.getMessage()); }
    }

    /**
     * INSERT a row from a map. Column names come from map keys (trusted schema names),
     * values are always bound as parameters.
     *
     * dbInsert(alias, table, dataMap) → affected rows
     *
     *   dbInsert("db", "users", {"name": "Alice", "age": 30, "active": true})
     */
    public static Object jdbc_dbInsert(Object alias, Object table, Object data) {
        Map<String, Object> m = map(data, "dbInsert");
        if (m.isEmpty()) throw new LarvRuntimeException("dbInsert(): data map is empty");
        StringBuilder cols = new StringBuilder(), placeholders = new StringBuilder();
        List<Object> vals = new ArrayList<>();
        for (var e : m.entrySet()) {
            if (!cols.isEmpty()) { cols.append(", "); placeholders.append(", "); }
            cols.append(validateIdentifier(e.getKey(), "dbInsert"));
            placeholders.append("?");
            vals.add(e.getValue());
        }
        String sql = "INSERT INTO " + validateIdentifier(str(table, "dbInsert"), "dbInsert") +
                " (" + cols + ") VALUES (" + placeholders + ")";
        try (PreparedStatement ps = conn(str(alias, "dbInsert")).prepareStatement(sql)) {
            bindParams(ps, vals);
            return (double) ps.executeUpdate();
        } catch (SQLException e) { throw new LarvRuntimeException("dbInsert(): " + e.getMessage()); }
    }

    /**
     * UPDATE rows matching a parameterised WHERE clause.
     * Table name, column names are validated as identifiers.
     * WHERE values must be passed as params — never concatenated.
     *
     * dbUpdate(alias, table, dataMap, whereClause, whereParams) → affected rows
     *
     *   dbUpdate("db", "users", {"name": "Bob"}, "id = ? AND active = ?", [1, true])
     */
    @SuppressWarnings("unchecked")
    public static Object jdbc_dbUpdate(Object alias, Object table, Object data, Object where, Object whereParams) {
        Map<String, Object> m = map(data, "dbUpdate");
        if (m.isEmpty()) throw new LarvRuntimeException("dbUpdate(): data map is empty");
        List<Object> wp = whereParams instanceof List<?> l ? (List<Object>) l : List.of();
        StringBuilder sets = new StringBuilder();
        List<Object> vals = new ArrayList<>();
        for (var e : m.entrySet()) {
            if (!sets.isEmpty()) sets.append(", ");
            sets.append(validateIdentifier(e.getKey(), "dbUpdate")).append(" = ?");
            vals.add(e.getValue());
        }
        vals.addAll(wp); // SET params first, then WHERE params
        String sql = "UPDATE " + validateIdentifier(str(table, "dbUpdate"), "dbUpdate") +
                " SET " + sets + " WHERE " + str(where, "dbUpdate");
        try (PreparedStatement ps = conn(str(alias, "dbUpdate")).prepareStatement(sql)) {
            bindParams(ps, vals);
            return (double) ps.executeUpdate();
        } catch (SQLException e) { throw new LarvRuntimeException("dbUpdate(): " + e.getMessage()); }
    }

    /**
     * DELETE rows matching a parameterised WHERE clause.
     *
     * dbDelete(alias, table, whereClause, whereParams) → affected rows
     *
     *   dbDelete("db", "users", "id = ?", [42])
     *   dbDelete("db", "sessions", "user_id = ? AND expired = ?", [userId, true])
     */
    @SuppressWarnings("unchecked")
    public static Object jdbc_dbDelete(Object alias, Object table, Object where, Object whereParams) {
        List<Object> wp = whereParams instanceof List<?> l ? (List<Object>) l : List.of();
        String sql = "DELETE FROM " + validateIdentifier(str(table, "dbDelete"), "dbDelete") +
                " WHERE " + str(where, "dbDelete");
        try (PreparedStatement ps = conn(str(alias, "dbDelete")).prepareStatement(sql)) {
            bindParams(ps, wp);
            return (double) ps.executeUpdate();
        } catch (SQLException e) { throw new LarvRuntimeException("dbDelete(): " + e.getMessage()); }
    }

    public static @Nullable Object jdbc_dbBegin(Object alias) {
        try { conn(str(alias, "dbBegin")).setAutoCommit(false); return null; }
        catch (SQLException e) { throw new LarvRuntimeException("dbBegin(): " + e.getMessage()); }
    }

    public static @Nullable Object jdbc_dbCommit(Object alias) {
        try { Connection c = conn(str(alias, "dbCommit")); c.commit(); c.setAutoCommit(true); return null; }
        catch (SQLException e) { throw new LarvRuntimeException("dbCommit(): " + e.getMessage()); }
    }

    public static @Nullable Object jdbc_dbRollback(Object alias) {
        try { Connection c = conn(str(alias, "dbRollback")); c.rollback(); c.setAutoCommit(true); return null; }
        catch (SQLException e) { throw new LarvRuntimeException("dbRollback(): " + e.getMessage()); }
    }

    public static @NotNull Object jdbc_dbTables(Object alias) {
        try {
            DatabaseMetaData meta = conn(str(alias, "dbTables")).getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                List<Object> names = new ArrayList<>();
                while (rs.next()) names.add(rs.getString("TABLE_NAME"));
                return names;
            }
        } catch (SQLException e) { throw new LarvRuntimeException("dbTables(): " + e.getMessage()); }
    }

    public static @NotNull Object jdbc_dbColumns(Object alias, Object table) {
        try {
            DatabaseMetaData meta = conn(str(alias, "dbColumns")).getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, str(table, "dbColumns"), "%")) {
                List<Object> cols = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("name",     rs.getString("COLUMN_NAME"));
                    col.put("type",     rs.getString("TYPE_NAME"));
                    col.put("size",     (double) rs.getInt("COLUMN_SIZE"));
                    col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    cols.add(col);
                }
                return cols;
            }
        } catch (SQLException e) { throw new LarvRuntimeException("dbColumns(): " + e.getMessage()); }
    }


    private static @NotNull Connection conn(String alias) {
        Connection c = CONNECTIONS.get(alias);
        if (c == null)
            throw new LarvRuntimeException("jdbc: no connection '" + alias + "'. Call dbConnect() first.");
        try {
            if (c.isClosed())
                throw new LarvRuntimeException("jdbc: connection '" + alias + "' is closed.");
        } catch (SQLException e) { throw new LarvRuntimeException("jdbc: " + e.getMessage()); }
        return c;
    }

    /**
     * Validates that a table or column name contains only safe identifier characters.
     * Prevents injection through map keys and table name arguments.
     * Allows: letters, digits, underscores, dots (schema.table), dollar signs.
     */
    @Contract("_, _ -> param1")
    private static @NotNull String validateIdentifier(@NotNull String name, String fn) {
        if (!name.matches("[\\w.$]+"))
            throw new LarvRuntimeException(
                    fn + "(): unsafe identifier '" + name + "'. " +
                            "Only letters, digits, underscores, dots and $ are allowed.");
        return name;
    }

    private static @NotNull List<Object> resultSetToList(@NotNull ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        List<Object> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                Object val = rs.getObject(i);
                if      (val instanceof Integer v) val = (double) v;
                else if (val instanceof Long v)    val = (double) v;
                else if (val instanceof Float v)   val = (double) v;
                else if (val instanceof Short v)   val = (double) v;
                else if (val instanceof Byte v)    val = (double) v;
                row.put(meta.getColumnLabel(i), val);
            }
            rows.add(row);
        }
        return rows;
    }

    private static void bindParams(PreparedStatement ps, @NotNull List<Object> vals) throws SQLException {
        for (int i = 0; i < vals.size(); i++) {
            Object v = vals.get(i);
            if      (v == null)             ps.setNull(i + 1, Types.NULL);
            else if (v instanceof String s)  ps.setString(i + 1, s);
            else if (v instanceof Double d)  ps.setDouble(i + 1, d);
            else if (v instanceof Boolean b) ps.setBoolean(i + 1, b);
            else                             ps.setObject(i + 1, v);
        }
    }

    @Contract("null,_ -> fail")
    private static String str(Object v, String fn) {
        if (!(v instanceof String s))
            throw new LarvRuntimeException(fn + "(): expected string, got " + typeName(v));
        return s;
    }

    @Contract("null,_ -> fail")
    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> map(Object v, String fn) {
        if (!(v instanceof Map<?,?>))
            throw new LarvRuntimeException(fn + "(): expected map, got " + typeName(v));
        return (Map<String, Object>) v;
    }

    @SuppressWarnings("rawtypes")
    private static @NotNull String typeName(Object v) {
        return switch (v) {
            case null -> "null";
            case String s -> "string";
            case Double aDouble -> "number";
            case Boolean b -> "bool";
            case List list -> "list";
            case Map map -> "map";
            default -> v.getClass().getSimpleName();
        };
    }
}