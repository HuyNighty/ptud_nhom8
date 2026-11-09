package com.quanlynhakhoa.nghiepvu.dieutri;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.nghiepvu.dieutri.service.TreatmentPlanService;
import java.time.LocalDate;

/** Pure denial tests: no JDBC connection or GUI is opened. */
public final class TreatmentPlanSecuritySelfTest {
    private static int checks;
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static DesktopSession as(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTH-TP", role, false));
    }
    private static void denied(Checked operation, String label) throws Exception {
        boolean rejected = false;
        try { operation.run(); }
        catch (AccessDeniedException expected) { rejected = true; }
        if (!rejected) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        TreatmentPlanService admin = new TreatmentPlanService(as(VaiTroHeThong.SYSTEM_ADMIN));
        TreatmentPlanService receptionist = new TreatmentPlanService(as(VaiTroHeThong.RECEPTIONIST));
        TreatmentPlanService manager = new TreatmentPlanService(as(VaiTroHeThong.CLINIC_MANAGER));
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
