package com.quanlynhakhoa.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** READ-ONLY DIAGNOSTICS: no DDL, DML, Flyway, or automatic schema bootstrap. */
public final class DbInspector {
    private DbInspector() { }

    public static void inspect() throws SQLException {
        try (Connection connection = Database.open()) {
            if (!connection.isValid(5)) {
                throw new SQLException("SQL Server connection is not valid.");
            }
            String product = connection.getMetaData().getDatabaseProductName();
            System.out.println("PASS: JDBC connection: " + product);

            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT DB_NAME() AS db_name, CURRENT_USER AS db_user")) {
                stmt.setQueryTimeout(10);
                try (ResultSet rows = stmt.executeQuery()) {
                    if (rows.next()) {
                        System.out.println("DATABASE: " + rows.getString("db_name"));
                        System.out.println("DATABASE PRINCIPAL: " + rows.getString("db_user"));
                    }
                }
            }

            String sql = "SELECT TABLE_SCHEMA, TABLE_NAME "
                    + "FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = 'dbo' "
                    + "ORDER BY TABLE_NAME";
            Set<String> names = new HashSet<>();
            int total = 0;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setQueryTimeout(10);
                try (ResultSet rows = stmt.executeQuery()) {
                    while (rows.next()) {
                        String tableName = rows.getString("TABLE_NAME");
                        names.add(tableName.toUpperCase(Locale.ROOT));
                        total++;
                        if (total <= 50) {
                            System.out.println("TABLE: dbo." + tableName);
                        }
                    }
                }
            }
            if (total > 50) {
                System.out.println("... " + (total - 50) + " additional visible dbo tables");
            }
            System.out.println("VISIBLE DBO TABLE COUNT: " + total);
            for (String required : new String[]{"NhanVien", "TaiKhoan"}) {
                if (names.contains(required.toUpperCase(Locale.ROOT))) {
                    System.out.println("PASS: dbo." + required + " visible");
                } else {
                    System.out.println("WARN: dbo." + required + " NOT visible or not created");
                }
            }
            System.out.println("R2 DB INSPECTION COMPLETE (metadata SELECT only; NO DATA MODIFIED)");
            System.out.println("NOTE: metadata visibility depends on the current SQL login permissions.");
        }
    }
}
