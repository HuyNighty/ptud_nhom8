package com.quanlynhakhoa.hethong.giaodien;

import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.dieuhuong.Module;
import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
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
    private static DesktopSession session(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "SYNTHETIC", role, false));
    }
    public static void main(String[] args) {
        MainNavigationModel admin = new MainNavigationModel(session(VaiTroHeThong.SYSTEM_ADMIN));
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

        MainNavigationModel reception = new MainNavigationModel(session(VaiTroHeThong.RECEPTIONIST));
        check(reception.availableModules().equals(List.of(
                Module.HOME, Module.PATIENTS, Module.APPOINTMENTS, Module.BILLING,
                Module.MY_SCHEDULE, Module.DEBTS, Module.PAYMENTS, Module.RECEPTION)));
        reception.select(Module.PATIENTS);
        check(reception.selected() == Module.PATIENTS);
        reception.select(Module.BILLING);
        check(reception.selected() == Module.BILLING);
        reception.select(Module.APPOINTMENTS);
        check(reception.selected() == Module.APPOINTMENTS);
        reception.select(Module.RECEPTION);
        check(reception.selected() == Module.RECEPTION);
        denied(() -> reception.select(Module.INVENTORY));
        reception.logout();
        denied(() -> reception.select(Module.PATIENTS));
        denied(() -> reception.select(Module.RECEPTION));

        MainNavigationModel doctor = new MainNavigationModel(session(VaiTroHeThong.DOCTOR));
        check(doctor.availableModules().equals(List.of(
                Module.HOME, Module.PATIENTS, Module.MEDICAL_RECORDS,
                Module.EXAMINATIONS, Module.TREATMENT_PLANS, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE)));
        doctor.select(Module.TREATMENT_PLANS);
        check(doctor.selected() == Module.TREATMENT_PLANS);
        doctor.select(Module.MEDICAL_RECORDS);
        check(doctor.selected() == Module.MEDICAL_RECORDS);
        denied(() -> doctor.select(Module.WORKFORCE));

        MainNavigationModel nurse = new MainNavigationModel(session(VaiTroHeThong.NURSE));
        check(nurse.availableModules().equals(List.of(
                Module.HOME, Module.SUPPLY_REQUESTS, Module.MY_SCHEDULE, Module.EMERGENCY_KIT)));
        for (Module module : Module.values()) {
            if (!nurse.availableModules().contains(module)) denied(() -> nurse.select(module));
        }
        MainNavigationModel manager = new MainNavigationModel(session(VaiTroHeThong.CLINIC_MANAGER));
        check(manager.availableModules().equals(List.of(
                Module.HOME, Module.BILLING, Module.WORKFORCE,
                Module.ACCOUNTS, Module.ROLES, Module.AUDIT,
                Module.SERVICE_CATALOG, Module.MY_SCHEDULE, Module.SERVICES, Module.DEBTS, Module.INVOICE_CANCELLATIONS, Module.SHIFT_ASSIGNMENTS, Module.REVENUE_REPORTS)));
        manager.select(Module.WORKFORCE);
        denied(() -> manager.select(Module.INVENTORY));
        MainNavigationModel inventory = new MainNavigationModel(session(VaiTroHeThong.INVENTORY_MANAGER));
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
