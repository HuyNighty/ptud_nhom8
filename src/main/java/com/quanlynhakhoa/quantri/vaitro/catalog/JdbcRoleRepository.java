package com.quanlynhakhoa.quantri.vaitro.catalog;

import com.quanlynhakhoa.entity.VaiTro;
import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Unwired, SELECT-only JDBC catalog reader for a separately verified V8 database.
 * Never opens a connection, migrates, authorizes, loads grants or commits.
 */
public final class JdbcRoleRepository implements RoleRepository {
    private static final String COLUMNS =
            "roleCode, roleName, description, enabled, isSystem, isProtected, revision";
    private static final String FIND = "SELECT " + COLUMNS + " FROM dbo.VaiTro WHERE roleCode = ?";
    private static final String ALL = "SELECT " + COLUMNS + " FROM dbo.VaiTro ORDER BY roleCode";

    private static void requireWorker(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        if (EventQueue.isDispatchThread())
            throw new SQLException("Không được đọc catalog trên Swing EDT.");
    }

    @Override public Optional<VaiTro> findByCode(Connection connection, String code) throws SQLException {
        VaiTro.requireCode(code);
        requireWorker(connection);
        try (PreparedStatement statement = connection.prepareStatement(FIND)) {
            statement.setQueryTimeout(10);
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                VaiTro role = map(result);
                if (!role.code().equals(code) || result.next())
                    throw new SQLException("Kết quả tra cứu vai trò không nhất quán.");
                return Optional.of(role);
            }
        }
    }

    @Override public List<VaiTro> findAll(Connection connection) throws SQLException {
        requireWorker(connection);
        try (PreparedStatement statement = connection.prepareStatement(ALL)) {
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery()) {
                List<VaiTro> roles = new ArrayList<>();
                HashSet<String> seen = new HashSet<>();
                while (result.next()) {
                    VaiTro role = map(result);
                    if (!seen.add(role.code())) throw new SQLException("Mã vai trò bị trùng trong catalog.");
                    roles.add(role);
                }
                return List.copyOf(roles);
            }
        }
    }

    private static boolean flag(ResultSet result, String column) throws SQLException {
        boolean value = result.getBoolean(column);
        if (result.wasNull()) throw new SQLException("Cờ vai trò không được null.");
        return value;
    }

    private static VaiTro map(ResultSet result) throws SQLException {
        try {
            return new VaiTro(result.getString("roleCode"), result.getString("roleName"),
                    result.getString("description"), flag(result, "enabled"),
                    flag(result, "isSystem"), flag(result, "isProtected"), result.getBytes("revision"));
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("Dữ liệu catalog vai trò không hợp lệ.", invalid);
        }
    }
}

