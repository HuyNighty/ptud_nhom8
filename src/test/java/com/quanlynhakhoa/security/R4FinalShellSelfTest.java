package com.quanlynhakhoa.security;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.navigation.NavigationPolicy;
import com.quanlynhakhoa.view.MainFrame;
import com.quanlynhakhoa.view.MainNavigationModel;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** R4-A4 pure-Java authorization acceptance. No real users, GUI, JDBC, or SQL. */
public final class R4FinalShellSelfTest {
    private static int checks;
    private static void check(boolean success, String why) {
        checks++;
        if (!success) throw new AssertionError("R4-A4 check " + checks + ": " + why);
    }
    private static void denied(Runnable task, String why) {
        boolean caught = false;
        try { task.run(); } catch (AccessDeniedException expected) { caught = true; }
        check(caught, why);
    }
    private static DesktopSession user(Role role) {
        return DesktopSession.fromSuccessfulLogin(
                new AuthDecision(true, "SYNTH-R4A4", role, false));
    }
    private static boolean granted(Role role, Permission p) {
        return switch (role) {
            case SYSTEM_ADMIN -> true;
            case RECEPTIONIST -> Set.of(
                    Permission.PATIENT_READ, Permission.PATIENT_CREATE, Permission.PATIENT_UPDATE,
                    Permission.PATIENT_CHECK_IN, Permission.APPOINTMENT_READ, Permission.APPOINTMENT_MANAGE,
                    Permission.BILLING_READ, Permission.BILLING_MANAGE, Permission.INVOICE_CANCEL_REQUEST,
                    Permission.SCHEDULE_SELF_READ).contains(p);
            case DOCTOR -> Set.of(
                    Permission.PATIENT_READ, Permission.MEDICAL_RECORD_READ, Permission.MEDICAL_RECORD_UPDATE,
                    Permission.REEXAM_SCHEDULE, Permission.SUPPLY_REQUEST, Permission.SCHEDULE_SELF_READ).contains(p);
            case NURSE -> Set.of(
                    Permission.SUPPLY_REQUEST, Permission.EMERGENCY_KIT_CHECK, Permission.SCHEDULE_SELF_READ).contains(p);
            case CLINIC_MANAGER -> Set.of(
                    Permission.BILLING_READ, Permission.WORKFORCE_READ, Permission.WORKFORCE_MANAGE,
                    Permission.ACCOUNT_MANAGE, Permission.ROLE_MANAGE, Permission.AUDIT_READ,
                    Permission.SERVICE_CATALOG_MANAGE, Permission.INVOICE_CANCEL_APPROVE,
                    Permission.SCHEDULE_SELF_READ).contains(p);
            case INVENTORY_MANAGER -> Set.of(
                    Permission.INVENTORY_READ, Permission.INVENTORY_MANAGE, Permission.SCHEDULE_SELF_READ).contains(p);
        };
    }
    public static void main(String[] args) {
        check(Role.values().length == 6, "unreviewed role added");
        check(Permission.values().length == 24, "unreviewed permission added");
        check(Module.values().length == 15, "unreviewed navigation destination added");
        AuthorizationService auth = new AuthorizationService(new RolePermissionPolicy());
        AuthorizedActionGate actions = new AuthorizedActionGate(auth);
        NavigationPolicy nav = new NavigationPolicy(auth);
        check(!auth.can(null, Permission.PATIENT_READ), "anonymous denied");
        check(!auth.can(user(Role.SYSTEM_ADMIN), null), "missing permission denied");
        denied(() -> actions.run(null, Permission.PATIENT_READ, () -> {}), "null principal blocked");
        denied(() -> actions.query(null, Permission.PATIENT_READ, () -> "never"), "null query blocked");
        denied(() -> actions.run(user(Role.SYSTEM_ADMIN), null, () -> {}), "null privilege blocked");
        denied(() -> actions.query(user(Role.SYSTEM_ADMIN), null, () -> "never"), "null query privilege blocked");

        for (Role role : Role.values()) {
            DesktopSession session = user(role);
            MainNavigationModel model = new MainNavigationModel(session);
            check(session.isActive(), "session starts active: " + role);
            check(nav.canOpen(session, Module.HOME), "home is allowed only with session");
            for (Permission p : Permission.values()) {
                final boolean expected = granted(role, p);
                check(auth.can(session, p) == expected, "service mapping: " + role + "/" + p);
                AtomicInteger count = new AtomicInteger();
                if (expected) {
                    actions.run(session, p, count::incrementAndGet);
                    check(count.get() == 1, "authorized action ran once");
                    String value = actions.query(session, p, () -> {
                        count.incrementAndGet(); return "OK";
                    });
                    check("OK".equals(value), "authorized query returns value");
                    check(count.get() == 2, "authorized query ran once");
                } else {
                    denied(() -> actions.run(session, p, count::incrementAndGet),
                            "disallowed action denied: " + role + "/" + p);
                    check(count.get() == 0, "denied runnable must not execute");
                    denied(() -> actions.query(session, p, () -> {
                        count.incrementAndGet(); return "WRONG";
                    }), "disallowed query denied");
                    check(count.get() == 0, "denied query must not execute");
                }
            }
            List<Module> allowed = nav.visibleModules(session);
            check(model.availableModules().equals(allowed), "sidebar matches authorization model");
            for (Module module : Module.values()) {
                boolean expected = module == Module.HOME || granted(role, module.openPermission());
                check(allowed.contains(module) == expected, "navigation visibility: " + role + "/" + module);
                if (expected) {
                    model.select(module);
                    check(model.selected() == module, "allowed navigation selected");
                } else {
                    Module before = model.selected();
                    denied(() -> model.select(module), "hidden navigation forced by code denied");
                    check(model.selected() == before, "denied navigation left state unchanged");
                }
            }
            session.logout();
            check(!session.isActive(), "logout deactivates session");
            check(nav.visibleModules(session).isEmpty(), "logout hides all navigation");
            for (Permission p : Permission.values()) {
                AtomicInteger count = new AtomicInteger();
                denied(() -> actions.run(session, p, count::incrementAndGet), "logout action denied");
                check(count.get() == 0, "logout action not executed");
                denied(() -> actions.query(session, p, () -> {
                    count.incrementAndGet(); return "BAD";
                }), "logout query denied");
                check(count.get() == 0, "logout query not executed");
            }
            for (Module module : Module.values()) {
                denied(() -> model.select(module), "logout navigation denied");
            }
            session.logout();
            check(!session.isActive(), "repeated logout cannot revive");
        }
        DesktopSession old = user(Role.SYSTEM_ADMIN);
        old.logout();
        DesktopSession fresh = user(Role.SYSTEM_ADMIN);
        check(old != fresh, "new authentication creates new object");
        denied(() -> actions.run(old, Permission.ACCOUNT_MANAGE, () -> {}), "old session remains denied");
        check(auth.can(fresh, Permission.ACCOUNT_MANAGE), "new session can be authorized");
        fresh.logout();

        // Reflection validates the public shell entry point without opening a JFrame.
        boolean authorizedConstructor = false;
        boolean unsafeNoArgs = false;
        for (Constructor<?> ctor : MainFrame.class.getConstructors()) {
            Class<?>[] argsTypes = ctor.getParameterTypes();
            if (argsTypes.length == 0) unsafeNoArgs = true;
            if (argsTypes.length == 2 && argsTypes[0] == DesktopSession.class
                    && argsTypes[1] == Runnable.class) authorizedConstructor = true;
        }
        check(authorizedConstructor, "shell requires session and logout action");
        check(!unsafeNoArgs, "no public unauthenticated shell constructor");
        System.out.println("PASS: R4-A4 AUTHORIZATION AND SHELL SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO SWING WINDOW, NO JDBC, NO SQL WRITES, NO REAL ACCOUNTS");
    }
}
