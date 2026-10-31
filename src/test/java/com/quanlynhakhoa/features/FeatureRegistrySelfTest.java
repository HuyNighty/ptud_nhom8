package com.quanlynhakhoa.features;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.navigation.NavigationPolicy;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;

public final class FeatureRegistrySelfTest {
    private static int checks;

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError("FeatureRegistry check " + checks + ": " + label);
    }

    private static void reject(Class<? extends Throwable> expected, Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw failure;
            rejected = true;
        }
        check(rejected, "expected " + expected.getSimpleName());
    }

    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }

    public static void main(String[] args) {
        FeatureRegistry empty = FeatureBootstrap.createRegistry();
        check(empty.contains(Module.AUDIT), "audit registered");
        check(empty.contains(Module.WORKFORCE), "workforce registered");
        check(empty.contains(Module.ROLES), "roles registered");
        check(empty.contains(Module.EXAMINATIONS), "examinations registered");
        check(empty.contains(Module.RECEPTION), "reception registered");
        check(empty.contains(Module.INVENTORY), "inventory registered");
        check(empty.contains(Module.SERVICE_CATALOG), "service catalogue integrated in main shell");
        check(empty.contains(Module.BILLING), "billing integrated in main shell");
        check(empty.contains(Module.ACCOUNTS), "existing accounts registration preserved");
        check(empty.contains(Module.SERVICES), "service information page registered");
        check(empty.contains(Module.DEBTS), "separate debt page registered");
        check(empty.contains(Module.PAYMENTS), "payment page registered");
        check(empty.contains(Module.INVOICE_CANCELLATIONS), "cancellation review page registered");
        check(empty.contains(Module.PATIENTS), "latest main patient registration preserved");
        check(empty.contains(Module.APPOINTMENTS), "appointments panel registered");
        check(empty.contains(Module.MEDICAL_RECORDS), "medical records panel registered");
        check(!empty.contains(Module.HOME), "home remains owned by Core");
        check(!empty.contains(null), "null has no registration");

        AtomicInteger created = new AtomicInteger();
        FeatureRegistry registry = FeatureRegistry.builder()
                .register(Module.PATIENTS, principal -> {
                    created.incrementAndGet();
                    return new JPanel();
                }).build();
        check(registry.contains(Module.PATIENTS), "registered panel exists");
        check(!registry.contains(Module.BILLING), "missing billing panel is not fabricated");
        check(created.get() == 0, "factory not called during registration");

        NavigationPolicy navigation = new NavigationPolicy();
        check(navigation.canOpen(session(Role.SYSTEM_ADMIN), Module.AUDIT),
                "admin can open audit");
        check(navigation.canOpen(session(Role.CLINIC_MANAGER), Module.AUDIT),
                "manager can open audit");
        check(!navigation.canOpen(session(Role.RECEPTIONIST), Module.AUDIT),
                "receptionist cannot open audit");
        check(!navigation.canOpen(session(Role.DOCTOR), Module.AUDIT),
                "doctor cannot open audit");

        check(navigation.canOpen(session(Role.RECEPTIONIST), Module.RECEPTION),
                "receptionist can open reception");
        check(navigation.canOpen(session(Role.SYSTEM_ADMIN), Module.RECEPTION),
                "admin can open reception");
        check(!navigation.canOpen(session(Role.DOCTOR), Module.RECEPTION),
                "doctor cannot open reception");
        check(!navigation.canOpen(session(Role.CLINIC_MANAGER), Module.RECEPTION),
                "manager cannot open reception");
        DesktopSession receptionist = session(Role.RECEPTIONIST);
        check(navigation.canOpen(receptionist, Module.APPOINTMENTS), "receptionist can open appointments");
        check(!navigation.canOpen(receptionist, Module.MEDICAL_RECORDS), "receptionist cannot open medical records");
        DesktopSession doctor = session(Role.DOCTOR);
        check(navigation.canOpen(doctor, Module.MEDICAL_RECORDS), "doctor can open medical records");
        check(!navigation.canOpen(doctor, Module.APPOINTMENTS), "doctor cannot open appointments");
        check(registry.create(receptionist, Module.PATIENTS) != null, "permitted panel created");
        check(created.get() == 1, "factory called only when requested");
        reject(AccessDeniedException.class, () -> registry.create(receptionist, Module.INVENTORY));
        check(created.get() == 1, "forbidden navigation cannot run factory");
        reject(IllegalArgumentException.class, () -> registry.create(receptionist, Module.HOME));
        receptionist.logout();
        reject(AccessDeniedException.class, () -> registry.create(receptionist, Module.PATIENTS));
        check(created.get() == 1, "logged-out session cannot run factory");

        DesktopSession admin = session(Role.SYSTEM_ADMIN);
        check(navigation.canOpen(admin, Module.APPOINTMENTS), "admin can open appointments");
        check(navigation.canOpen(admin, Module.MEDICAL_RECORDS), "admin can open medical records");
        reject(IllegalArgumentException.class, () -> registry.create(admin, Module.BILLING));
        reject(AccessDeniedException.class, () -> registry.create(null, Module.PATIENTS));
        reject(AccessDeniedException.class, () -> registry.create(admin, null));
        reject(IllegalArgumentException.class, () -> FeatureRegistry.builder()
                .register(Module.HOME, principal -> new JPanel()));
        reject(NullPointerException.class, () -> FeatureRegistry.builder()
                .register(null, principal -> new JPanel()));
        reject(NullPointerException.class, () -> FeatureRegistry.builder()
                .register(Module.PATIENTS, null));
        reject(IllegalArgumentException.class, () -> FeatureRegistry.builder()
                .register(Module.PATIENTS, principal -> new JPanel())
                .register(Module.PATIENTS, principal -> new JPanel()));
        FeatureRegistry bad = FeatureRegistry.builder().register(Module.INVENTORY, principal -> null).build();
        reject(NullPointerException.class, () -> bad.create(admin, Module.INVENTORY));

        System.out.println("PASS: FEATURE REGISTRY (" + checks + " checks; no DB, no JFrame)");
    }
}
