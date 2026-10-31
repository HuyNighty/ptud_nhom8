package com.quanlynhakhoa.account;

import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Restricts eligible-employee discovery to account managers. */
public final class EligibleEmployeeService {
    @FunctionalInterface public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final EligibleEmployeeDao dao = new EligibleEmployeeDao();

    public EligibleEmployeeService(DesktopSession session, ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public EligibleEmployeeDao.Result listEligible() throws SQLException {
        authorization.require(session, Permission.ACCOUNT_MANAGE);
        try (Connection connection = connections.open()) {
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            EligibleEmployeeDao.Result result = dao.findEligible(connection);
            authorization.require(session, Permission.ACCOUNT_MANAGE);
            return result;
        }
    }
}
