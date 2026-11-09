package com.quanlynhakhoa.service.dieutri;

import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** H2 fail-closed boundary: no DAO/connection until identity, ownership and writes are reviewed. */
public final class KeHoachDieuTriService {
    private final DesktopSession session;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());

    public KeHoachDieuTriService(DesktopSession session) {
        if (session == null) throw new IllegalArgumentException("Cần phiên đăng nhập.");
        this.session = session;
    }

    public List<Object[]> danhSach() throws SQLException {
        requireLocalPermission(Quyen.MEDICAL_RECORD_READ);
        throw unavailable();
    }

    public List<Object[]> chiTiet(String planId) throws SQLException {
        requireLocalPermission(Quyen.MEDICAL_RECORD_READ);
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
        requireLocalPermission(Quyen.MEDICAL_RECORD_UPDATE);
        if (session.role() != VaiTroHeThong.DOCTOR) throw new AccessDeniedException();
        // DOCTOR is a prerequisite, NOT proof of linked active doctor identity.
        // All operations still fail closed: never wire DAO writes behind this check alone.
    }

    /** Disabled operations reject locally before the PR118 verifier can open JDBC.
     * Shared verifier/session binding remains intact for other reviewed services.
     */
    private void requireLocalPermission(Quyen permission) {
        if (!authorization.can(session, permission)) throw new AccessDeniedException();
    }

    private static KeHoachDieuTriKhongKhaDungException unavailable() {
        return new KeHoachDieuTriKhongKhaDungException();
    }
}
