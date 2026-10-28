package com.quanlynhakhoa.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC connectivity. Never builds a schema, never runs migrations. */
public final class Database {
    private Database() { }

    public static Connection open() throws SQLException {
        String url = System.getenv("N08_DB_URL");
        if (url == null || url.isBlank()) {
            throw new SQLException("N08_DB_URL is missing. UI demo can run without DB.");
        }
        String user = System.getenv("N08_DB_USER");
        String password = System.getenv("N08_DB_PASSWORD");
        DriverManager.setLoginTimeout(8);
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url); // e.g., integrated security when configured
        }
        return DriverManager.getConnection(url, user, password == null ? "" : password);
    }

    public static void checkConnection() throws SQLException {
        try (Connection connection = open()) {
            if (!connection.isValid(5)) {
                throw new SQLException("SQL Server connection is not valid.");
            }
            System.out.println("DB CONNECTED: " + connection.getMetaData().getDatabaseProductName());
        }
    }
}
