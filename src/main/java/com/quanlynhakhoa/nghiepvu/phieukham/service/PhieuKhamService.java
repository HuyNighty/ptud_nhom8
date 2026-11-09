package com.quanlynhakhoa.nghiepvu.phieukham.service;

import com.quanlynhakhoa.model.phieukham.ThongTinKhoaKetLuanPhieuKham;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.PhieuKhamDao;
import com.quanlynhakhoa.model.phieukham.BacSiChonKham;
import com.quanlynhakhoa.model.phieukham.ThongTinPhieuKham;
import com.quanlynhakhoa.model.phieukham.DongDanhSachPhieuKham;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class PhieuKhamService {
    private static final int MAX_TRIEU_CHUNG = 500;
    private static final int MAX_CHAN_DOAN = 1000;
    private static final int MAX_KET_LUAN = 1000;

    private final DesktopSession session;
    private final AuthorizationService authorization =
            new AuthorizationService(new com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy());
    private final PhieuKhamDao dao;

    public PhieuKhamService(DesktopSession session) {
        this(session, new PhieuKhamDao());
    }

    public PhieuKhamService(DesktopSession session, PhieuKhamDao dao) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        this.dao = java.util.Objects.requireNonNull(dao, "dao");
    }

    public List<DongDanhSachPhieuKham> timKiem(LocalDate ngayKham, String maBacSi,
                                            String trangThai, String tuKhoa) throws SQLException {
        authorization.require(session, Quyen.MEDICAL_RECORD_READ);
        try (Connection connection = TeamDevConnection.open()) {
            String doctorFilter = safe(maBacSi);
            if (session.role() == VaiTroHeThong.DOCTOR) {
                String employeeId = layMaNhanVienDangNhap(connection);
                doctorFilter = employeeId;
            }
            return dao.timKiem(connection, ngayKham, doctorFilter, trangThai, tuKhoa);
        }
    }

    public ThongTinPhieuKham chiTiet(String maPhieuKham) throws SQLException {
        authorization.require(session, Quyen.MEDICAL_RECORD_READ);
        try (Connection connection = TeamDevConnection.open()) {
            ThongTinPhieuKham phieu = dao.timTheoMa(connection, maPhieuKham)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám."));
            kiemTraPhuTrach(connection, phieu);
            return phieu;
        }
    }

    public List<BacSiChonKham> danhSachBacSi() throws SQLException {
        authorization.require(session, Quyen.MEDICAL_RECORD_READ);
        try (Connection connection = TeamDevConnection.open()) {
            List<BacSiChonKham> doctors = dao.danhSachBacSiDangLamViec(connection);
            if (session.role() != VaiTroHeThong.DOCTOR) return doctors;

            String myEmployeeId = layMaNhanVienDangNhap(connection);
            return doctors.stream()
                    .filter(doctor -> doctor.maNhanVien().equals(myEmployeeId))
                    .toList();
        }
    }

    public void luuNhap(String maPhieuKham, String maBacSiDuocChon,
                        String trieuChung, String chanDoan) throws SQLException {
        authorization.require(session, Quyen.MEDICAL_RECORD_UPDATE);
        String symptom = normalize(trieuChung);
        String diagnosis = normalize(chanDoan);
        if (symptom.length() > MAX_TRIEU_CHUNG) {
            throw new IllegalArgumentException("Triệu chứng tối đa 500 ký tự.");
        }
        if (diagnosis.length() > MAX_CHAN_DOAN) {
            throw new IllegalArgumentException("Chẩn đoán tối đa 1000 ký tự.");
        }

        try (Connection connection = TeamDevConnection.open()) {
            connection.setAutoCommit(false);
            try {
                ThongTinPhieuKham phieu = dao.timTheoMa(connection, maPhieuKham)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám."));
                if (!"CHO_KHAM".equals(phieu.trangThai())
                        && !"DANG_KHAM".equals(phieu.trangThai())) {
                    throw new IllegalStateException("Chỉ có thể lưu khi phiếu đang Chờ khám hoặc Đang khám.");
                }
                kiemTraPhuTrach(connection, phieu);

                String doctorId = phieu.maNhanVien();
                if (session.role() == VaiTroHeThong.SYSTEM_ADMIN) {
                    String selected = safe(maBacSiDuocChon);
                    if (!selected.equals(doctorId)) {
                        throw new IllegalArgumentException("Bác sĩ chịu trách nhiệm phải trùng với bác sĩ đã nhận phiếu.");
                    }
                }
                dao.luuNhap(connection, maPhieuKham, doctorId,
                        symptom, diagnosis, session.accountId());
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }


    public void ketLuan(String maPhieuKham, String maBacSiDuocChon,
                        String chanDoan, String ketLuan) throws SQLException {

        authorization.require(session, Quyen.MEDICAL_RECORD_UPDATE);

        String diagnosis = normalize(chanDoan);
        String conclusion = normalize(ketLuan);

        if (diagnosis.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nhập chẩn đoán trước khi kết luận.");
        }

        if (diagnosis.length() > MAX_CHAN_DOAN) {
            throw new IllegalArgumentException(
                    "Chẩn đoán tối đa 1000 ký tự.");
        }

        if (conclusion.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nhập kết luận / hướng điều trị.");
        }

        if (conclusion.length() > MAX_KET_LUAN) {
            throw new IllegalArgumentException(
                    "Kết luận tối đa 1000 ký tự.");
        }

        try (Connection connection = TeamDevConnection.open()) {
            connection.setAutoCommit(false);

            try {
                ThongTinKhoaKetLuanPhieuKham locked =
                        dao.khoaPhieuDeKetLuan(connection, maPhieuKham)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Không tìm thấy phiếu khám."));

                if (!"CHO_KHAM".equals(locked.trangThai())
                        && !"DANG_KHAM".equals(locked.trangThai())) {
                    throw new IllegalStateException(
                            "Phiếu khám không còn ở trạng thái có thể kết luận.");
                }

                ThongTinPhieuKham visit = dao.timTheoMa(connection, maPhieuKham)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy phiếu khám."));

                kiemTraPhuTrach(connection, visit);

                String doctorId = visit.maNhanVien();

                if (session.role() == VaiTroHeThong.SYSTEM_ADMIN
                        && !safe(maBacSiDuocChon).equals(doctorId)) {
                    throw new IllegalArgumentException(
                            "Quản trị hệ thống phải xác nhận đúng bác sĩ chịu trách nhiệm của phiếu.");
                }

                int pendingOrders =
                        dao.demChiDinhChuaCoKetQua(connection, maPhieuKham);

                if (pendingOrders > 0) {
                    throw new IllegalStateException(
                            "Còn " + pendingOrders
                                    + " chỉ định cận lâm sàng chưa có kết quả. "
                                    + "Hãy hoàn tất trước khi kết luận.");
                }

                dao.ketLuan(
                        connection,
                        maPhieuKham,
                        doctorId,
                        diagnosis,
                        conclusion,
                        locked.maLichHen(),
                        session.accountId()
                );

                connection.commit();

            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }


    private void kiemTraPhuTrach(Connection connection, ThongTinPhieuKham phieu) throws SQLException {
        if (session.role() == VaiTroHeThong.DOCTOR) {
            String employeeId = layMaNhanVienDangNhap(connection);
            if (!employeeId.equals(phieu.maNhanVien())) {
                throw new IllegalStateException("Phiếu khám do bác sĩ khác phụ trách.");
            }
        }
    }

    private String layMaNhanVienDangNhap(Connection connection) throws SQLException {
        Optional<String> employeeId = dao.maNhanVienTheoTaiKhoan(connection, session.accountId());
        return employeeId.filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Tài khoản chưa được liên kết với hồ sơ nhân viên."));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            failure.addSuppressed(rollbackError);
        }
    }
}
