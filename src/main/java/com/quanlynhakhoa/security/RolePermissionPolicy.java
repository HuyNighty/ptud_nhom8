package com.quanlynhakhoa.security;

import com.quanlynhakhoa.model.Role;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolePermissionPolicy {
    private final Map<Role, Set<Permission>> grants;

    public RolePermissionPolicy() {
        EnumMap<Role, Set<Permission>> mapping = new EnumMap<>(Role.class);
        for (Role role : Role.values()) mapping.put(role, Set.of());

        mapping.put(Role.RECEPTIONIST, Set.of(
                Permission.PATIENT_READ,
                Permission.PATIENT_CREATE,
                Permission.PATIENT_UPDATE,
                Permission.PATIENT_CHECK_IN,
                Permission.APPOINTMENT_READ,
                Permission.APPOINTMENT_MANAGE,
                Permission.BILLING_READ,
                Permission.BILLING_MANAGE,
                Permission.INVOICE_CANCEL_REQUEST,
                Permission.SCHEDULE_SELF_READ));

        mapping.put(Role.DOCTOR, Set.of(
                Permission.PATIENT_READ,
                Permission.MEDICAL_RECORD_READ,
                Permission.MEDICAL_RECORD_UPDATE,
                Permission.REEXAM_SCHEDULE,
                Permission.SUPPLY_REQUEST,
                Permission.SCHEDULE_SELF_READ));

        mapping.put(Role.NURSE, Set.of(
                Permission.SUPPLY_REQUEST,
                Permission.EMERGENCY_KIT_CHECK,
                Permission.SCHEDULE_SELF_READ));

        mapping.put(Role.CLINIC_MANAGER, Set.of(
                Permission.BILLING_READ,
                Permission.WORKFORCE_READ,
                Permission.WORKFORCE_MANAGE,
                Permission.ACCOUNT_MANAGE,
                Permission.ROLE_MANAGE,
                Permission.AUDIT_READ,
                Permission.SERVICE_CATALOG_MANAGE,
                Permission.INVOICE_CANCEL_APPROVE,
                Permission.SCHEDULE_SELF_READ));

        mapping.put(Role.INVENTORY_MANAGER, Set.of(
                Permission.INVENTORY_READ,
                Permission.INVENTORY_MANAGE,
                Permission.SCHEDULE_SELF_READ));

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
