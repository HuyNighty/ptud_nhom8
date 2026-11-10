package com.quanlynhakhoa.service.vaitro;

import com.quanlynhakhoa.dao.vaitro.PhanQuyenCoBanDao;
import com.quanlynhakhoa.model.vaitro.DongVaiTro;
import com.quanlynhakhoa.model.vaitro.DongQuyen;
import com.quanlynhakhoa.entity.VaiTro;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.JdbcSessionVerifier;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.SQLException;
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
public final class PhanQuyenCoBanService {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }


    private static final Pattern PERMISSION_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private final PhanQuyenCoBanDao dao = new PhanQuyenCoBanDao();
    private final DesktopSession session;
    private final Connections connections;

    public PhanQuyenCoBanService(DesktopSession session, Connections connections) {
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


    public List<DongVaiTro> listRoles() throws SQLException {
        requireAdmin();
        requireWorker();
        try (Connection c = connections.open()) {
            verify(c);
            dao.epoch(c);
            List<DongVaiTro> result=dao.listRoles(c);
            verify(c);
            return result;
        }
    }
    public List<DongQuyen> listPermissions() throws SQLException {
        requireAdmin();
        requireWorker();
        try (Connection c = connections.open()) {
            verify(c);
            dao.epoch(c);
            List<DongQuyen> result=dao.listPermissions(c);
            verify(c);
            return result;
        }
    }
    public void createRole(String code, String name, String description, Set<Quyen> permissions)
            throws SQLException {
        requireAdmin();
        requireWorker();
        VaiTro.requireCode(code);
        if (isLegacyRole(code)) throw new IllegalArgumentException("Không được tạo lại vai trò hệ thống.");
        String cleanName = label(name, 100, "Tên vai trò");
        String cleanDescription = description == null ? "" : description.trim();
        if (cleanDescription.length() > 500) throw new IllegalArgumentException("Mô tả tối đa 500 ký tự.");
        Objects.requireNonNull(permissions, "permissions");
        EnumSet<Quyen> selected = EnumSet.noneOf(Quyen.class);
        selected.addAll(permissions);
        transaction(c -> {
            long before = dao.epoch(c);
            dao.taoVaiTro(c,code,cleanName,cleanDescription);
            dao.ganQuyenBanDau(c,code,selected);
            // V9 triggers increment epoch in the SAME transaction; no manual epoch update.
            if (dao.epoch(c) <= before) throw new SQLException("RBAC epoch không được cập nhật; rollback.");
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
        for (Quyen known : Quyen.values()) {
            if (known.name().equals(code)) throw new IllegalArgumentException("Quyền hệ thống đã tồn tại.");
        }
        String cleanName = label(name, 100, "Tên quyền");
        transaction(c -> {
            long before = dao.epoch(c);
            dao.khaiBaoQuyen(c,code,cleanName);
            if (dao.epoch(c) <= before) throw new SQLException("RBAC epoch không được cập nhật; rollback.");
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

    @FunctionalInterface private interface SqlAction { void run(Connection c) throws SQLException; }
}
