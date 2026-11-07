package com.quanlynhakhoa.nghiepvu.dieutri;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.quantri.vaitro.model.Role;
import com.quanlynhakhoa.nghiepvu.dieutri.service.TreatmentPlanService;
import java.time.LocalDate;

/** Pure denial tests: no JDBC connection or GUI is opened. */
public final class TreatmentPlanSecuritySelfTest {
    private static int checks;
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static DesktopSession as(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTH-TP", role, false));
    }
    private static void denied(Checked operation, String label) throws Exception {
        boolean rejected = false;
        try { operation.run(); }
        catch (AccessDeniedException expected) { rejected = true; }
        if (!rejected) throw new AssertionError(label);
        checks++;
    }
    private static void invalid(Checked operation,String label)throws Exception {
        try{operation.run();throw new AssertionError(label);}
        catch(IllegalArgumentException expected){checks++;}
    }
    public static void main(String[] args) throws Exception {
        TreatmentPlanService admin = new TreatmentPlanService(as(Role.SYSTEM_ADMIN));
        TreatmentPlanService receptionist = new TreatmentPlanService(as(Role.RECEPTIONIST));
        TreatmentPlanService manager = new TreatmentPlanService(as(Role.CLINIC_MANAGER));
        invalid(() -> admin.taoKeHoach("", "HSDEMO01", "PKDEMO01", LocalDate.now(), "Demo"), "Admin can create but must supply a valid plan ID");
        invalid(() -> admin.themBuoc("", "DVDEMO01", "16", LocalDate.now(), ""), "Admin can add steps but IDs remain validated");
        invalid(() -> admin.hoanThanhBuoc(0, LocalDate.now(), "Demo"), "Admin completion validates detail ID");
        invalid(() -> admin.huyBuoc(0, "Demo"), "Admin cancellation validates detail ID");
        invalid(() -> admin.hoanThanhKeHoach(""), "Admin completion validates plan ID");
        invalid(() -> admin.huyKeHoach("", "Demo"), "Admin cancellation validates plan ID");
        denied(() -> receptionist.themBuoc("KHDEMO01", "DVDEMO01", "16", LocalDate.now(), ""), "Receptionist cannot mutate treatment plans");
        denied(() -> manager.huyKeHoach("KHDEMO01", "Demo"), "Manager cannot mutate treatment plans");
        com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy permissions=new com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy();
        for(var permission:com.quanlynhakhoa.hethong.phanquyen.Permission.values()){
            if(!permissions.isAllowed(Role.SYSTEM_ADMIN,permission))throw new AssertionError("Admin permission missing: "+permission);checks++;
        }
        DesktopSession expired=as(Role.SYSTEM_ADMIN);expired.logout();
        denied(() -> new TreatmentPlanService(expired).huyKeHoach("KHDEMO01", "Demo"), "Logged out admin cannot write");
        denied(() -> receptionist.danhSach(), "Receptionist cannot read treatment plans");
        denied(() -> manager.chiTiet("KHDEMO01"), "Manager cannot read treatment plans");
        System.out.println("PASS: ADMIN FULL PERMISSIONS AND TREATMENT SECURITY (" + checks + " checks, no database)");
    }
}