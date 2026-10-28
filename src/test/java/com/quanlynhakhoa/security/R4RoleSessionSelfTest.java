package com.quanlynhakhoa.security;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.navigation.NavigationPolicy;
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

    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "synthetic-account", role, false));
    }

    public static void main(String[] args) {
        RolePermissionPolicy roles = new RolePermissionPolicy();
        AuthorizationService service = new AuthorizationService(roles);
        NavigationPolicy nav = new NavigationPolicy(service);
        check(Role.values().length == 6, "Unexpected new role requires policy review");
        check(Permission.values().length == 16, "Unexpected new permission requires policy review");
        check(Module.values().length == 10, "Unexpected module requires policy review");

        Set<Permission> receptionist = Set.of(Permission.PATIENT_READ, Permission.PATIENT_CREATE,
                Permission.PATIENT_UPDATE);
        Set<Permission> doctor = Set.of(Permission.PATIENT_READ,
                Permission.MEDICAL_RECORD_READ, Permission.MEDICAL_RECORD_UPDATE);
        for (Role role : Role.values()) {
            for (Permission permission : Permission.values()) {
                boolean expected = switch (role) {
                    case RECEPTIONIST -> receptionist.contains(permission);
                    case DOCTOR -> doctor.contains(permission);
                    case SYSTEM_ADMIN -> true;
                    default -> false;
                };
                check(roles.isAllowed(role, permission) == expected,
                        "Grant mismatch: " + role + "/" + permission);
            }
        }
        check(!roles.isAllowed(null, Permission.PATIENT_READ), "Null role denied");
        check(!roles.isAllowed(Role.SYSTEM_ADMIN, null), "Null permission denied");
        check(roles.permissionsFor(Role.SYSTEM_ADMIN).size() == Permission.values().length,
                "All admin permissions");
        check(roles.permissionsFor(Role.RECEPTIONIST).equals(receptionist), "Reception permissions");
        check(roles.permissionsFor(Role.DOCTOR).equals(doctor), "Doctor permissions");
        try {
            roles.permissionsFor(Role.DOCTOR).clear();
            throw new AssertionError("Grant set must be immutable");
        } catch (UnsupportedOperationException expected) {
            check(true, "Grant set immutable");
        }

        denied(() -> DesktopSession.fromSuccessfulLogin(AuthDecision.rejected()));
        denied(() -> DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "test", Role.DOCTOR, true)));
        denied(() -> DesktopSession.fromSuccessfulLogin(new AuthDecision(true, " ", Role.DOCTOR, false)));
        denied(() -> DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "test", null, false)));

        for (Role role : Role.values()) {
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
            for (Permission permission : Permission.values()) {
                check(service.can(session, permission) == roles.isAllowed(role, permission),
                        "Service permission mismatch: " + role + "/" + permission);
            }
            session.logout();
            check(!session.isActive(), "Logout closes session");
            check(nav.visibleModules(session).isEmpty(), "No navigation after logout");
            check(!nav.canOpen(session, Module.HOME), "Home denied after logout");
            check(!service.can(session, Permission.PATIENT_READ), "Service denied after logout");
            denied(() -> nav.requireOpen(session, Module.HOME));
            denied(() -> service.require(session, Permission.PATIENT_READ));
            session.logout();
            check(!session.isActive(), "Logout idempotent");
        }
        check(nav.visibleModules(null).isEmpty(), "No unauthenticated navigation");
        check(!nav.canOpen(null, Module.HOME), "Unauthenticated home denied");
        check(!nav.canOpen(session(Role.SYSTEM_ADMIN), null), "Null module denied");
        check(!service.can(null, Permission.PATIENT_READ), "Null session denied");
        check(!service.can(session(Role.RECEPTIONIST), null), "Null permission denied");
        denied(() -> service.require(null, Permission.PATIENT_READ));

        check(nav.visibleModules(session(Role.RECEPTIONIST)).equals(
                List.of(Module.HOME, Module.PATIENTS)), "Receptionist only home/patients");
        check(nav.visibleModules(session(Role.DOCTOR)).equals(
                List.of(Module.HOME, Module.PATIENTS, Module.MEDICAL_RECORDS)), "Doctor safe list");
        check(nav.visibleModules(session(Role.NURSE)).equals(List.of(Module.HOME)), "Nurse pending policy");
        check(nav.visibleModules(session(Role.CLINIC_MANAGER)).equals(List.of(Module.HOME)), "Manager pending policy");
        check(nav.visibleModules(session(Role.INVENTORY_MANAGER)).equals(List.of(Module.HOME)), "Inventory pending policy");
        check(nav.visibleModules(session(Role.SYSTEM_ADMIN)).size() == Module.values().length,
                "Admin navigation covers all modules");

        System.out.println("PASS: R4-A1 ROLE, SESSION AND NAVIGATION SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO GUI, NO JDBC, NO SQL WRITES, NO REAL ACCOUNTS");
    }
}
