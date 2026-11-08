package com.quanlynhakhoa.nghiepvu.phieukham.service;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.CanLamSangDao;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.PhieuKhamDao;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.DuLieuCanLamSang;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class CanLamSangService {
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private static final int MAX_YEU_CAU = 300;
    private static final int MAX_KET_QUA = 1000;
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final PhieuKhamDao phieuKhamDao;
    private final CanLamSangDao dao;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());

    public CanLamSangService(DesktopSession session) {
        this(session, TeamDevConnection::open, new PhieuKhamDao(), new CanLamSangDao());
    }

    public CanLamSangService(DesktopSession session, ConnectionProvider connections,
                             PhieuKhamDao phieuKhamDao, CanLamSangDao dao) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.phieuKhamDao = Objects.requireNonNull(phieuKhamDao, "phieuKhamDao");
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public DuLieuCanLamSang taiDuLieu(String maPhieuKham, String maBacSiDuocChon)
            throws SQLException {
        authorization.require(session, Permission.MEDICAL_RECORD_READ);
        String visitId = validateId(maPhieuKham, "Mã phiếu khám");
        try (Connection connection = connections.open()) {
            verifyAssignment(connection, visitId, maBacSiDuocChon, false);
            DuLieuCanLamSang data = new DuLieuCanLamSang(
                    dao.danhSachDichVu(connection),
                    dao.danhSachChiDinh(connection, visitId));
            authorization.require(session, Permission.MEDICAL_RECORD_READ);
            return data;
        }
    }

    public void taoChiDinh(String maPhieuKham, String maBacSiDuocChon,
                           String maDichVu, String yeuCau) throws SQLException {
        authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
        String visitId = validateId(maPhieuKham, "Mã phiếu khám");
        String serviceId = validateId(maDichVu, "Mã dịch vụ");
        String request = normalize(yeuCau);
        if (request.isEmpty()) throw new IllegalArgumentException("Nhập yêu cầu cận lâm sàng.");
        if (request.length() > MAX_YEU_CAU) {
            throw new IllegalArgumentException("Yêu cầu cận lâm sàng tối đa 300 ký tự.");
        }

        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                String doctorId = verifyAssignment(connection, visitId, maBacSiDuocChon, true);
                if (!dao.dichVuConApDung(connection, serviceId)) {
                    throw new IllegalArgumentException("Dịch vụ không tồn tại, đã ngừng áp dụng hoặc không phải dịch vụ cận lâm sàng.");
                }
                String orderId = generateId();
                dao.taoChiDinh(connection, orderId, visitId, serviceId,
                        request, doctorId, session.accountId());
                authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }

    public void luuKetQua(String maPhieuKham, String maBacSiDuocChon,
                          String maChiDinh, String ketQua) throws SQLException {
        authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
        String visitId = validateId(maPhieuKham, "Mã phiếu khám");
        String orderId = validateId(maChiDinh, "Mã chỉ định");
        String result = normalize(ketQua);
        if (result.isEmpty()) throw new IllegalArgumentException("Nhập kết quả cận lâm sàng.");
        if (result.length() > MAX_KET_QUA) {
            throw new IllegalArgumentException("Kết quả cận lâm sàng tối đa 1000 ký tự.");
        }

        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                String doctorId = verifyAssignment(connection, visitId, maBacSiDuocChon, true);
                dao.luuKetQua(connection, orderId, visitId, result,
                        doctorId, session.accountId());
                authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }

    private String verifyAssignment(Connection connection, String visitId,
                                    String maBacSiDuocChon, boolean mustBeOpen)
            throws SQLException {
        CanLamSangDao.PhieuKhamAssignment assignment = (mustBeOpen
                ? dao.khoaPhieuKham(connection, visitId)
                : dao.timPhieuKham(connection, visitId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám."));
        String assignedDoctor = safe(assignment.maNhanVien());
        if (assignedDoctor.isEmpty()) {
            throw new IllegalStateException("Phiếu khám chưa được gán bác sĩ chịu trách nhiệm.");
        }
        if (mustBeOpen && !"CHO_KHAM".equals(assignment.trangThai())
                && !"DANG_KHAM".equals(assignment.trangThai())) {
            throw new IllegalStateException("Chỉ được lập chỉ định hoặc nhập kết quả khi phiếu Chờ khám hoặc Đang khám.");
        }

        if (session.role() == Role.DOCTOR) {
            Optional<String> employeeId = phieuKhamDao.maNhanVienTheoTaiKhoan(
                    connection, session.accountId());
            String signedInEmployee = employeeId.filter(value -> !value.isBlank())
                    .orElseThrow(() -> new IllegalStateException(
                            "Tài khoản bác sĩ chưa được liên kết với hồ sơ nhân viên."));
            if (!signedInEmployee.equals(assignedDoctor)) {
                throw new IllegalStateException("Phiếu khám do bác sĩ khác phụ trách.");
            }
        } else if (session.role() == Role.SYSTEM_ADMIN) {
            String selectedDoctor = safe(maBacSiDuocChon);
            if (selectedDoctor.isEmpty()) {
                throw new IllegalArgumentException("Quản trị hệ thống phải xác nhận bác sĩ chịu trách nhiệm.");
            }
            if (!selectedDoctor.equals(assignedDoctor)) {
                throw new IllegalArgumentException("Không được đổi bác sĩ chịu trách nhiệm của phiếu khám.");
            }
        } else {
            throw new IllegalStateException("Tài khoản không được phép thao tác cận lâm sàng.");
        }
        return assignedDoctor;
    }

    private static String generateId() {
        StringBuilder value = new StringBuilder(10);
        for (int i = 0; i < 10; i++) value.append(BASE32[RANDOM.nextInt(BASE32.length)]);
        return value.toString();
    }

    private static String validateId(String value, String field) {
        String text = safe(value);
        if (text.isEmpty() || text.length() > 10) {
            throw new IllegalArgumentException(field + " phải có từ 1 đến 10 ký tự.");
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
                throw new IllegalArgumentException(field + " chỉ được dùng chữ in hoa không dấu và chữ số.");
            }
        }
        return text;
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
