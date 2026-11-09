package com.quanlynhakhoa.enums;

import com.quanlynhakhoa.enums.Quyen;

public enum Module {

    HOME(null),
    PATIENTS(Quyen.PATIENT_READ),
    MEDICAL_RECORDS(Quyen.MEDICAL_RECORD_READ),
    APPOINTMENTS(Quyen.APPOINTMENT_READ),
    BILLING(Quyen.BILLING_READ),
    INVENTORY(Quyen.INVENTORY_READ),
    WORKFORCE(Quyen.WORKFORCE_READ),
    ACCOUNTS(Quyen.ACCOUNT_MANAGE),
    ROLES(Quyen.ROLE_MANAGE),
    AUDIT(Quyen.AUDIT_READ),
    EXAMINATIONS(Quyen.MEDICAL_RECORD_READ),
    TREATMENT_PLANS(Quyen.MEDICAL_RECORD_READ),
    SERVICE_CATALOG(Quyen.SERVICE_CATALOG_MANAGE),
    SUPPLY_REQUESTS(Quyen.SUPPLY_REQUEST),
    MY_SCHEDULE(Quyen.SCHEDULE_SELF_READ),
    EMERGENCY_KIT(Quyen.EMERGENCY_KIT_CHECK),
    SERVICES(Quyen.SERVICE_CATALOG_MANAGE),
    DEBTS(Quyen.BILLING_READ),
    PAYMENTS(Quyen.BILLING_MANAGE),
    INVOICE_CANCELLATIONS(Quyen.INVOICE_CANCEL_APPROVE),
    RECEPTION(Quyen.PATIENT_CHECK_IN),
    SHIFT_ASSIGNMENTS(Quyen.WORKFORCE_READ),
    REVENUE_REPORTS(Quyen.REVENUE_REPORT_READ);

    private final Quyen openPermission;

    Module(Quyen openPermission) {
        this.openPermission = openPermission;
    }

    public Quyen openPermission() {
        return openPermission;
    }

}
