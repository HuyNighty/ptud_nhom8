package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolePermissionPolicy {
    private final Map<VaiTroHeThong, Set<Quyen>> quyenTheoVaiTro;

    public RolePermissionPolicy() {
        EnumMap<VaiTroHeThong, Set<Quyen>> mapping = new EnumMap<>(VaiTroHeThong.class);
        for (VaiTroHeThong role : VaiTroHeThong.values()) mapping.put(role, Set.of());

        mapping.put(VaiTroHeThong.RECEPTIONIST, Set.of(
                Quyen.PATIENT_READ,
                Quyen.PATIENT_CREATE,
                Quyen.PATIENT_UPDATE,
                Quyen.PATIENT_CHECK_IN,
                Quyen.APPOINTMENT_READ,
                Quyen.APPOINTMENT_MANAGE,
                Quyen.BILLING_READ,
                Quyen.BILLING_MANAGE,
                Quyen.INVOICE_CANCEL_REQUEST,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE));

        mapping.put(VaiTroHeThong.DOCTOR, Set.of(
                Quyen.PATIENT_READ,
                Quyen.MEDICAL_RECORD_READ,
                Quyen.MEDICAL_RECORD_UPDATE,
                Quyen.REEXAM_SCHEDULE,
                Quyen.SUPPLY_REQUEST,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE));

        mapping.put(VaiTroHeThong.NURSE, Set.of(
                Quyen.SUPPLY_REQUEST,
                Quyen.EMERGENCY_KIT_CHECK,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE));

        mapping.put(VaiTroHeThong.CLINIC_MANAGER, Set.of(
                Quyen.REVENUE_REPORT_READ,
                Quyen.BILLING_READ,
                Quyen.WORKFORCE_READ,
                Quyen.WORKFORCE_MANAGE,
                Quyen.ACCOUNT_MANAGE,
                Quyen.ROLE_MANAGE,
                Quyen.AUDIT_READ,
                Quyen.SERVICE_CATALOG_MANAGE,
                Quyen.INVOICE_CANCEL_APPROVE,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE));

        mapping.put(VaiTroHeThong.INVENTORY_MANAGER, Set.of(
                Quyen.INVENTORY_READ,
                Quyen.INVENTORY_MANAGE,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE));

        mapping.put(VaiTroHeThong.SYSTEM_ADMIN,
                Collections.unmodifiableSet(EnumSet.allOf(Quyen.class)));

        quyenTheoVaiTro = Collections.unmodifiableMap(mapping);
    }

    public Set<Quyen> permissionsFor(VaiTroHeThong role) {
        if (role == null) return Set.of();
        return quyenTheoVaiTro.getOrDefault(role, Set.of());
    }

    public boolean isAllowed(VaiTroHeThong role, Quyen permission) {
        return permission != null && permissionsFor(role).contains(permission);
    }
}
