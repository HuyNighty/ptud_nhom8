package com.quanlynhakhoa.nghiepvu.phieukham.dao;

import com.quanlynhakhoa.nghiepvu.phieukham.model.BacSi;
import com.quanlynhakhoa.nghiepvu.phieukham.model.PhieuKham;
import com.quanlynhakhoa.nghiepvu.phieukham.model.PhieuKhamListItem;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PhieuKhamDao {

    public record PhieuKhamKetLuanLock(
            String maBenhNhan,
            String maNhanVien,
            String maLichHen,
            String trangThai) {
    }

    public Optional<PhieuKhamKetLuanLock> khoaPhieuDeKetLuan(
            Connection connection, String maPhieuKham) throws SQLException {

        String sql = """
            SELECT maBenhNhan, maNhanVien, maLichHen, trangThai
            FROM dbo.PhieuKham WITH (UPDLOCK, HOLDLOCK)
            WHERE maPhieuKham = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maPhieuKham);
            statement.setQueryTimeout(10);

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }

                return Optional.of(new PhieuKhamKetLuanLock(
                        rows.getString("maBenhNhan"),
                        rows.getString("maNhanVien"),
                        rows.getString("maLichHen"),
                        rows.getString("trangThai")));
            }
        }
    }

    public int demChiDinhChuaCoKetQua(
            Connection connection, String maPhieuKham) throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM dbo.ChiDinhCanLamSang
            WHERE maPhieuKham = ?
              AND (trangThai <> 'DA_CO_KET_QUA'
                   OR ketQua IS NULL
                   OR LEN(TRIM(ketQua)) = 0)
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maPhieuKham);
            statement.setQueryTimeout(10);

            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static final String SQL_DANH_SACH = """
            SELECT TOP 200
                   pk.maPhieuKham,
                   pk.maBenhNhan,
                   bn.hoTen AS tenBenhNhan,
                   bn.soDienThoai,
                   pk.thoiGianTiepNhan,
                   pk.maNhanVien,
                   nv.hoTen AS tenBacSi,
                   pk.trangThai
            FROM dbo.PhieuKham AS pk
            INNER JOIN dbo.BenhNhan AS bn
                    ON bn.maBenhNhan = pk.maBenhNhan
            INNER JOIN dbo.NhanVien AS nv
                    ON nv.maNhanVien = pk.maNhanVien
            WHERE CONVERT(date, pk.thoiGianTiepNhan) = ?
              AND (? = '' OR pk.maNhanVien = ?)
              AND (? = '' OR pk.trangThai = ?)
              AND (? = '' OR pk.maPhieuKham LIKE ?
                         OR bn.hoTen LIKE ?
                         OR bn.soDienThoai LIKE ?)
            ORDER BY pk.thoiGianTiepNhan DESC, pk.maPhieuKham DESC
            """;

    private static final String SQL_CHI_TIET = """
            SELECT pk.maPhieuKham,
                   pk.maBenhNhan,
                   bn.hoTen AS tenBenhNhan,
                   bn.soDienThoai,
                   pk.maHoSo,
                   pk.maLichHen,
                   pk.thoiGianTiepNhan,
                   pk.maNhanVien,
                   nv.hoTen AS tenBacSi,
                   pk.trieuChung,
                   pk.chanDoan,
                   pk.ketLuan,
                   pk.trangThai,
                   hs.tienSuBenh,
                   hs.diUng
            FROM dbo.PhieuKham AS pk
            INNER JOIN dbo.BenhNhan AS bn
                    ON bn.maBenhNhan = pk.maBenhNhan
            INNER JOIN dbo.NhanVien AS nv
                    ON nv.maNhanVien = pk.maNhanVien
            LEFT JOIN dbo.HoSoBenhAn AS hs
                   ON hs.maHoSo = pk.maHoSo
            WHERE pk.maPhieuKham = ?
            """;

    public List<PhieuKhamListItem> timKiem(Connection connection,
                                            LocalDate ngayKham,
                                            String maBacSi,
                                            String trangThai,
                                            String tuKhoa) throws SQLException {
        if (connection == null) throw new IllegalArgumentException("connection required");
        if (ngayKham == null) throw new IllegalArgumentException("Ngày khám không được để trống.");

        String doctor = safe(maBacSi);
        String status = safe(trangThai);
        String keyword = safe(tuKhoa);
        String pattern = "%" + keyword + "%";
        List<PhieuKhamListItem> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(SQL_DANH_SACH)) {
            statement.setDate(1, Date.valueOf(ngayKham));
            statement.setString(2, doctor);
            statement.setString(3, doctor);
            statement.setString(4, status);
            statement.setString(5, status);
            statement.setString(6, keyword);
            statement.setString(7, pattern);
            statement.setNString(8, pattern);
            statement.setString(9, pattern);
            statement.setQueryTimeout(10);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new PhieuKhamListItem(
                            rs.getString("maPhieuKham"),
                            rs.getString("maBenhNhan"),
                            rs.getNString("tenBenhNhan"),
                            rs.getString("soDienThoai"),
                            toLocalDateTime(rs.getTimestamp("thoiGianTiepNhan")),
                            rs.getString("maNhanVien"),
                            rs.getNString("tenBacSi"),
                            rs.getString("trangThai")));
                }
            }
        }
        return result;
    }

    public Optional<PhieuKham> timTheoMa(Connection connection, String maPhieuKham)
            throws SQLException {
        if (connection == null) throw new IllegalArgumentException("connection required");
        if (maPhieuKham == null || maPhieuKham.isBlank()) return Optional.empty();
        try (PreparedStatement statement = connection.prepareStatement(SQL_CHI_TIET)) {
            statement.setString(1, maPhieuKham.trim());
            statement.setQueryTimeout(10);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PhieuKham(
                        rs.getString("maPhieuKham"),
                        rs.getString("maBenhNhan"),
                        rs.getNString("tenBenhNhan"),
                        rs.getString("soDienThoai"),
                        rs.getString("maHoSo"),
                        rs.getString("maLichHen"),
                        toLocalDateTime(rs.getTimestamp("thoiGianTiepNhan")),
                        rs.getString("maNhanVien"),
                        rs.getNString("tenBacSi"),
                        rs.getNString("trieuChung"),
                        rs.getNString("chanDoan"),
                        rs.getNString("ketLuan"),
                        rs.getString("trangThai"),
                        rs.getNString("tienSuBenh"),
                        rs.getNString("diUng")));
            }
        }
    }

    public Optional<String> maNhanVienTheoTaiKhoan(Connection connection, String maTaiKhoan)
            throws SQLException {
        String sql = "SELECT maNhanVien FROM dbo.TaiKhoan WHERE maTaiKhoan = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maTaiKhoan);
            statement.setQueryTimeout(10);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String employeeId = rs.getString("maNhanVien");
                return Optional.ofNullable(employeeId);
            }
        }
    }

    public List<BacSi> danhSachBacSiDangLamViec(Connection connection) throws SQLException {
        String sql = """
                SELECT maNhanVien, hoTen
                FROM dbo.NhanVien
                WHERE chucDanh = 'BS' AND trangThai = 'DANG_LAM_VIEC'
                ORDER BY hoTen, maNhanVien
                """;
        List<BacSi> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new BacSi(rs.getString("maNhanVien"), rs.getNString("hoTen")));
                }
            }
        }
        return result;
    }

    public void luuNhap(Connection connection, String maPhieuKham,
                        String maBacSi, String trieuChung,
                        String chanDoan, String maTaiKhoan) throws SQLException {
        String sql = """
                UPDATE dbo.PhieuKham
                SET trieuChung = ?, chanDoan = ?
                WHERE maPhieuKham = ?
                  AND maNhanVien = ?
                  AND trangThai IN ('CHO_KHAM', 'DANG_KHAM')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, nullable(trieuChung));
            statement.setNString(2, nullable(chanDoan));
            statement.setString(3, maPhieuKham);
            statement.setString(4, maBacSi);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không thể lưu: phiếu đã kết thúc hoặc bác sĩ không phụ trách phiếu.");
            }
        }
        ghiNhatKy(connection, maTaiKhoan, maPhieuKham, maBacSi, "Lưu nháp kết quả khám");
    }

    private void ghiNhatKy(Connection connection, String maTaiKhoan,
                           String maPhieuKham, String maBacSi,
                           String hanhDong) throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyThaoTac
                    (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua,
                     doiTuong, maDoiTuong, maNguoiChiuTrachNhiem)
                VALUES ('ACCOUNT', ?, N'PhieuKham', ?, 'SUCCESS',
                        'PhieuKham', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maTaiKhoan);
            statement.setNString(2, hanhDong);
            statement.setString(3, maPhieuKham);
            statement.setString(4, maBacSi);
            statement.setQueryTimeout(10);
            statement.executeUpdate();
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullable(String value) {
        String normalized = safe(value);
        return normalized.isEmpty() ? null : normalized;
    }

    public void ketLuan(
            Connection connection,
            String maPhieuKham,
            String maBacSi,
            String chanDoan,
            String ketLuan,
            String maLichHen,
            String maTaiKhoan) throws SQLException {

        String updateVisit = """
            UPDATE dbo.PhieuKham
            SET chanDoan = ?, ketLuan = ?, trangThai = 'DA_KET_LUAN'
            WHERE maPhieuKham = ?
              AND maNhanVien = ?
              AND trangThai IN ('CHO_KHAM', 'DANG_KHAM')
            """;

        try (PreparedStatement statement = connection.prepareStatement(updateVisit)) {
            statement.setNString(1, chanDoan);
            statement.setNString(2, ketLuan);
            statement.setString(3, maPhieuKham);
            statement.setString(4, maBacSi);
            statement.setQueryTimeout(10);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Không thể kết luận: phiếu đã kết thúc hoặc bác sĩ không phụ trách.");
            }
        }

        if (maLichHen != null && !maLichHen.isBlank()) {

            String appointmentStatus;

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT trangThai FROM dbo.LichHen WITH (UPDLOCK, HOLDLOCK) WHERE maLichHen = ?")) {

                statement.setString(1, maLichHen);
                statement.setQueryTimeout(10);

                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        throw new SQLException(
                                "Không tìm thấy lịch hẹn gắn với phiếu khám.");
                    }

                    appointmentStatus = rows.getString("trangThai");
                }
            }

            if (!"HOAN_THANH".equals(appointmentStatus)) {

                if ("DA_HUY".equals(appointmentStatus)
                        || "DA_DOI_LICH".equals(appointmentStatus)
                        || "VANG_MAT".equals(appointmentStatus)) {

                    throw new SQLException(
                            "Lịch hẹn đã hủy, đổi lịch hoặc vắng mặt; không thể tự động hoàn thành.");
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.LichHen SET trangThai = 'HOAN_THANH' WHERE maLichHen = ?")) {

                    statement.setString(1, maLichHen);
                    statement.setQueryTimeout(10);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Không cập nhật được trạng thái lịch hẹn.");
                    }
                }
            }
        }

        String auditSql = """
            INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua,
                 doiTuong, maDoiTuong, noiDung, maNguoiChiuTrachNhiem)
            VALUES ('ACCOUNT', ?, N'PhieuKham', N'Kết luận phiếu khám', 'SUCCESS',
                    'PhieuKham', ?, N'Đã lưu chẩn đoán, kết luận và hoàn tất phiếu khám.', ?)
            """;

        try (PreparedStatement statement = connection.prepareStatement(auditSql)) {
            statement.setString(1, maTaiKhoan);
            statement.setString(2, maPhieuKham);
            statement.setString(3, maBacSi);
            statement.setQueryTimeout(10);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Không ghi được nhật ký kết luận phiếu khám.");
            }
        }
    }

}
