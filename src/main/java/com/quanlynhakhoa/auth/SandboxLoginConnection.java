package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.config.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** R3-A3 guard: authenticated GUI must never issue writes against N08Dental. */
public final class SandboxLoginConnection {
    private SandboxLoginConnection() { }

    public static Connection open() throws SQLException {
        Connection connection = Database.open();
        try {
            String name;
            try (PreparedStatement query = connection.prepareStatement("SELECT DB_NAME()")) {
                query.setQueryTimeout(5);
                try (ResultSet results = query.executeQuery()) {
                    if (!results.next()) throw new SQLException("Missing catalog identity");
                    name = results.getString(1);
                }
            }
            if (name == null || !name.matches("N08AuthIT_[0-9a-fA-F]{8}")) {
                throw new SQLException("R3-A3 permits isolated N08AuthIT databases only");
            }
            return connection;
        } catch (SQLException | RuntimeException failure) {
            try { connection.close(); } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }
}
