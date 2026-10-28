package com.quanlynhakhoa.security;

import com.quanlynhakhoa.model.Role;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Conservative port of the previous role-to-permission mapping.
 * Unknown or not-yet-approved grants remain DENIED, including appointments/billing.
 */
public final class RolePermissionPolicy {
    private final Map<Role, Set<Permission>> grants;

    public RolePermissionPolicy() {
        EnumMap<Role, Set<Permission>> mapping = new EnumMap<>(Role.class);
        for (Role role : Role.values()) mapping.put(role, Set.of());
        mapping.put(Role.RECEPTIONIST, Set.of(
                Permission.PATIENT_READ, Permission.PATIENT_CREATE, Permission.PATIENT_UPDATE));
        mapping.put(Role.DOCTOR, Set.of(
                Permission.PATIENT_READ, Permission.MEDICAL_RECORD_READ,
                Permission.MEDICAL_RECORD_UPDATE));
        mapping.put(Role.SYSTEM_ADMIN,
                Collections.unmodifiableSet(EnumSet.allOf(Permission.class)));
        grants = Collections.unmodifiableMap(mapping);
    }

    public Set<Permission> permissionsFor(Role role) {
        if (role == null) return Set.of();
        return grants.getOrDefault(role, Set.of());
    }

    public boolean isAllowed(Role role, Permission permission) {
        return permission != null && permissionsFor(role).contains(permission);
    }
}
