package com.quanlynhakhoa.nghiepvu.dieutri.service;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import com.quanlynhakhoa.quantri.vaitro.model.Role;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** H2 fail-closed boundary: no DAO/connection until identity, ownership and writes are reviewed. */
public final class TreatmentPlanService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());

    public TreatmentPlanService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public List<Object[]> danhSach() throws SQLException {
        requireLocalPermission(Permission.MEDICAL_RECORD_READ);
        throw unavailable();
    }

    public List<Object[]> chiTiet(String planId) throws SQLException {
        requireLocalPermission(Permission.MEDICAL_RECORD_READ);
        throw unavailable();
    }

    public void taoKeHoach(String planId, String chartId, String visitId, LocalDate date, String goal) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    public void themBuoc(String planId, String serviceId, String tooth, LocalDate planned, String note) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    public void hoanThanhBuoc(int detailId, LocalDate actual, String result) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    public void huyBuoc(int detailId, String reason) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    public void hoanThanhKeHoach(String planId) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    public void huyKeHoach(String planId, String reason) throws SQLException {
        requireDoctorCandidate();
        throw unavailable();
    }

    private void requireDoctorCandidate() {
        requireLocalPermission(Permission.MEDICAL_RECORD_UPDATE);
        if (session.role() != Role.DOCTOR) throw new AccessDeniedException();
        // DOCTOR is a prerequisite, NOT proof of linked active doctor identity.
        // All operations still fail closed: never wire DAO writes behind this check alone.
    }

    /** Disabled operations reject locally before the PR118 verifier can open JDBC.
     * Shared verifier/session binding remains intact for other reviewed services.
     */
    private void requireLocalPermission(Permission permission) {
        if (!authorization.can(session, permission)) throw new AccessDeniedException();
    }

    private static TreatmentPlanUnavailableException unavailable() {
        return new TreatmentPlanUnavailableException();
    }
}
