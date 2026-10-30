package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.LockSource;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Administrator locks affect new logins; existing desktop sessions are not revoked. */
public final class AccountStateService {
    private final DesktopSession session;
    private final AccountDirectoryService.ConnectionProvider connections;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final AccountStateDao dao = new AccountStateDao();

    public AccountStateService(DesktopSession session, AccountDirectoryService.ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public void lock(String targetId, String reason) throws SQLException {
        requireAuthorized();
        String normalized = reason == null ? "" : reason.strip();
        if (normalized.isBlank() || normalized.length() > 300) {
            throw new IllegalArgumentException("Lý do khóa phải có từ 1 đến 300 ký tự.");
        }
        change(targetId, true, normalized);
    }

    public void unlock(String targetId) throws SQLException {
        change(targetId, false, null);
    }

    private void change(String targetId, boolean locking, String reason) throws SQLException {
        requireAuthorized();
        String id = targetId == null ? "" : targetId.strip();
        if (id.isBlank() || id.length() > 10) {
            throw new IllegalArgumentException("Mã tài khoản không hợp lệ.");
        }
        if (locking && session.accountId().equalsIgnoreCase(id)) {
            throw new IllegalArgumentException("Không được tự khóa tài khoản đang đăng nhập.");
        }
        try (Connection connection = connections.open()) {
            if (!connection.getAutoCommit()) throw new SQLException("Expected a fresh JDBC connection");
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                requireAuthorized();
                AccountStateDao.Target target = dao.lockTarget(connection, id);
                requireAuthorized();
                if (target.role() == Role.SYSTEM_ADMIN) {
                    throw new IllegalArgumentException("Không được khóa hoặc mở khóa tài khoản quản trị hệ thống.");
                }
                if (locking) {
                    if (session.accountId().equalsIgnoreCase(target.accountId())) {
                        throw new IllegalArgumentException("Không được tự khóa tài khoản đang đăng nhập.");
                    }
                    if (target.status() != AccountStatus.HOAT_DONG) {
                        throw new IllegalArgumentException("Chỉ được khóa tài khoản đang hoạt động. Hãy làm mới danh sách.");
                    }
                    dao.lock(connection, target.accountId(), session.accountId(), reason);
                } else {
                    if (target.status() != AccountStatus.BI_KHOA || target.lockSource() != LockSource.ADMIN_ACTION) {
                        throw new IllegalArgumentException("Chỉ được mở khóa tài khoản do quản trị viên khóa. Hãy làm mới danh sách.");
                    }
                    dao.unlock(connection, target.accountId());
                }
                requireAuthorized();
                dao.audit(connection, session.accountId(), target.accountId(), locking, reason);
                requireAuthorized();
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        }
    }

    private void requireAuthorized() {
        authorization.require(session, Permission.ACCOUNT_MANAGE);
    }
}
