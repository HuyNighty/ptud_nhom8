package com.quanlynhakhoa.config.phattrien;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public final class TeamM1Permissions {
    private static final Pattern DB_NAME = Pattern.compile("N08Dev_[0-9a-f]{8}");
    private static final Pattern LOGIN_NAME = Pattern.compile("n08dev_[0-9a-f]{8}");

    private static final class CapQuyenBang {
        private final String tenBang;
        private final String cacQuyen;

        private CapQuyenBang(String table, String privileges) {
            this.tenBang = table;
            this.cacQuyen = privileges;
        }

        public String table() { return tenBang; }
        public String privileges() { return cacQuyen; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CapQuyenBang value)) return false;
            return java.util.Objects.equals(tenBang, value.tenBang)
                    && java.util.Objects.equals(cacQuyen, value.cacQuyen);
        }

        @Override
        public int hashCode() {
            return 31 * java.util.Objects.hashCode(tenBang) + java.util.Objects.hashCode(cacQuyen);
        }

        @Override
        public String toString() {
            return "Grant[table=" + tenBang + ", privileges=" + cacQuyen + "]";
        }
    }

    private static final List<CapQuyenBang> GRANTS = List.of(
            new CapQuyenBang("NhanVien", "INSERT"),
            new CapQuyenBang("BenhNhan", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("HoSoBenhAn", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("LichHen", "SELECT"),
            new CapQuyenBang("PhieuKham", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("KeHoachDieuTri", "SELECT"),
            new CapQuyenBang("DichVu", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("Thuoc", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("VatTu", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("NhaCungCap", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("LoHang", "SELECT"),
            new CapQuyenBang("HoaDon", "SELECT"),
            new CapQuyenBang("ChiTietHoaDon", "SELECT"),
            new CapQuyenBang("ThanhToan", "SELECT"),
            new CapQuyenBang("LanThuHoaDon", "SELECT, INSERT, UPDATE"),
            new CapQuyenBang("DieuChinhThanhToan", "SELECT, INSERT"),
            new CapQuyenBang("DongTienHoaDon", "SELECT"),
            new CapQuyenBang("NhatKyThaoTac", "SELECT, INSERT")
    );

    private TeamM1Permissions() { }

    public static void grant(Connection admin, String appSqlLogin) throws SQLException {
        requireDevelopmentSchema(admin, appSqlLogin);
        try (Statement statement = admin.createStatement()) {
            statement.setQueryTimeout(15);
            for (CapQuyenBang grant : GRANTS) {
                statement.execute("GRANT " + grant.privileges()
                        + " ON OBJECT::dbo." + grant.table()
                        + " TO [" + appSqlLogin + "]");
            }
        }
    }

    private static void requireDevelopmentSchema(Connection admin, String appSqlLogin) throws SQLException {
        String database;
        try (Statement statement = admin.createStatement();
             ResultSet rs = statement.executeQuery("SELECT DB_NAME()")) {
            if (!rs.next()) throw new SQLException("No current database");
            database = rs.getString(1);
        }
        if (database == null || !DB_NAME.matcher(database).matches()) {
            throw new SQLException("Refusing permissions outside a local N08Dev database");
        }
        String expectedLogin = "n08dev_" + database.substring("N08Dev_".length());
        if (!LOGIN_NAME.matcher(appSqlLogin).matches() || !expectedLogin.equals(appSqlLogin)) {
            throw new SQLException("Application SQL login does not match the local database");
        }
        try (Statement statement = admin.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo')")) {
            if (!rs.next() || rs.getInt(1) != 43) {
                throw new SQLException("Expected V1-V8 schema (43 dbo tables) before granting M1 permissions");
            }
        }
        try (PreparedStatement statement = admin.prepareStatement("""
                SELECT COUNT(*)
                FROM sys.database_principals AS dp
                INNER JOIN sys.server_principals AS sp ON sp.sid = dp.sid
                WHERE dp.name = ? AND sp.name = ? AND dp.type = 'S'
                """)) {
            statement.setString(1, appSqlLogin);
            statement.setString(2, appSqlLogin);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 1) {
                    throw new SQLException("Expected a matching restricted SQL database user and login");
                }
            }
        }
        try (PreparedStatement statement = admin.prepareStatement(
                "SELECT IS_ROLEMEMBER('db_owner', ?)")) {
            statement.setString(1, appSqlLogin);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getInt(1) != 0 || rs.wasNull()) {
                    throw new SQLException("Cannot confirm that the SQL login is not db_owner");
                }
            }
        }
        for (CapQuyenBang grant : GRANTS) {
            try (PreparedStatement statement = admin.prepareStatement(
                    "SELECT OBJECT_ID(?)")) {
                statement.setString(1, "dbo." + grant.table());
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next() || rs.getObject(1) == null) {
                        throw new SQLException("Missing approved M1 table: " + grant.table());
                    }
                }
            }
        }
    }

    private static int verifyEffectivePrivileges(boolean mustBeComplete) throws SQLException {
        try (Connection app = TeamDevConnection.open()) {
            int missing = 0;
            for (CapQuyenBang grant : GRANTS) {
                for (String privilege : grant.privileges().split(", ")) {
                    if (!hasPermission(app, grant.table(), privilege)) {
                        System.out.println("MISSING: " + grant.table() + " / " + privilege);
                        missing++;
                    }
                }
                if (hasPermission(app, grant.table(), "DELETE")) {
                    throw new SQLException("M1 SQL login unexpectedly has DELETE on " + grant.table());
                }
            }
            if (missing > 0 && mustBeComplete) {
                throw new SQLException("Missing " + missing + " required M1 SQL permissions");
            }
            if (missing == 0) {
                System.out.println("PASS: all M1 SQL permissions verified; DELETE not granted");
            } else {
                System.out.println("CHECK: " + missing + " required M1 grants are missing; no SQL writes");
            }
            return missing;
        }
    }

    private static boolean hasPermission(Connection app, String table, String privilege)
            throws SQLException {
        try (PreparedStatement statement = app.prepareStatement(
                "SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', ?)")) {
            statement.setString(1, "dbo." + table);
            statement.setString(2, privilege);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    public static void main(String[] args) {
        String stage = "arguments";
        try {
            if (args.length != 1 || !("--check".equals(args[0]) || "--apply".equals(args[0]))) {
                throw new IllegalArgumentException("Use --check or --apply");
            }
            boolean apply = "--apply".equals(args[0]);
            Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path config = root.resolve(".n08-dev.local.properties");
            if (!Files.isRegularFile(root.resolve("pom.xml")) || !Files.isRegularFile(config)) {
                throw new IllegalStateException("Run from the configured Maven project root");
            }
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            String database = properties.getProperty("dev.database", "").trim();
            String appUser = properties.getProperty("dev.user", "").trim();
            int port = Integer.parseInt(properties.getProperty("dev.port", ""));
            if (!DB_NAME.matcher(database).matches() || port < 1 || port > 65535
                    || !appUser.equals("n08dev_" + database.substring("N08Dev_".length()))) {
                throw new IllegalStateException("Invalid isolated development database configuration");
            }
            String adminName = System.getenv("N08_M1_SQL_ADMIN_USER");
            String adminPassword = System.getenv("N08_M1_SQL_ADMIN_PASSWORD");
            if (adminName == null || !adminName.matches("[a-zA-Z0-9._-]{1,60}")
                    || adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException("Missing local SQL administrator credentials");
            }
            String url = "jdbc:sqlserver://127.0.0.1:" + port
                    + ";databaseName=" + database
                    + ";encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=N08-M1-Grants";
            stage = "database preflight";
            try (Connection admin = DriverManager.getConnection(url, adminName, adminPassword)) {
                requireDevelopmentSchema(admin, appUser);
                if (apply) {
                    stage = "grant permissions";
                    admin.setAutoCommit(false);
                    try {
                        grant(admin, appUser);
                        admin.commit();
                    } catch (SQLException | RuntimeException e) {
                        admin.rollback();
                        throw e;
                    }
                }
            }
            stage = "effective permission verification";
            verifyEffectivePrivileges(apply);
            System.out.println(apply ? "M1 GRANT PASS: development database only"
                    : "M1 PREFLIGHT PASS: protected database and login checked; no SQL writes");
        } catch (Exception error) {
            System.err.println("M1 CORE FAILED at stage: " + stage);
            if (error instanceof SQLException sql) {
                System.err.println("SQLState=" + sql.getSQLState() + ", code=" + sql.getErrorCode());
            } else if (error instanceof IOException) {
                System.err.println("Local configuration could not be read");
            } else {
                System.err.println("Reason: " + error.getClass().getSimpleName());
            }
            System.exit(2);
        }
    }
}
