package com.quanlynhakhoa.roles;

import com.quanlynhakhoa.account.AccountDirectoryService;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The existing RolePermissionPolicy remains the only source of authority. */
public final class RoleOverviewService {
    private final DesktopSession session;
    private final AccountDirectoryService.ConnectionProvider connections;
    private final RolePermissionPolicy policy = new RolePermissionPolicy();
    private final AuthorizationService authorization = new AuthorizationService(policy);
    private final RoleOverviewDao dao = new RoleOverviewDao();

    public RoleOverviewService(DesktopSession session,
                               AccountDirectoryService.ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public List<RoleOverview> load() throws SQLException {
        requireRead();
        try (Connection connection = connections.open()) {
            requireRead();
            Map<Role, Integer> counts = dao.countAccountsByRole(connection);
            requireRead();
            List<RoleOverview> rows = new ArrayList<>();
            for (Role role : Role.values()) {
                rows.add(new RoleOverview(role, policy.permissionsFor(role), counts.get(role)));
            }
            requireRead();
            return List.copyOf(rows);
        }
    }

    private void requireRead() {
        // Role-management permission controls access to the security policy overview.
        authorization.require(session, Permission.ROLE_MANAGE);
    }
}
