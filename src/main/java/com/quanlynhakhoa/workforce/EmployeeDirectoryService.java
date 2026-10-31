package com.quanlynhakhoa.workforce;

import com.quanlynhakhoa.model.EmployeeStatus;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Permission enforcement belongs here, not merely in the Swing menu. */
public final class EmployeeDirectoryService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final EmployeeDirectoryDao dao = new EmployeeDirectoryDao();

    public EmployeeDirectoryService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public EmployeeDirectoryDao.Result search(String keyword, EmployeeStatus status) throws SQLException {
        authorization.require(session, Permission.WORKFORCE_READ);
        String normalized = Objects.requireNonNull(keyword, "keyword").trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 80 ký tự.");
        }
        try (Connection connection = connections.open()) {
            authorization.require(session, Permission.WORKFORCE_READ);
            EmployeeDirectoryDao.Result result = dao.search(connection, normalized, status);
            authorization.require(session, Permission.WORKFORCE_READ);
            return result;
        }
    }
}
