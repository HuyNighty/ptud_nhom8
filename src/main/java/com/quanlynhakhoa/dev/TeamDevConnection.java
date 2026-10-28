package com.quanlynhakhoa.dev;

import com.quanlynhakhoa.auth.SandboxLoginConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * A local development credential source, never a production database connector.
 * Existing N08AuthIT_ synthetic launcher remains available when no local config exists.
 */
public final class TeamDevConnection {
    private static final Pattern DEV_NAME = Pattern.compile("N08Dev_[0-9a-f]{8}");
    private static final Pattern APP_USER = Pattern.compile("n08dev_[0-9a-f]{8}");

    private TeamDevConnection() { }

    public static Path configPath() {
        return Path.of(System.getProperty("user.dir"), ".n08-dev.local.properties")
                .toAbsolutePath().normalize();
    }

    public static boolean hasLocalConfig() {
        return Files.isRegularFile(configPath());
    }

    public static Connection open() throws SQLException {
        if (!hasLocalConfig()) {
            return SandboxLoginConnection.open(); // Historical synthetic guard is preserved.
        }
        Properties values = new Properties();
        try (var reader = Files.newBufferedReader(configPath(), StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException e) {
            throw new SQLException("Cannot read local development config; rerun onboarding setup", e);
        }
        String database = values.getProperty("dev.database", "").trim();
        String portText = values.getProperty("dev.port", "").trim();
        String user = values.getProperty("dev.user", "").trim();
        String password = values.getProperty("dev.password", "");
        if (!DEV_NAME.matcher(database).matches() || !APP_USER.matcher(user).matches()
                || !user.substring("n08dev_".length()).equals(database.substring("N08Dev_".length()))
                || password.isBlank()) {
            throw new SQLException("Invalid local dev configuration. Expected isolated N08Dev database.");
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            throw new SQLException("Invalid SQL Server local port", e);
        }
        if (port < 1 || port > 65535) throw new SQLException("Invalid SQL Server local port");
        // The host is intentionally fixed to loopback: configuration cannot target a remote DB.
        String url = "jdbc:sqlserver://127.0.0.1:" + port
                + ";databaseName=" + database
                + ";encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=N08-Team-Dev";
        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            try (PreparedStatement query = connection.prepareStatement("SELECT DB_NAME()")) {
                query.setQueryTimeout(5);
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next() || !database.equals(result.getString(1))) {
                        throw new SQLException("Connected SQL database is not the approved local dev database");
                    }
                }
            }
            return connection;
        } catch (SQLException | RuntimeException failure) {
            try { connection.close(); } catch (SQLException closeError) {
                failure.addSuppressed(closeError);
            }
            throw failure;
        }
    }
}
