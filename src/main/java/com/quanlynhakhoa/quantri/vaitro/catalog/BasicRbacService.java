package com.quanlynhakhoa.quantri.vaitro.catalog;

import com.quanlynhakhoa.model.vaitro.DongVaiTro;
import com.quanlynhakhoa.model.vaitro.DongQuyen;
import com.quanlynhakhoa.entity.VaiTro;
import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.JdbcSessionVerifier;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Local-only RBAC catalog editor. Does not activate dynamic authorization or assign accounts.
 * Writes require a live, server-bound SYSTEM_ADMIN and use an isolated caller-owned SQL transaction.
 * V9 schema/triggers and least-privilege grants must have been independently verified first.
 */
public final class BasicRbacService {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }


    private static final Pattern PERMISSION_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private final DesktopSession session;
    private final Connections connections;

    public BasicRbacService(DesktopSession session, Connections connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    private void requireAdmin() {
        // A synthetic/unbound in-memory session must never be authorized for catalog writes.
        if (!session.isActive() || session.role() != VaiTroHeThong.SYSTEM_ADMIN || !session.isServerBound()) {
            throw new AccessDeniedException();
        }
    }
    private static void requireWorker() throws SQLException {
        if (EventQueue.isDispatchThread()) throw new SQLException("Không được truy cập SQL Server trên Swing EDT.");
    }
    private void verify(Connection connection) throws SQLException {
        requireAdmin();
        JdbcSessionVerifier.verify(connection, session, true);
        requireAdmin();
    }
    private static PreparedStatement statement(Connection c, String sql) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setQueryTimeout(10);
        return ps;
    }
    private static long epoch(Connection c) throws SQLException {
        try (PreparedStatement ps = statement(c, "SELECT epoch FROM dbo.RbacEpoch WHERE singletonId=1");
             ResultSet rows = ps.executeQuery()) {
            if (!rows.next()) throw new SQLException("RBAC V9 chưa sẵn sàng: thiếu epoch.");
            long value = rows.getLong(1);
            if (rows.wasNull() || value < 1 || rows.next()) throw new SQLException("RBAC epoch không hợp lệ.");
            return value;
        }
    }
    public List<DongVaiTro> listRoles() throws SQLException {
        requireAdmin();
        requireWorker();
        try (Connection c = connections.open()) {
            verify(c);
            epoch(c);
            List<DongVaiTro> result = new ArrayList<>();
            try (PreparedStatement ps = statement(c,
                    "SELECT roleCode,roleName,isSystem,enabled FROM dbo.VaiTro ORDER BY roleCode");
                 ResultSet rows = ps.executeQuery()) {
                while (rows.next()) result.add(new DongVaiTro(rows.getString(1), rows.getNString(2),
                        rows.getBoolean(3), rows.getBoolean(4)));
            }
            verify(c);
            return List.copyOf(result);
        }
    }
    public List<DongQuyen> listPermissions() throws SQLException {
        requireAdmin();
        requireWorker();
        try (Connection c = connections.open()) {
            verify(c);
            epoch(c);
            List<DongQuyen> result = new ArrayList<>();
            try (PreparedStatement ps = statement(c,
                    "SELECT permissionCode,permissionName FROM dbo.Quyen ORDER BY permissionCode");
                 ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    String code = rows.getString(1);
                    if (code == null || !PERMISSION_CODE.matcher(code).matches())
                        throw new SQLException("RBAC permission code không hợp lệ.");
                    boolean builtIn = false;
                    for (Permission p : Permission.values()) if (p.name().equals(code)) builtIn = true;
                    result.add(new DongQuyen(code, rows.getNString(2), builtIn));
                }
            }
            verify(c);
            return List.copyOf(result);
        }
    }
    public void createRole(String code, String name, String description, Set<Permission> permissions)
            throws SQLException {
        requireAdmin();
        requireWorker();
        VaiTro.requireCode(code);
        if (isLegacyRole(code)) throw new IllegalArgumentException("Không được tạo lại vai trò hệ thống.");
        String cleanName = label(name, 100, "Tên vai trò");
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.length() > 500) throw new IllegalArgumentException("Mô tả tối đa 500 ký tự.");
        Objects.requireNonNull(permissions, "permissions");
        EnumSet<Permission> selected = EnumSet.noneOf(Permission.class);
        selected.addAll(permissions);
        transaction(c -> {
            long before = epoch(c);
            try (PreparedStatement ps = statement(c,
                    "INSERT INTO dbo.VaiTro(roleCode,roleName,description,enabled,isSystem,isProtected) " +
                            "VALUES(?,?,?,1,0,0)")) {
                ps.setString(1, code);
                ps.setNString(2, cleanName);
                ps.setNString(3, cleanDescription);
                exactlyOne(ps.executeUpdate());
            }
            try (PreparedStatement grant = statement(c,
                    "INSERT INTO dbo.VaiTroQuyen(roleCode,permissionCode) " +
                            "SELECT ?,permissionCode FROM dbo.Quyen WHERE permissionCode=?")) {
                for (Permission permission : selected) {
                    grant.setString(1, code);
                    grant.setString(2, permission.name());
                    exactlyOne(grant.executeUpdate());
                }
            }
            // V9 triggers increment epoch in the SAME transaction; no manual epoch update.
            if (epoch(c) <= before) throw new SQLException("RBAC epoch không được cập nhật; rollback.");
        });
    }
    /**
     * Declares a catalog permission only. A newly created code is NOT enforced by business code,
     * not included in the Java Permission enum, and not automatically granted to SYSTEM_ADMIN.
     */
    public void declarePermission(String code, String name) throws SQLException {
        requireAdmin();
        requireWorker();
        if (code == null || !PERMISSION_CODE.matcher(code).matches())
            throw new IllegalArgumentException("Mã quyền phải là A-Z, 0-9, _, tối đa 64 ký tự.");
        for (Permission known : Permission.values()) {
            if (known.name().equals(code)) throw new IllegalArgumentException("Quyền hệ thống đã tồn tại.");
        }
        String cleanName = label(name, 100, "Tên quyền");
        transaction(c -> {
            long before = epoch(c);
            try (PreparedStatement ps = statement(c,
                    "INSERT INTO dbo.Quyen(permissionCode,permissionName) VALUES(?,?)")) {
                ps.setString(1, code);
                ps.setNString(2, cleanName);
                exactlyOne(ps.executeUpdate());
            }
            if (epoch(c) <= before) throw new SQLException("RBAC epoch không được cập nhật; rollback.");
        });
    }
    private void transaction(SqlAction action) throws SQLException {
        try (Connection c = connections.open()) {
            if (!c.getAutoCommit()) throw new SQLException("Expected fresh auto-commit connection.");
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            c.setAutoCommit(false);
            try {
                verify(c);
                action.run(c);
                verify(c);
                c.commit();
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }
    private static String label(String value, int length, String title) {
        if (value == null || value.isBlank() || value.trim().length() > length)
            throw new IllegalArgumentException(title + " phải có 1–" + length + " ký tự.");
        return value.trim();
    }
    private static boolean isLegacyRole(String code) {
        for (VaiTroHeThong role : VaiTroHeThong.values()) if (role.name().equals(code)) return true;
        return false;
    }
    private static void exactlyOne(int n) throws SQLException {
        if (n != 1) throw new SQLException("Không thể ghi danh mục RBAC hợp lệ.");
    }
    @FunctionalInterface private interface SqlAction { void run(Connection c) throws SQLException; }
}
