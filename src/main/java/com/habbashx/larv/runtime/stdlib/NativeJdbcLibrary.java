package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

/**
 * Universal JDBC library for Larv
 *
 * Supports:
 * - SQLite
 * - MySQL
 * - PostgreSQL
 * - MariaDB
 * - SQL Server
 * - Oracle
 * - Any JDBC-compatible database
 *
 * Example:
 *
 * import "jdbc"
 *
 * dbOpen(
 *     "main",
 *     "org.sqlite.JDBC",
 *     "jdbc:sqlite:app.db"
 * )
 *
 * dbExec("
 *     CREATE TABLE IF NOT EXISTS users (
 *         id INTEGER PRIMARY KEY,
 *         name TEXT
 *     )
 * ")
 *
 * dbInsert(
 *     "INSERT INTO users(name) VALUES(?)",
 *     ["Alice"]
 * )
 *
 * var rows = dbQuery("SELECT * FROM users")
 */

@Native("JDBC Library")
@Deprecated(since = "1.1.0") // unused by compiler & interpreter
public class NativeJdbcLibrary extends NativeLibrary {

    /**
     * Named database connections
     */
    private final Map<String, Connection> connections = new HashMap<>();

    /**
     * Current active connection name
     */
    private String activeConnection = null;

    /**
     * Last generated insert ID
     */
    private long lastInsertId = -1;

    /**
     * Rows affected by last operation
     */
    private int lastAffected = 0;

    public NativeJdbcLibrary(ExecutionContext context) {
        super(context);
    }

    @Override
    public void registerAll() {

        getExecutionContext().registerNative("dbOpen", this::dbOpen);
        getExecutionContext().registerNative("dbClose", this::dbClose);
        getExecutionContext().registerNative("dbUse", this::dbUse);

        getExecutionContext().registerNative("dbExec", this::dbExec);
        getExecutionContext().registerNative("dbInsert", this::dbInsert);

        getExecutionContext().registerNative("dbQuery", this::dbQuery);
        getExecutionContext().registerNative("dbQueryOne", this::dbQueryOne);

        getExecutionContext().registerNative("dbLastId", this::dbLastId);
        getExecutionContext().registerNative("dbAffected", this::dbAffected);

        getExecutionContext().registerNative("dbBegin", this::dbBegin);
        getExecutionContext().registerNative("dbCommit", this::dbCommit);
        getExecutionContext().registerNative("dbRollback", this::dbRollback);

        getExecutionContext().registerNative("dbTables", this::dbTables);
        getExecutionContext().registerNative("dbColumns", this::dbColumns);

        getExecutionContext().registerNative("dbConfigure", this::dbConfigure);
    }

