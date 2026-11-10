package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.security.NavigationPolicy;
import java.util.Set;
import java.util.List;

/** All-synthetic, no GUI, no DB, no secret values. */
public final class R4RoleSessionSelfTest {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError("CHECK " + checks + ": " + description);
    }

    private static void denied(Runnable action) {
        boolean denied = false;
        try { action.run(); } catch (AccessDeniedException expected) { denied = true; }
        check(denied, "AccessDeniedException expected");
    }

    private static DesktopSession session(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "synthetic-account", role, false));
    }

    public static void main(String[] args) {
        RolePermissionPolicy roles = new RolePermissionPolicy();
        AuthorizationService service = new AuthorizationService(roles);
        NavigationPolicy nav = new NavigationPolicy(service);
        check(VaiTroHeThong.values().length == 6, "Unexpected new role requires policy review");
        check(Quyen.values().length == 26, "Unexpected new permission requires policy review");
        check(Module.values().length == 23, "Unexpected module requires policy review");

        Set<Quyen> receptionist = Set.of(
                Quyen.PATIENT_READ, Quyen.PATIENT_CREATE, Quyen.PATIENT_UPDATE,
                Quyen.PATIENT_CHECK_IN, Quyen.APPOINTMENT_READ, Quyen.APPOINTMENT_MANAGE,
                Quyen.BILLING_READ, Quyen.BILLING_MANAGE, Quyen.INVOICE_CANCEL_REQUEST,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE);
        Set<Quyen> doctor = Set.of(
                Quyen.PATIENT_READ, Quyen.MEDICAL_RECORD_READ, Quyen.MEDICAL_RECORD_UPDATE,
                Quyen.REEXAM_SCHEDULE, Quyen.SUPPLY_REQUEST, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE);
        Set<Quyen> nurse = Set.of(
                Quyen.SUPPLY_REQUEST, Quyen.EMERGENCY_KIT_CHECK, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE);
        Set<Quyen> manager = Set.of(
                Quyen.REVENUE_REPORT_READ, Quyen.BILLING_READ, Quyen.WORKFORCE_READ, Quyen.WORKFORCE_MANAGE,
                Quyen.ACCOUNT_MANAGE, Quyen.ROLE_MANAGE, Quyen.AUDIT_READ,
                Quyen.SERVICE_CATALOG_MANAGE, Quyen.INVOICE_CANCEL_APPROVE,
                Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE);
        Set<Quyen> inventory = Set.of(
                Quyen.INVENTORY_READ, Quyen.INVENTORY_MANAGE, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE);
        for (VaiTroHeThong role : VaiTroHeThong.values()) {
            for (Quyen permission : Quyen.values()) {
                boolean expected = switch (role) {
                    case RECEPTIONIST -> receptionist.contains(permission);
                    case DOCTOR -> doctor.contains(permission);
                    case NURSE -> nurse.contains(permission);
                    case CLINIC_MANAGER -> manager.contains(permission);
                    case INVENTORY_MANAGER -> inventory.contains(permission);
                    case SYSTEM_ADMIN -> true;
                };
                check(roles.isAllowed(role, permission) == expected,
                        "Grant mismatch: " + role + "/" + permission);
            }
        }
        check(!roles.isAllowed(null, Quyen.PATIENT_READ), "Null role denied");
        check(!roles.isAllowed(VaiTroHeThong.SYSTEM_ADMIN, null), "Null permission denied");
        check(roles.permissionsFor(VaiTroHeThong.SYSTEM_ADMIN).size() == Quyen.values().length,
                "All admin permissions");
        check(roles.permissionsFor(VaiTroHeThong.RECEPTIONIST).equals(receptionist), "Reception permissions");
        check(roles.permissionsFor(VaiTroHeThong.DOCTOR).equals(doctor), "Doctor permissions");
        check(roles.permissionsFor(VaiTroHeThong.NURSE).equals(nurse), "Nurse permissions");
        check(roles.permissionsFor(VaiTroHeThong.CLINIC_MANAGER).equals(manager), "Manager permissions");
        check(roles.permissionsFor(VaiTroHeThong.INVENTORY_MANAGER).equals(inventory), "Inventory permissions");
        try {
            roles.permissionsFor(VaiTroHeThong.DOCTOR).clear();
            throw new AssertionError("Grant set must be immutable");
        } catch (UnsupportedOperationException expected) {
            check(true, "Grant set immutable");
        }

        denied(() -> DesktopSession.fromSuccessfulLogin(KetQuaDangNhap.rejected()));
        denied(() -> DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "test", VaiTroHeThong.DOCTOR, true)));
        denied(() -> DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, " ", VaiTroHeThong.DOCTOR, false)));
        denied(() -> DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "test", null, false)));

        for (VaiTroHeThong role : VaiTroHeThong.values()) {
            DesktopSession session = session(role);
            check(session.isActive(), "Session active");
            check(session.accountId().equals("synthetic-account"), "Session account bound");
            check(session.role() == role, "Session role bound");
            check(session.startedAt() != null, "Session has start instant");
            check(!session.toString().contains("synthetic-account"), "Do not print account ID");
            check(nav.canOpen(session, Module.HOME), "Authenticated home visible");
            for (Module module : Module.values()) {
                boolean expected = module == Module.HOME
                        || roles.isAllowed(role, module.openPermission());
                check(nav.canOpen(session, module) == expected,
                        "Nav grant mismatch: " + role + "/" + module);
                check(nav.visibleModules(session).contains(module) == expected,
                        "Nav projection mismatch: " + role + "/" + module);
                if (expected) nav.requireOpen(session, module);
                else denied(() -> nav.requireOpen(session, module));
            }
            for (Quyen permission : Quyen.values()) {
                check(service.can(session, permission) == roles.isAllowed(role, permission),
                        "Service permission mismatch: " + role + "/" + permission);
            }
            session.logout();
            check(!session.isActive(), "Logout closes session");
            check(nav.visibleModules(session).isEmpty(), "No navigation after logout");
            check(!nav.canOpen(session, Module.HOME), "Home denied after logout");
            check(!service.can(session, Quyen.PATIENT_READ), "Service denied after logout");
            denied(() -> nav.requireOpen(session, Module.HOME));
            denied(() -> service.require(session, Quyen.PATIENT_READ));
            session.logout();
            check(!session.isActive(), "Logout idempotent");
        }
        check(nav.visibleModules(null).isEmpty(), "No unauthenticated navigation");
        check(!nav.canOpen(null, Module.HOME), "Unauthenticated home denied");
        check(!nav.canOpen(session(VaiTroHeThong.SYSTEM_ADMIN), null), "Null module denied");
        check(!service.can(null, Quyen.PATIENT_READ), "Null session denied");
        check(!service.can(session(VaiTroHeThong.RECEPTIONIST), null), "Null permission denied");
        denied(() -> service.require(null, Quyen.PATIENT_READ));

        check(nav.visibleModules(session(VaiTroHeThong.RECEPTIONIST)).equals(
                List.of(Module.HOME, Module.PATIENTS, Module.APPOINTMENTS, Module.BILLING,
                        Module.MY_SCHEDULE, Module.DEBTS, Module.PAYMENTS, Module.RECEPTION)),
                "Receptionist allowed modules");
        check(nav.visibleModules(session(VaiTroHeThong.DOCTOR)).equals(
                List.of(Module.HOME, Module.PATIENTS, Module.MEDICAL_RECORDS,
                        Module.EXAMINATIONS, Module.TREATMENT_PLANS, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE)), "Doctor safe list");
        check(nav.visibleModules(session(VaiTroHeThong.NURSE)).equals(List.of(
                Module.HOME, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE, Module.EMERGENCY_KIT)),
                "Nurse routes are restricted to assigned activities");
        check(nav.visibleModules(session(VaiTroHeThong.CLINIC_MANAGER)).equals(
                List.of(Module.HOME, Module.BILLING, Module.WORKFORCE,
                        Module.ACCOUNTS, Module.ROLES, Module.AUDIT,
                        Module.SERVICE_CATALOG, Module.MY_SCHEDULE, Module.SERVICES, Module.DEBTS, Module.INVOICE_CANCELLATIONS, Module.SHIFT_ASSIGNMENTS, Module.REVENUE_REPORTS)), "Manager allowed modules");
        check(nav.visibleModules(session(VaiTroHeThong.INVENTORY_MANAGER)).equals(
                List.of(Module.HOME, Module.INVENTORY, Module.MY_SCHEDULE)), "Inventory allowed modules");
        check(nav.visibleModules(session(VaiTroHeThong.SYSTEM_ADMIN)).size() == Module.values().length,
                "Admin navigation covers all modules");

        System.out.println("PASS: R4-A1 ROLE, SESSION AND NAVIGATION SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO GUI, NO JDBC, NO SQL WRITES, NO REAL ACCOUNTS");
    }
}
