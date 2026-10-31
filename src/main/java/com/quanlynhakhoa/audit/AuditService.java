package com.quanlynhakhoa.audit;

import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Enforce AUDIT_READ before opening JDBC and after reading it (including logout races). */
public final class AuditService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuditDao dao;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());

    public AuditService(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, new AuditDao());
    }

    AuditService(DesktopSession session, ConnectionProvider connections, AuditDao dao) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public AuditDao.Result search(AuditFilter filter) throws SQLException {
        authorization.require(session, Permission.AUDIT_READ);
        Objects.requireNonNull(filter, "filter");
        try (Connection connection = connections.open()) {
            authorization.require(session, Permission.AUDIT_READ);
            AuditDao.Result result = dao.search(connection, filter);
            authorization.require(session, Permission.AUDIT_READ);
            return result;
        }
    }
}
