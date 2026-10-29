package com.quanlynhakhoa.navigation;

import com.quanlynhakhoa.security.Permission;

public enum Module {
    HOME(null),
    PATIENTS(Permission.PATIENT_READ),
    MEDICAL_RECORDS(Permission.MEDICAL_RECORD_READ),
    APPOINTMENTS(Permission.APPOINTMENT_READ),
    BILLING(Permission.BILLING_READ),
    INVENTORY(Permission.INVENTORY_READ),
    WORKFORCE(Permission.WORKFORCE_READ),
    ACCOUNTS(Permission.ACCOUNT_MANAGE),
    ROLES(Permission.ROLE_MANAGE),
    AUDIT(Permission.AUDIT_READ),
    EXAMINATIONS(Permission.MEDICAL_RECORD_READ),
    SERVICE_CATALOG(Permission.SERVICE_CATALOG_MANAGE),
    SUPPLY_REQUESTS(Permission.SUPPLY_REQUEST),
    MY_SCHEDULE(Permission.SCHEDULE_SELF_READ),
    EMERGENCY_KIT(Permission.EMERGENCY_KIT_CHECK);

    private final Permission openPermission;

    Module(Permission openPermission) {
        this.openPermission = openPermission;
    }

    public Permission openPermission() {
        return openPermission;
    }
}
