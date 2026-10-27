package com.quanlynhakhoa.security;

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

    public void require(DesktopSession session, Permission permission) {
        if (!can(session, permission)) throw new AccessDeniedException();
    }
}
