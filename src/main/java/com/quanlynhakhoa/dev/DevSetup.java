package com.quanlynhakhoa.dev;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.auth.BCryptPasswordVerifier;
import com.quanlynhakhoa.auth.JdbcAuthService;
import com.quanlynhakhoa.auth.LoginPolicy;
import com.quanlynhakhoa.model.Role;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.Base64;
import java.util.Properties;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Interactive PowerShell wrapper supplies secrets through child-process env (not command args).
 * Creates a NEW local-only N08Dev_ DB, a least-privileged SQL login, and a dev-only app account.
 * Existing databases are never modified. Does not DROP, TRUNCATE, or change protected N08Dental.
 */
public final class DevSetup {
    private static final String APP_USERNAME = "dev.admin";
    private static final String APP_ID = "DEVADMIN01";
    private DevSetup() { }

    public static void main(String[] args) {
        String stage = "validate";
        try {
            Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path configFile = root.resolve(".n08-dev.local.properties");
            if (!Files.isRegularFile(root.resolve("pom.xml"))
                    || !Files.isDirectory(root.resolve("sql/bootstrap"))) {
                throw new IllegalStateException("Run setup from the Maven repository root");
            }
            if (Files.exists(configFile)) {
                throw new IllegalStateException("Local development config already exists; will not overwrite");
            }
            String database = required("N08_SETUP_DB");
            if (!database.matches("N08Dev_[0-9a-f]{8}")) {
                throw new IllegalStateException("Refusing database outside isolated N08Dev naming policy");
            }
            String sqlUser = "n08dev_" + database.substring("N08Dev_".length());
            int port = Integer.parseInt(required("N08_SETUP_PORT"));
            if (port < 1 || port > 65535) throw new IllegalStateException("Invalid SQL Server port");
            String adminUser = required("N08_SETUP_ADMIN_USER");
            if (!adminUser.matches("[a-zA-Z0-9._-]{1,60}")) {
                throw new IllegalStateException("Use a simple local SQL Authentication account name");
            }
            String adminPassword = required("N08_SETUP_ADMIN_PASSWORD");
            String appPassword = required("N08_SETUP_APP_PASSWORD");
            int passwordBytes = appPassword.getBytes(StandardCharsets.UTF_8).length;
            if (passwordBytes < 12 || passwordBytes > 72) {
                throw new IllegalStateException("Application password must be 12-72 UTF-8 bytes");
            }
            String hash = BCrypt.hashpw(appPassword, BCrypt.gensalt(12));
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            String sqlPassword = "Aa1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            String masterUrl = connectionUrl(port, "master", "N08-Local-Setup");
            String devUrl = connectionUrl(port, database, "N08-Local-Setup");

            stage = "inspect server";
            try (Connection master = DriverManager.getConnection(masterUrl, adminUser, adminPassword)) {
                master.setAutoCommit(true);
                try (PreparedStatement check = master.prepareStatement("SELECT DB_ID(?)")) {
                    check.setString(1, database);
                    try (ResultSet r = check.executeQuery()) {
                        if (r.next() && r.getObject(1) != null) {
                            throw new IllegalStateException("Refusing to modify existing SQL database");
                        }
                    }
                }
                try (PreparedStatement check = master.prepareStatement(
                        "SELECT COUNT(*) FROM sys.server_principals WHERE name = ?")) {
                    check.setString(1, sqlUser);
                    try (ResultSet r = check.executeQuery()) {
                        r.next();
                        if (r.getInt(1) != 0) {
                            throw new IllegalStateException("SQL login name already exists; refusing reuse");
                        }
                    }
                }
                stage = "create isolated database";
                try (Statement st = master.createStatement()) {
                    st.setQueryTimeout(60);
                    st.execute("CREATE DATABASE [" + database + "]");
                }
                // The login is server-scoped; generated name/password, no shared 'sa' at runtime.
                stage = "create restricted SQL login";
                try (Statement st = master.createStatement()) {
                    st.execute("CREATE LOGIN [" + sqlUser + "] WITH PASSWORD = '"
                            + sqlPassword + "', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF");
                }
            }

            stage = "install V1-V5 schema";
            try (Connection dev = DriverManager.getConnection(devUrl, adminUser, adminPassword)) {
                dev.setAutoCommit(false);
                try {
                    String[] scripts = {
                            "V1__create_workforce_identity.sql",
                            "V2__create_auth_sessions.sql",
                            "V3__harden_account_lock_constraints.sql",
                            "V4__create_audit_log.sql",
                            "V5__create_team_feature_schema.sql"
                    };
                    for (String name : scripts) {
                        String sql = Files.readString(root.resolve("sql/bootstrap").resolve(name),
                                StandardCharsets.UTF_8);
                        try (Statement statement = dev.createStatement()) {
                            statement.setQueryTimeout(60);
                            boolean hasResultSet = statement.execute(sql);
                            // Drain every result/update count before the next migration batch.
                            while (hasResultSet || statement.getUpdateCount() != -1) {
                                if (hasResultSet) {
                                    try (ResultSet ignored = statement.getResultSet()) {
                                        while (ignored.next()) { /* no query rows expected */ }
                                    }
                                }
                                hasResultSet = statement.getMoreResults();
                            }
                        }
                    }
                    stage = "create synthetic administrator";
                    try (PreparedStatement st = dev.prepareStatement("""
                            INSERT INTO dbo.TaiKhoan (
                                maTaiKhoan, maNhanVien, tenDangNhap, tenHienThi, matKhauHash,
                                vaiTro, trangThai, soLanSai, phaiDoiMatKhau, matKhauCapNhatLuc
                            ) VALUES (?, NULL, ?, ?, ?, 'SYSTEM_ADMIN', 'HOAT_DONG', 0, 0,
                                      SYSUTCDATETIME())
                            """)) {
                        st.setString(1, APP_ID);
                        st.setString(2, APP_USERNAME);
                        st.setNString(3, "Local development administrator");
                        st.setString(4, hash);
                        st.executeUpdate();
                    }
                    stage = "grant login-only SQL permissions";
                    try (Statement st = dev.createStatement()) {
                        st.execute("CREATE USER [" + sqlUser + "] FOR LOGIN [" + sqlUser + "]");
                        st.execute("GRANT SELECT ON OBJECT::dbo.TaiKhoan TO [" + sqlUser + "]");
                        st.execute("GRANT SELECT ON OBJECT::dbo.NhanVien TO [" + sqlUser + "]");
                        st.execute("GRANT UPDATE (trangThai, soLanSai, lanSaiDau, khoaLuc, khoaDen, "
                                + "nguonKhoa, lyDoKhoa, lanDangNhapCuoi, capNhatLuc) "
                                + "ON OBJECT::dbo.TaiKhoan TO [" + sqlUser + "]");
                        st.execute("INSERT INTO dbo.NhatKyThaoTac "
                                + "(loaiTacNhan, dinhDanhTacNhan, chucNang, hanhDong, ketQua) "
                                + "VALUES ('MAINTENANCE', 'LOCAL_DEV_SETUP', 'BOOTSTRAP', "
                                + "'CREATE_SYNTHETIC_ACCOUNT', 'SUCCESS')");
                    }
                    dev.commit();
                } catch (Exception e) {
                    dev.rollback();
                    throw e;
                }
            }

            stage = "verify real JDBC login";
            JdbcAuthService auth = new JdbcAuthService(
                    () -> DriverManager.getConnection(devUrl, sqlUser, sqlPassword),
                    new LoginPolicy(new BCryptPasswordVerifier()), Clock.systemUTC());
            AuthDecision decision = auth.authenticate(APP_USERNAME, appPassword);
            if (!decision.authenticated() || decision.forcePasswordChange()
                    || decision.role() != Role.SYSTEM_ADMIN) {
                throw new IllegalStateException("Synthetic login verification did not pass");
            }

            stage = "save local-only connection configuration";
            Properties props = new Properties();
            props.setProperty("dev.database", database);
            props.setProperty("dev.port", Integer.toString(port));
            props.setProperty("dev.user", sqlUser);
            props.setProperty("dev.password", sqlPassword);
            // Write into a new file only. Never overwrite any pre-existing configuration.
            try (var writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                props.store(writer, "N08 local development ONLY - NEVER COMMIT OR SHARE");
            }
            System.out.println("SETUP PASS: local SQL bootstrap and real JDBC login verified");
            System.out.println("Database: " + database + " on 127.0.0.1:" + port);
            System.out.println("Swing account: " + APP_USERNAME + " (use the app password you chose)");
            System.out.println("Local config: " + configFile);
            System.out.println("NEXT: IntelliJ Run com.quanlynhakhoa.App (project-root working directory)");
        } catch (Exception failure) {
            // Avoid leaking passwords or JDBC internals from exception messages.
            System.err.println("SETUP FAILED at stage: " + stage);
            if (failure instanceof SQLException sql) {
                System.err.println("SQL state=" + sql.getSQLState() + ", code=" + sql.getErrorCode());
            } else {
                System.err.println("Reason: " + failure.getClass().getSimpleName());
            }
            System.err.println("No automatic cleanup attempted. Inspect the local dev DB before retrying.");
            System.exit(2);
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing setup environment: " + key);
        return value;
    }

    private static String connectionUrl(int port, String database, String appName) {
        return "jdbc:sqlserver://127.0.0.1:" + port + ";databaseName=" + database
                + ";encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=" + appName;
    }
}
