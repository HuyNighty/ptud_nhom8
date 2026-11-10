package com.quanlynhakhoa.service.dieutri;

import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.service.dieutri.KeHoachDieuTriService;
import java.time.LocalDate;

/** Pure denial tests: no JDBC connection or GUI is opened. */
public final class TreatmentPlanSecuritySelfTest {
    private static int checks;
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static DesktopSession as(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "SYNTH-TP", role, false));
    }
    private static void denied(Checked operation, String label) throws Exception {
        boolean rejected = false;
        try { operation.run(); }
        catch (AccessDeniedException expected) { rejected = true; }
        if (!rejected) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        KeHoachDieuTriService admin = new KeHoachDieuTriService(as(VaiTroHeThong.SYSTEM_ADMIN));
        KeHoachDieuTriService receptionist = new KeHoachDieuTriService(as(VaiTroHeThong.RECEPTIONIST));
        KeHoachDieuTriService manager = new KeHoachDieuTriService(as(VaiTroHeThong.CLINIC_MANAGER));
        denied(() -> admin.taoKeHoach("KHDEMO01", "HSDEMO01", "PKDEMO01", LocalDate.now(), "Demo"), "Admin may not create plans without doctor identity");
        denied(() -> admin.themBuoc("KHDEMO01", "DVDEMO01", "16", LocalDate.now(), ""), "Admin may not add steps");
        denied(() -> admin.hoanThanhBuoc(1, LocalDate.now(), "Demo"), "Admin may not complete steps");
        denied(() -> admin.huyBuoc(1, "Demo"), "Admin may not cancel steps");
        denied(() -> admin.hoanThanhKeHoach("KHDEMO01"), "Admin may not complete plans");
        denied(() -> admin.huyKeHoach("KHDEMO01", "Demo"), "Admin may not cancel plans");
        denied(() -> receptionist.danhSach(), "Receptionist cannot read treatment plans");
        denied(() -> manager.chiTiet("KHDEMO01"), "Manager cannot read treatment plans");
        System.out.println("PASS: TREATMENT SECURITY DENIAL (" + checks + " checks, no database)");
    }
}