    private @NotNull Connection requireConnection() {

        if (activeConnection == null) {
            throw new LarvError(
                    "jdbc: no active database connection",
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }

        Connection connection = connections.get(activeConnection);

        if (connection == null) {
            throw new LarvError(
                    "jdbc: active connection does not exist",
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }

        return connection;
    }

    @SuppressWarnings("unchecked")
    private @NotNull List<Object> listArg(
            @NotNull List<Object> args,
            int index
    ) {

        if (args.size() <= index || args.get(index) == null) {
            return List.of();
        }

        if (!(args.get(index) instanceof List<?> list)) {

            throw new LarvError(
                    "Argument " + (index + 1) + " must be an array",
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }

        return (List<Object>) list;
    }

    private void bind(
            @NotNull PreparedStatement ps,
            @NotNull List<Object> params
    ) throws SQLException {

        for (int i = 0; i < params.size(); i++) {

            Object value = params.get(i);

            switch (value) {

                case null ->
                        ps.setNull(i + 1, Types.NULL);

                case Double d ->
                        ps.setDouble(i + 1, d);

                case Integer n ->
                        ps.setInt(i + 1, n);

                case Long l ->
                        ps.setLong(i + 1, l);

                case Boolean b ->
                        ps.setBoolean(i + 1, b);

                case byte[] bytes ->
                        ps.setBytes(i + 1, bytes);

                default ->
                        ps.setObject(i + 1, value);
            }
        }
    }

    private @NotNull Map<String, Object> rowToMap(
            @NotNull ResultSet rs
    ) throws SQLException {

        ResultSetMetaData meta = rs.getMetaData();

        int columns = meta.getColumnCount();

        Map<String, Object> row = new LinkedHashMap<>();

        for (int i = 1; i <= columns; i++) {

            String column = meta.getColumnLabel(i);

            Object value = rs.getObject(i);

            if (value instanceof Number number) {
                value = number.doubleValue();
            }

            row.put(column, value);
        }

        return row;
    }

    /**
     * dbOpen(name, driver, url)
     * dbOpen(name, driver, url, user, password)
     */
    private @Nullable Object dbOpen(@NotNull List<Object> args) {

        final String name = strArg(args, 0, "dbOpen");
        final String driver = strArg(args, 1, "dbOpen");
        final String url = strArg(args, 2, "dbOpen");

        final String username = args.size() > 3 && args.get(3) != null ? args.get(3).toString() : "";
        final String password = args.size() > 4 && args.get(4) != null ? args.get(4).toString() : "";

        try {

            Class.forName(driver);

            final Connection connection = DriverManager.getConnection(
                    url,
                    username,
                    password
            );

            connections.put(name, connection);

            activeConnection = name;

            return null;

        } catch (ClassNotFoundException e) {

            throw new LarvError(
                    "JDBC driver not found: " + driver,
                    -1,
                    LarvError.Kind.RUNTIME
            );

        } catch (SQLException e) {

            throw new LarvError(
                    "dbOpen(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    /**
     * dbUse(name)
     */
    private @Nullable Object dbUse(@NotNull List<Object> args) {

        String name = strArg(args, 0, "dbUse");

        if (!connections.containsKey(name)) {

            throw new LarvError(
                    "Connection does not exist: " + name,
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }

        activeConnection = name;

        return null;
    }

    /**
     * dbClose()
     * dbClose(name)
     */
    private @Nullable Object dbClose(@NotNull List<Object> args) {

        String name =
                args.isEmpty()
                        ? activeConnection
                        : strArg(args, 0, "dbClose");

        if (name == null) {
            return null;
        }

        Connection connection = connections.get(name);

        if (connection == null) {
            return null;
        }

        try {

            connection.close();

            connections.remove(name);

            if (Objects.equals(activeConnection, name)) {
                activeConnection = null;
            }

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbClose(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    // =========================================================
    // EXECUTION
    // =========================================================

    private @Nullable Object dbExec(@NotNull List<Object> args) {

        String sql = strArg(args, 0, "dbExec");

        List<Object> params = listArg(args, 1);

        try (
                PreparedStatement ps =
                        requireConnection().prepareStatement(sql)
        ) {

            bind(ps, params);

            lastAffected = ps.executeUpdate();

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbExec(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    private @NotNull Object dbInsert(@NotNull List<Object> args) {

        String sql = strArg(args, 0, "dbInsert");

        List<Object> params = listArg(args, 1);

        try (
                PreparedStatement ps =
                        requireConnection().prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS
                        )
        ) {

            bind(ps, params);

            lastAffected = ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {

                if (keys.next()) {
                    lastInsertId = keys.getLong(1);
                } else {
                    lastInsertId = -1;
                }
            }

            return (double) lastInsertId;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbInsert(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    // =========================================================
    // QUERYING
    // =========================================================

    private @NotNull Object dbQuery(@NotNull List<Object> args) {

        String sql = strArg(args, 0, "dbQuery");

        List<Object> params = listArg(args, 1);

        try (
                PreparedStatement ps =
                        requireConnection().prepareStatement(sql)
        ) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {

                List<Object> rows = new ArrayList<>();

                while (rs.next()) {
                    rows.add(rowToMap(rs));
                }

                return rows;
            }

        } catch (SQLException e) {

            throw new LarvError(
                    "dbQuery(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    private @Nullable Object dbQueryOne(@NotNull List<Object> args) {

        String sql = strArg(args, 0, "dbQueryOne");

        List<Object> params = listArg(args, 1);

        try (
                PreparedStatement ps =
                        requireConnection().prepareStatement(sql)
        ) {

            bind(ps, params);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rowToMap(rs);
                }

                return null;
            }

        } catch (SQLException e) {

            throw new LarvError(
                    "dbQueryOne(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    // =========================================================
    // METADATA
    // =========================================================

    private @NotNull Object dbLastId(@NotNull List<Object> args) {
        return (double) lastInsertId;
    }

    private @NotNull Object dbAffected(@NotNull List<Object> args) {
        return (double) lastAffected;
    }

    private @NotNull Object dbTables(@NotNull List<Object> args) {

        try {

            DatabaseMetaData meta =
                    requireConnection().getMetaData();

            List<Object> tables = new ArrayList<>();

            try (
                    ResultSet rs = meta.getTables(
                            null,
                            null,
                            "%",
                            new String[]{"TABLE"}
                    )
            ) {

                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            return tables;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbTables(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    private @NotNull Object dbColumns(@NotNull List<Object> args) {

        String table = strArg(args, 0, "dbColumns");

        try {

            DatabaseMetaData meta =
                    requireConnection().getMetaData();

            List<Object> columns = new ArrayList<>();

            try (
                    ResultSet rs = meta.getColumns(
                            null,
                            null,
                            table,
                            "%"
                    )
            ) {

                while (rs.next()) {

                    Map<String, Object> column =
                            new LinkedHashMap<>();

                    column.put(
                            "name",
                            rs.getString("COLUMN_NAME")
                    );

                    column.put(
                            "type",
                            rs.getString("TYPE_NAME")
                    );

                    column.put(
                            "size",
                            (double) rs.getInt("COLUMN_SIZE")
                    );

                    column.put(
                            "nullable",
                            rs.getInt("NULLABLE")
                                    == DatabaseMetaData.columnNullable
                    );

                    column.put(
                            "default",
                            rs.getString("COLUMN_DEF")
                    );

                    columns.add(column);
                }
            }

            return columns;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbColumns(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    // =========================================================
    // TRANSACTIONS
    // =========================================================

    private @Nullable Object dbBegin(@NotNull List<Object> args) {

        try {

            requireConnection().setAutoCommit(false);

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbBegin(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    private @Nullable Object dbCommit(@NotNull List<Object> args) {

        try {

            Connection connection = requireConnection();

            connection.commit();

            connection.setAutoCommit(true);

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbCommit(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    private @Nullable Object dbRollback(@NotNull List<Object> args) {

        try {

            Connection connection = requireConnection();

            connection.rollback();

            connection.setAutoCommit(true);

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbRollback(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }

    // =========================================================
    // CONFIGURATION
    // =========================================================

    /**
     * Execute database-specific configuration SQL
     *
     * Example:
     * dbConfigure("PRAGMA foreign_keys=ON")
     */
    private @Nullable Object dbConfigure(@NotNull List<Object> args) {

        String sql = strArg(args, 0, "dbConfigure");

        try (
                Statement statement =
                        requireConnection().createStatement()
        ) {

            statement.execute(sql);

            return null;

        } catch (SQLException e) {

            throw new LarvError(
                    "dbConfigure(): " + e.getMessage(),
                    -1,
                    LarvError.Kind.RUNTIME
            );
        }
    }
}