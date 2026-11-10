package com.quanlynhakhoa.dao.phieukham;

import com.quanlynhakhoa.model.phieukham.ThongTinPhanCongKham;
import com.quanlynhakhoa.model.phieukham.ThongTinChiDinhCanLamSang;
import com.quanlynhakhoa.model.phieukham.DichVuCanLamSangChon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CanLamSangDao {



    public List<DichVuCanLamSangChon> danhSachDichVu(Connection connection) throws SQLException {
        String sql = """
                SELECT maDichVu, tenDichVu, donGia, moTa
                FROM dbo.DichVu
                WHERE laCanLamSang = 1 AND dangApDung = 1
                ORDER BY tenDichVu, maDichVu
                """;
        List<DichVuCanLamSangChon> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new DichVuCanLamSangChon(
                            rows.getString("maDichVu"),
                            rows.getNString("tenDichVu"),
                            rows.getBigDecimal("donGia"),
                            rows.getNString("moTa")));
                }
            }
        }
        return List.copyOf(result);
    }

    public List<ThongTinChiDinhCanLamSang> danhSachChiDinh(Connection connection, String maPhieuKham)
            throws SQLException {
        String sql = """
                SELECT c.maChiDinh, c.maPhieuKham, c.maDichVu,
                       d.tenDichVu, c.yeuCau, c.ketQua,
                       c.maNhanVien, CAST(NULL AS NVARCHAR(100)) AS tenBacSi,
                       c.thoiGianChiDinh, c.thoiGianKetQua, c.trangThai
                FROM dbo.ChiDinhCanLamSang AS c
                INNER JOIN dbo.DichVu AS d ON d.maDichVu = c.maDichVu
                WHERE c.maPhieuKham = ?
                ORDER BY c.thoiGianChiDinh DESC, c.maChiDinh DESC
                """;
        List<ThongTinChiDinhCanLamSang> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maPhieuKham);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Timestamp orderedAt = rows.getTimestamp("thoiGianChiDinh");
                    Timestamp resultAt = rows.getTimestamp("thoiGianKetQua");
                    result.add(new ThongTinChiDinhCanLamSang(
                            rows.getString("maChiDinh"),
                            rows.getString("maPhieuKham"),
                            rows.getString("maDichVu"),
                            rows.getNString("tenDichVu"),
                            rows.getNString("yeuCau"),
                            rows.getNString("ketQua"),
                            rows.getString("maNhanVien"),
                            rows.getNString("tenBacSi"),
                            orderedAt == null ? null : orderedAt.toLocalDateTime(),
                            resultAt == null ? null : resultAt.toLocalDateTime(),
                            rows.getString("trangThai")));
                }
            }
        }
        return List.copyOf(result);
    }

    public Optional<ThongTinPhanCongKham> timPhieuKham(Connection connection, String maPhieuKham)
            throws SQLException {
        return readAssignment(connection, maPhieuKham, false);
    }

    public Optional<ThongTinPhanCongKham> khoaPhieuKham(Connection connection, String maPhieuKham)
            throws SQLException {
        return readAssignment(connection, maPhieuKham, true);
    }

    private Optional<ThongTinPhanCongKham> readAssignment(Connection connection, String maPhieuKham,
                                                          boolean lock) throws SQLException {
        String sql = lock
                ? "SELECT maNhanVien, trangThai FROM dbo.PhieuKham WITH (UPDLOCK, HOLDLOCK) WHERE maPhieuKham = ?"
                : "SELECT maNhanVien, trangThai FROM dbo.PhieuKham WHERE maPhieuKham = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maPhieuKham);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(new ThongTinPhanCongKham(
                        rows.getString("maNhanVien"), rows.getString("trangThai")));
            }
        }
    }

    public boolean dichVuConApDung(Connection connection, String maDichVu) throws SQLException {
        String sql = """
                SELECT 1
                FROM dbo.DichVu
                WHERE maDichVu = ? AND laCanLamSang = 1 AND dangApDung = 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maDichVu);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    public void taoChiDinh(Connection connection, String maChiDinh,
                           String maPhieuKham, String maDichVu,
                           String yeuCau, String maBacSi,
                           String maTaiKhoan) throws SQLException {
        String sql = """
                INSERT INTO dbo.ChiDinhCanLamSang
                    (maChiDinh, maPhieuKham, maDichVu, yeuCau,
                     maNhanVien, thoiGianChiDinh, trangThai)
                VALUES (?, ?, ?, ?, ?, SYSDATETIME(), 'CHO_THUC_HIEN')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maChiDinh);
            statement.setString(2, maPhieuKham);
            statement.setString(3, maDichVu);
            statement.setNString(4, yeuCau);
            statement.setString(5, maBacSi);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không tạo được chỉ định cận lâm sàng.");
            }
        }
        ghiNhatKy(connection, maTaiKhoan, maChiDinh, maBacSi,
                "Tạo chỉ định cận lâm sàng");
    }

    public void luuKetQua(Connection connection, String maChiDinh,
                          String maPhieuKham, String ketQua,
                          String maBacSi, String maTaiKhoan) throws SQLException {
        String sql = """
                UPDATE dbo.ChiDinhCanLamSang
                SET ketQua = ?, thoiGianKetQua = SYSDATETIME(), trangThai = 'DA_CO_KET_QUA'
                WHERE maChiDinh = ? AND maPhieuKham = ?
                  AND trangThai = 'CHO_THUC_HIEN'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, ketQua);
            statement.setString(2, maChiDinh);
            statement.setString(3, maPhieuKham);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không lưu được kết quả: chỉ định không tồn tại, đã có kết quả hoặc không thuộc phiếu khám này.");
            }
        }
        ghiNhatKy(connection, maTaiKhoan, maChiDinh, maBacSi,
                "Nhập kết quả cận lâm sàng");
    }

    private static void ghiNhatKy(Connection connection, String maTaiKhoan,
                                  String maChiDoiTuong, String maBacSi,
                                  String hanhDong) throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyThaoTac
                    (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua,
                     doiTuong, maDoiTuong, maNguoiChiuTrachNhiem)
                VALUES ('ACCOUNT', ?, N'CanLamSang', ?, 'SUCCESS',
                        'ChiDinhCanLamSang', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maTaiKhoan);
            statement.setNString(2, hanhDong);
            statement.setString(3, maChiDoiTuong);
            statement.setString(4, maBacSi);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không ghi được nhật ký thao tác cận lâm sàng.");
            }
        }
    }
}
