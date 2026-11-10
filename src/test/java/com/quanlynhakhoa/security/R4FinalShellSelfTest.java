package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.security.NavigationPolicy;
import com.quanlynhakhoa.view.hethong.MainFrame;
import com.quanlynhakhoa.model.dieuhuong.DieuHuongChinh;
import com.quanlynhakhoa.config.tinhnang.FeatureBootstrap;
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
    private static DesktopSession user(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(
                new KetQuaDangNhap(true, "SYNTH-R4A4", role, false));
    }
    private static boolean granted(VaiTroHeThong role, Quyen p) {
        return switch (role) {
            case SYSTEM_ADMIN -> true;
            case RECEPTIONIST -> Set.of(
                    Quyen.PATIENT_READ, Quyen.PATIENT_CREATE, Quyen.PATIENT_UPDATE,
                    Quyen.PATIENT_CHECK_IN, Quyen.APPOINTMENT_READ, Quyen.APPOINTMENT_MANAGE,
                    Quyen.BILLING_READ, Quyen.BILLING_MANAGE, Quyen.INVOICE_CANCEL_REQUEST,
                    Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE).contains(p);
            case DOCTOR -> Set.of(
                    Quyen.PATIENT_READ, Quyen.MEDICAL_RECORD_READ, Quyen.MEDICAL_RECORD_UPDATE,
                    Quyen.REEXAM_SCHEDULE, Quyen.SUPPLY_REQUEST, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE).contains(p);
            case NURSE -> Set.of(
                    Quyen.SUPPLY_REQUEST, Quyen.EMERGENCY_KIT_CHECK, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE).contains(p);
            case CLINIC_MANAGER -> Set.of(
                    Quyen.REVENUE_REPORT_READ, Quyen.BILLING_READ, Quyen.WORKFORCE_READ, Quyen.WORKFORCE_MANAGE,
                    Quyen.ACCOUNT_MANAGE, Quyen.ROLE_MANAGE, Quyen.AUDIT_READ,
                    Quyen.SERVICE_CATALOG_MANAGE, Quyen.INVOICE_CANCEL_APPROVE,
                    Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE).contains(p);
            case INVENTORY_MANAGER -> Set.of(
                    Quyen.INVENTORY_READ, Quyen.INVENTORY_MANAGE, Quyen.SCHEDULE_SELF_READ, Quyen.SHIFT_HANDOVER_MANAGE).contains(p);
        };
    }
    public static void main(String[] args) {
        check(VaiTroHeThong.values().length == 6, "unreviewed role added");
        check(Quyen.values().length == 26, "unreviewed permission added");
        check(Module.values().length == 23, "unreviewed navigation destination added");
        check(Module.TREATMENT_PLANS.openPermission() == Quyen.MEDICAL_RECORD_READ,
                "treatment plan navigation requires medical record read permission");
        check(Module.RECEPTION.openPermission() == Quyen.PATIENT_CHECK_IN,
                "reception requires patient check-in permission");
        check(FeatureBootstrap.createRegistry().contains(Module.RECEPTION),
                "reception feature remains registered");
        AuthorizationService auth = new AuthorizationService(new RolePermissionPolicy());
        AuthorizedActionGate actions = new AuthorizedActionGate(auth);
        NavigationPolicy nav = new NavigationPolicy(auth);
        check(!auth.can(null, Quyen.PATIENT_READ), "anonymous denied");
        check(!auth.can(user(VaiTroHeThong.SYSTEM_ADMIN), null), "missing permission denied");
        denied(() -> actions.run(null, Quyen.PATIENT_READ, () -> {}), "null principal blocked");
        denied(() -> actions.query(null, Quyen.PATIENT_READ, () -> "never"), "null query blocked");
        denied(() -> actions.run(user(VaiTroHeThong.SYSTEM_ADMIN), null, () -> {}), "null privilege blocked");
        denied(() -> actions.query(user(VaiTroHeThong.SYSTEM_ADMIN), null, () -> "never"), "null query privilege blocked");

        check(!nav.canOpen(null, Module.RECEPTION), "anonymous reception denied");
        denied(() -> nav.requireOpen(null, Module.RECEPTION), "anonymous reception forced route denied");
        for (VaiTroHeThong role : VaiTroHeThong.values()) {
            DesktopSession session = user(role);
            DieuHuongChinh model = new DieuHuongChinh(session);
            // Explicit business expectation, independent of Module.openPermission().
            boolean receptionAllowed = role == VaiTroHeThong.RECEPTIONIST || role == VaiTroHeThong.SYSTEM_ADMIN;
            check(auth.can(session, Quyen.PATIENT_CHECK_IN) == receptionAllowed,
                    "check-in role matrix: " + role);
            check(nav.canOpen(session, Module.RECEPTION) == receptionAllowed,
                    "reception route role matrix: " + role);
            check(model.availableModules().contains(Module.RECEPTION) == receptionAllowed,
                    "reception visibility role matrix: " + role);
            check(session.isActive(), "session starts active: " + role);
            check(nav.canOpen(session, Module.HOME), "home is allowed only with session");
            for (Quyen p : Quyen.values()) {
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
            check(!auth.can(session, Quyen.PATIENT_CHECK_IN), "logout check-in denied: " + role);
            check(!nav.canOpen(session, Module.RECEPTION), "logout reception denied: " + role);
            check(nav.visibleModules(session).isEmpty(), "logout hides all navigation");
            for (Quyen p : Quyen.values()) {
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
        DesktopSession old = user(VaiTroHeThong.SYSTEM_ADMIN);
        old.logout();
        DesktopSession fresh = user(VaiTroHeThong.SYSTEM_ADMIN);
        check(old != fresh, "new authentication creates new object");
        denied(() -> actions.run(old, Quyen.ACCOUNT_MANAGE, () -> {}), "old session remains denied");
        check(auth.can(fresh, Quyen.ACCOUNT_MANAGE), "new session can be authorized");
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
