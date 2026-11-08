package com.quanlynhakhoa.quantri.vaitro;

import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
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
