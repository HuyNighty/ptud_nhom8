package com.quanlynhakhoa.roles;

import com.quanlynhakhoa.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Role counts only. Never reads credentials, changes role assignments, or writes SQL. */
public final class RoleOverviewDao {
    static final String COUNT_SQL = """
            SELECT vaiTro, COUNT(*) AS soLuong
            FROM dbo.TaiKhoan
            GROUP BY vaiTro
            """;

    public Map<Role, Integer> countAccountsByRole(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        EnumMap<Role, Integer> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) counts.put(role, 0);
        try (PreparedStatement statement = connection.prepareStatement(COUNT_SQL)) {
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String name = result.getString("vaiTro");
                    final Role role;
                    try {
                        role = Role.valueOf(name);
                    } catch (IllegalArgumentException | NullPointerException invalid) {
                        throw new SQLException("Unexpected role in the account database", invalid);
                    }
                    int count = result.getInt("soLuong");
                    if (result.wasNull() || count < 0) {
                        throw new SQLException("Invalid account count for role");
                    }
                    counts.put(role, count);
                }
            }
        }
        return Map.copyOf(counts);
    }
}
