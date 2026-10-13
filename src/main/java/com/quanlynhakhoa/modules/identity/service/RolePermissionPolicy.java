package com.quanlynhakhoa.modules.identity.service;

import com.quanlynhakhoa.modules.identity.enums.Permission;
import com.quanlynhakhoa.modules.identity.enums.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class RolePermissionPolicy {
    private final Map<Role, Set<Permission>> grants;

    public RolePermissionPolicy() {
        EnumMap<Role, Set<Permission>> mapping = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            mapping.put(role, Set.of());
        }

        mapping.put(Role.RECEPTIONIST, Set.of(
                Permission.PATIENT_READ, Permission.PATIENT_CREATE, Permission.PATIENT_UPDATE));
        mapping.put(Role.DOCTOR, Set.of(
                Permission.PATIENT_READ, Permission.MEDICAL_RECORD_READ,
                Permission.MEDICAL_RECORD_UPDATE));

        mapping.put(Role.SYSTEM_ADMIN,
                Collections.unmodifiableSet(EnumSet.allOf(Permission.class)));
        grants = Collections.unmodifiableMap(mapping);
    }

    public Set<Permission> permissionsFor(Set<Role> roles) {
        Objects.requireNonNull(roles, "roles must not be null");
        EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
        for (Role role : roles) {
            Objects.requireNonNull(role, "role must not be null");
            result.addAll(grants.getOrDefault(role, Set.of()));
        }
        return Collections.unmodifiableSet(result);
    }

    public boolean isAllowed(Set<Role> roles, Permission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        return permissionsFor(roles).contains(permission);
    }
}
