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

public final class AccountCreateSqlGrants {
    private static final Pattern DATABASE = Pattern.compile("N08Dev_[0-9a-f]{8}");
    private static final Pattern LOGIN = Pattern.compile("n08dev_[0-9a-f]{8}");

    private AccountCreateSqlGrants() { }

    public static void main(String[] args) {
        try {
            if (args.length != 1 || !("--check".equals(args[0]) || "--apply".equals(args[0]))) {
                throw new IllegalArgumentException("Use --check or --apply");
            }
            Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path local = project.resolve(".n08-dev.local.properties");
            if (!Files.isRegularFile(project.resolve("pom.xml")) || !Files.isRegularFile(local)) {
                throw new IllegalArgumentException("Missing project or local dev configuration");
            }
            Properties cfg = new Properties();
            try (var reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                cfg.load(reader);
            }
            String db = cfg.getProperty("dev.database", "").trim();
            String user = cfg.getProperty("dev.user", "").trim();
            String portText = cfg.getProperty("dev.port", "").trim();
            int port = Integer.parseInt(portText);
            if (!DATABASE.matcher(db).matches() || !LOGIN.matcher(user).matches()
                    || !user.equals("n08dev_" + db.substring("N08Dev_".length()))
                    || port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid isolated N08Dev database/user");
            }
            String admin = System.getenv("N08_CORE06_SQL_ADMIN_USER");
            String password = System.getenv("N08_CORE06_SQL_ADMIN_PASSWORD");
            if (admin == null || !admin.matches("[a-zA-Z0-9._-]{1,60}")
                    || password == null || password.isBlank()) {
                throw new IllegalArgumentException("Missing temporary SQL administrator credentials");
            }
            String url = "jdbc:sqlserver://127.0.0.1:" + port
                    + ";databaseName=" + db
                    + ";encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=N08-Core06-Grants";
            try (Connection connection = DriverManager.getConnection(url, admin, password)) {
                if (!db.equals(queryString(connection, "SELECT DB_NAME()"))) {
                    throw new SQLException("Wrong database");
                }
                if (!"39".equals(queryString(connection,
                        "SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo')"))) {
                    throw new SQLException("Unexpected database schema");
                }
                try (PreparedStatement st = connection.prepareStatement("""
                        SELECT COUNT(*)
                        FROM sys.database_principals AS dp
                        INNER JOIN sys.server_principals AS sp ON sp.sid = dp.sid
                        WHERE dp.name = ? AND sp.name = ? AND dp.type = 'S'
                        """)) {
                    st.setString(1, user);
                    st.setString(2, user);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 1) {
                            throw new SQLException("App SQL user/login mismatch");
                        }
                    }
                }
                try (PreparedStatement st = connection.prepareStatement("SELECT IS_ROLEMEMBER('db_owner', ?)")) {
                    st.setString(1, user);
                    try (ResultSet rs = st.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 0 || rs.wasNull()) {
                            throw new SQLException("App SQL user must not be db_owner");
                        }
                    }
                }
                if ("--apply".equals(args[0])) {
                    connection.setAutoCommit(false);
                    try (Statement st = connection.createStatement()) {
                        st.execute("GRANT INSERT ON OBJECT::dbo.TaiKhoan TO [" + user + "]");
                        connection.commit();
                    } catch (SQLException | RuntimeException failure) {
                        connection.rollback();
                        throw failure;
                    }
                }
            }
            try (Connection app = TeamDevConnection.open()) {
                boolean canInsert = has(app, "dbo.TaiKhoan", "INSERT");
                boolean canAudit = has(app, "dbo.NhatKyThaoTac", "INSERT");
                boolean canReadAccounts = has(app, "dbo.TaiKhoan", "SELECT");
                boolean canReadEmployees = has(app, "dbo.NhanVien", "SELECT");
                boolean deleteAccounts = has(app, "dbo.TaiKhoan", "DELETE");
                if (deleteAccounts) throw new SQLException("Unexpected DELETE grant");
                if (!canInsert || !canAudit || !canReadAccounts || !canReadEmployees) {
                    if ("--apply".equals(args[0])) throw new SQLException("Missing required scoped grants");
                    System.out.println("CHECK: CORE-06B1 requires INSERT dbo.TaiKhoan, INSERT audit and existing SELECT grants");
                } else {
                    System.out.println("PASS: scoped CORE-06B1 database permissions; no DELETE");
                }
            }
        } catch (Exception failure) {
            System.err.println("CORE-06B1 SQL GRANT FAILED: " + failure.getClass().getSimpleName());
            System.exit(2);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) throw new SQLException("Missing preflight result");
            return rs.getString(1);
        }
    }

    private static boolean has(Connection connection, String object, String privilege) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', ?)")) {
            st.setString(1, object);
            st.setString(2, privilege);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
}
