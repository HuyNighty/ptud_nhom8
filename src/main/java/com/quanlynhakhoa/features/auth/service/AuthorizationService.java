package com.quanlynhakhoa.features.auth.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;

import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public final class AuthorizationService {
    private final RolePermissionPolicy policy;

    public AuthorizationService(RolePermissionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public boolean can(Set<Role> authenticatedRoles, Permission permission) {
        return policy.isAllowed(authenticatedRoles, permission);
    }

    public void require(Set<Role> authenticatedRoles, Permission permission) {
        if (!can(authenticatedRoles, permission)) {
            throw new PermissionDeniedException(permission);
        }
    }
}
