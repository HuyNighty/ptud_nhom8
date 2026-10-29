package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class AccountDirectoryService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final AccountDirectoryDao dao = new AccountDirectoryDao();

    public AccountDirectoryService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public AccountDirectoryDao.Result search(String keyword, Role role,
                                             AccountStatus status) throws SQLException {
        authorization.require(session, Permission.ACCOUNT_MANAGE);
        String normalized = Objects.requireNonNull(keyword, "keyword").trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("Search keyword must have at most 80 characters");
        }
        try (Connection connection = connections.open()) {
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            AccountDirectoryDao.Result result = dao.search(connection, normalized, role, status);
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            return result;
        }
    }
}
