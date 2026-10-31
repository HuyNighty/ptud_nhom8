package com.quanlynhakhoa.dev;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/** Grant only INSERT on NhanVien to the existing isolated development SQL login. */
public final class WorkforceSqlGrants {
    private static final Pattern DATABASE = Pattern.compile("N08Dev_[0-9a-f]{8}");
    private static final Pattern LOGIN = Pattern.compile("n08dev_[0-9a-f]{8}");

    private WorkforceSqlGrants() { }

    public static void main(String[] args) {
        String stage = "arguments";
        try {
            if (args.length != 1 || !("--check".equals(args[0]) || "--apply".equals(args[0]))) {
                throw new IllegalArgumentException("Use --check or --apply");
            }
            boolean apply = "--apply".equals(args[0]);
            Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path local = root.resolve(".n08-dev.local.properties");
            if (!Files.isRegularFile(root.resolve("pom.xml")) || !Files.isRegularFile(local)) {
                throw new IllegalArgumentException("Missing Maven project or local dev configuration");
            }
            Properties cfg = new Properties();
            try (var reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                cfg.load(reader);
            }
            String db = cfg.getProperty("dev.database", "").trim();
            String appLogin = cfg.getProperty("dev.user", "").trim();
            int port = Integer.parseInt(cfg.getProperty("dev.port", ""));
            if (!DATABASE.matcher(db).matches() || !LOGIN.matcher(appLogin).matches()
                    || !appLogin.equals("n08dev_" + db.substring("N08Dev_".length()))
                    || port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid N08Dev database or application SQL login");
            }
            String admin = System.getenv("N08_W1B_SQL_ADMIN_USER");
            String password = System.getenv("N08_W1B_SQL_ADMIN_PASSWORD");
            if (admin == null || !admin.matches("[a-zA-Z0-9._-]{1,60}")
                    || password == null || password.isBlank()) {
                throw new IllegalArgumentException("Missing temporary SQL administrator credentials");
            }
            String url = "jdbc:sqlserver://127.0.0.1:" + port
                    + ";databaseName=" + db
                    + ";encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=N08-W1B-Grants";
            stage = "validate isolated database";
            try (Connection connection = DriverManager.getConnection(url, admin, password)) {
                if (!db.equals(queryString(connection, "SELECT DB_NAME()"))) {
                    throw new SQLException("Wrong database");
                }
                if (!"39".equals(queryString(connection,
                        "SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo')"))) {
                    throw new SQLException("Unexpected isolated database schema");
                }
                if (!"1".equals(queryString(connection,
                        "SELECT COUNT(*) FROM sys.tables WHERE object_id = OBJECT_ID(N'dbo.NhanVien')"))) {
                    throw new SQLException("Employee table missing");
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM sys.database_principals AS dp
                        INNER JOIN sys.server_principals AS sp ON dp.sid = sp.sid
                        WHERE dp.name = ? AND sp.name = ? AND dp.type = 'S'
                        """)) {
                    statement.setString(1, appLogin);
                    statement.setString(2, appLogin);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 1) {
                            throw new SQLException("Application login/principal mismatch");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT IS_ROLEMEMBER('db_owner', ?)")) {
                    statement.setString(1, appLogin);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 0 || rs.wasNull()) {
                            throw new SQLException("Application principal must not be db_owner");
                        }
                    }
                }
                if (apply) {
                    stage = "grant employee INSERT only";
                    connection.setAutoCommit(false);
                    try (Statement statement = connection.createStatement()) {
                        statement.setQueryTimeout(15);
                        // App login is generated by a restricted regex and matches DB suffix.
                        statement.execute("GRANT INSERT ON OBJECT::dbo.NhanVien TO [" + appLogin + "]");
                        connection.commit();
                    } catch (SQLException | RuntimeException failure) {
                        connection.rollback();
                        throw failure;
                    }
                }
            }
            stage = "verify effective application privileges";
            try (Connection app = TeamDevConnection.open()) {
                boolean readEmployees = has(app, "dbo.NhanVien", "SELECT");
                boolean insertEmployees = has(app, "dbo.NhanVien", "INSERT");
                boolean audit = has(app, "dbo.NhatKyThaoTac", "INSERT");
                boolean unexpectedUpdate = has(app, "dbo.NhanVien", "UPDATE");
                boolean unexpectedDelete = has(app, "dbo.NhanVien", "DELETE");
                if (unexpectedDelete || unexpectedUpdate) {
                    throw new SQLException("Unexpected UPDATE/DELETE privilege on dbo.NhanVien");
                }
                if (!readEmployees || !audit) {
                    throw new SQLException("Missing baseline SELECT employee or INSERT audit privilege");
                }
                if (!insertEmployees && apply) {
                    throw new SQLException("Expected scoped INSERT privilege after grant");
                }
                if (!insertEmployees) {
                    System.out.println("CHECK: W1B needs INSERT dbo.NhanVien; no SQL writes performed");
                } else {
                    System.out.println("PASS: W1B INSERT dbo.NhanVien; audit INSERT and employee SELECT; no UPDATE/DELETE");
                }
                System.out.println(apply ? "W1B GRANT APPLIED (N08Dev only)" : "W1B PREFLIGHT COMPLETE (read-only)");
            }
        } catch (Exception failure) {
            System.err.println("W1B SQL GATE FAILED at " + stage + ": " + failure.getClass().getSimpleName());
            System.exit(2);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) throw new SQLException("Missing SQL preflight result");
            return rs.getString(1);
        }
    }

    private static boolean has(Connection connection, String object, String permission) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', ?)")) {
            statement.setString(1, object);
            statement.setString(2, permission);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
}
