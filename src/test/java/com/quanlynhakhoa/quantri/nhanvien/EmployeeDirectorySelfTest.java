package com.quanlynhakhoa.quantri.nhanvien;

import com.quanlynhakhoa.model.nhanvien.KetQuaDanhSachNhanVien;
import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.hethong.tinhnang.FeatureBootstrap;
import com.quanlynhakhoa.quantri.nhanvien.entity.EmployeeStatus;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.dieuhuong.Module;
import com.quanlynhakhoa.hethong.dieuhuong.NavigationPolicy;
import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic RBAC and validation checks. No JDBC connection is opened. */
public final class EmployeeDirectorySelfTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError("W1A check " + checks + ": " + message);
    }
    private static DesktopSession session(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }
    private static void reject(Class<? extends Throwable> expected, ThrowingAction action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName());
        } catch (Throwable ex) {
            if (!expected.isInstance(ex)) throw new AssertionError("Unexpected " + ex, ex);
            check(true, "Rejected " + expected.getSimpleName());
        }
    }
    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }

    public static void main(String[] args) {
        AtomicInteger opens = new AtomicInteger();
        EmployeeDirectoryService.ConnectionProvider unreachable = () -> {
            opens.incrementAndGet();
            throw new SQLException("Synthetic provider: never connect");
        };
        NavigationPolicy navigation = new NavigationPolicy();
        check(FeatureBootstrap.createRegistry().contains(Module.WORKFORCE), "Workforce panel registered");
        DesktopSession receptionist = session(VaiTroHeThong.RECEPTIONIST);
        DesktopSession doctor = session(VaiTroHeThong.DOCTOR);
        DesktopSession manager = session(VaiTroHeThong.CLINIC_MANAGER);
        DesktopSession admin = session(VaiTroHeThong.SYSTEM_ADMIN);
        check(!navigation.canOpen(receptionist, Module.WORKFORCE), "Receptionist cannot open Workforce");
        check(!navigation.canOpen(doctor, Module.WORKFORCE), "Doctor cannot open Workforce");
        check(navigation.canOpen(manager, Module.WORKFORCE), "Manager can open Workforce");
        check(navigation.canOpen(admin, Module.WORKFORCE), "System admin can open Workforce");
        reject(AccessDeniedException.class,
                () -> new EmployeeDirectoryService(receptionist, unreachable).search("", null));
        reject(AccessDeniedException.class,
                () -> new EmployeeDirectoryService(doctor, unreachable).search("", null));
        check(opens.get() == 0, "Denied roles never open JDBC connection");
        reject(IllegalArgumentException.class,
                () -> new EmployeeDirectoryService(manager, unreachable).search("a".repeat(81), null));
        check(opens.get() == 0, "Bad keyword rejected before JDBC connection");
        reject(SQLException.class,
                () -> new EmployeeDirectoryService(manager, unreachable).search("Huy", EmployeeStatus.DANG_LAM_VIEC));
        check(opens.get() == 1, "Manager reaches DB provider after validation");
        manager.logout();
        reject(AccessDeniedException.class,
                () -> new EmployeeDirectoryService(manager, unreachable).search("", null));
        check(opens.get() == 1, "Logged-out session cannot open connection");
        reject(SQLException.class,
                () -> new EmployeeDirectoryService(admin, unreachable).search("", null));
        check(opens.get() == 2, "Admin is permitted to reach DB provider");
        KetQuaDanhSachNhanVien empty = new KetQuaDanhSachNhanVien(List.of(), false);
        check(empty.rows().isEmpty() && !empty.truncated(), "Immutable empty result works");
        System.out.println("PASS: W1A employee directory (" + checks + " checks; no DB)");
    }
}
