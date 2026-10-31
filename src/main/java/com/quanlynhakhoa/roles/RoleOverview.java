package com.quanlynhakhoa.roles;

import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.Permission;
import java.util.Set;

/** Read-only current JVM RBAC policy plus total accounts in the development database. */
public record RoleOverview(Role role, Set<Permission> permissions, int accountCount) {
    public RoleOverview {
        if (role == null || permissions == null || accountCount < 0) {
            throw new IllegalArgumentException("Invalid role overview");
        }
        permissions = Set.copyOf(permissions);
    }
}
