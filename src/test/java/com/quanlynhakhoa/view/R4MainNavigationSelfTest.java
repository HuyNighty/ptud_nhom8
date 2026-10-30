package com.quanlynhakhoa.view;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.util.List;

/** R4-A2: Pure Java navigation tests. No Swing JFrame or database is instantiated. */
public final class R4MainNavigationSelfTest {
    private static int checks;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("R4-A2 failed at check " + (checks + 1));
        checks++;
    }
    private static void denied(Runnable action) {
        boolean failedClosed = false;
        try { action.run(); } catch (AccessDeniedException expected) { failedClosed = true; }
        check(failedClosed);
    }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }
    public static void main(String[] args) {
        MainNavigationModel admin = new MainNavigationModel(session(Role.SYSTEM_ADMIN));
        check(admin.availableModules().size() == Module.values().length);
        check(admin.selected() == Module.HOME);
        for (Module item : Module.values()) {
            admin.select(item);
            check(admin.selected() == item);
            check(admin.availableModules().contains(item));
        }
        denied(() -> admin.select(null));
        admin.logout();
        check(!admin.isActive());
        check(admin.availableModules().isEmpty());
        denied(() -> admin.select(Module.HOME));
        denied(() -> admin.select(Module.PATIENTS));

        MainNavigationModel reception = new MainNavigationModel(session(Role.RECEPTIONIST));
        check(reception.availableModules().equals(List.of(
                Module.HOME, Module.PATIENTS, Module.APPOINTMENTS, Module.BILLING,
                Module.MY_SCHEDULE, Module.DEBTS, Module.PAYMENTS)));
        reception.select(Module.PATIENTS);
        check(reception.selected() == Module.PATIENTS);
        reception.select(Module.BILLING);
        check(reception.selected() == Module.BILLING);
        reception.select(Module.APPOINTMENTS);
        check(reception.selected() == Module.APPOINTMENTS);
        denied(() -> reception.select(Module.INVENTORY));
        reception.logout();
        denied(() -> reception.select(Module.PATIENTS));

        MainNavigationModel doctor = new MainNavigationModel(session(Role.DOCTOR));
        check(doctor.availableModules().equals(List.of(
                Module.HOME, Module.PATIENTS, Module.MEDICAL_RECORDS,
                Module.EXAMINATIONS, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE)));
        doctor.select(Module.MEDICAL_RECORDS);
        check(doctor.selected() == Module.MEDICAL_RECORDS);
        denied(() -> doctor.select(Module.WORKFORCE));

        MainNavigationModel nurse = new MainNavigationModel(session(Role.NURSE));
        check(nurse.availableModules().equals(List.of(
                Module.HOME, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE, Module.EMERGENCY_KIT)));
        for (Module module : Module.values()) {
            if (!nurse.availableModules().contains(module)) denied(() -> nurse.select(module));
        }
        MainNavigationModel manager = new MainNavigationModel(session(Role.CLINIC_MANAGER));
        check(manager.availableModules().equals(List.of(
                Module.HOME, Module.BILLING, Module.WORKFORCE,
                Module.ACCOUNTS, Module.ROLES, Module.AUDIT,
                Module.SERVICE_CATALOG, Module.MY_SCHEDULE, Module.SERVICES, Module.DEBTS, Module.INVOICE_CANCELLATIONS)));
        manager.select(Module.WORKFORCE);
        denied(() -> manager.select(Module.INVENTORY));
        MainNavigationModel inventory = new MainNavigationModel(session(Role.INVENTORY_MANAGER));
        check(inventory.availableModules().equals(List.of(Module.HOME, Module.INVENTORY, Module.MY_SCHEDULE)));
        inventory.select(Module.INVENTORY);
        denied(() -> inventory.select(Module.BILLING));
        boolean nullRejected = false;
        try { new MainNavigationModel(null); } catch (NullPointerException expected) { nullRejected = true; }
        check(nullRejected);
        System.out.println("PASS: R4-A2 NAVIGATION MODEL TESTS (" + checks + " checks)");
        System.out.println("NO SWING WINDOW, NO DATABASE, NO SQL WRITES");
    }
}
