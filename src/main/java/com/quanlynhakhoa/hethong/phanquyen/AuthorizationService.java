package com.quanlynhakhoa.hethong.phanquyen;

import java.util.Objects;

/** Must be called by future business services; hiding a Swing menu is not authorization. */
public final class AuthorizationService {
    private final RolePermissionPolicy policy;

    public AuthorizationService(RolePermissionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public boolean can(DesktopSession session, Permission permission) {
        return session != null && session.isActive()
                && policy.isAllowed(session.role(), permission);
    }

    public void requireCurrent(DesktopSession session, Permission permission, java.sql.Connection c) throws java.sql.SQLException {
        if(!can(session,permission))throw new AccessDeniedException();
        if(session.isServerBound())JdbcSessionVerifier.verify(c,session,true);
    }

    public void require(DesktopSession session, Permission permission) {
        if (!can(session, permission)) throw new AccessDeniedException();
        session.verifyCurrentAccount();
        if (!can(session, permission)) throw new AccessDeniedException();
    }
}
