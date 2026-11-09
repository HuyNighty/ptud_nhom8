package com.quanlynhakhoa.nghiepvu.phieukham.dao;

import com.quanlynhakhoa.model.phieukham.ThongTinDauDonThuoc;

import com.quanlynhakhoa.model.phieukham.DongChiTietDonThuoc;
import com.quanlynhakhoa.model.phieukham.ThuocChonKeDon;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DonThuocDao {



    public List<ThuocChonKeDon> danhSachThuoc(Connection connection) throws SQLException {
        String sql = """
                SELECT maThuoc, tenThuoc, donViTinh, kiemSoatDacBiet, hamLuong, hoatChat
                FROM dbo.Thuoc
                ORDER BY tenThuoc, maThuoc
                """;
        List<ThuocChonKeDon> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new ThuocChonKeDon(
                            rows.getString("maThuoc"),
                            rows.getNString("tenThuoc"),
                            rows.getNString("donViTinh"),
                            rows.getBoolean("kiemSoatDacBiet"),
                            rows.getNString("hamLuong"),
                            rows.getNString("hoatChat")));
                }
            }
        }
        return List.copyOf(result);
    }

    public Optional<ThongTinDauDonThuoc> timDonThuocTheoPhieu(
            Connection connection, String maPhieuKham) throws SQLException {
        String sql = "SELECT maDonThuoc, loiDan FROM dbo.DonThuoc WHERE maPhieuKham = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maPhieuKham);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(new ThongTinDauDonThuoc(
                        rows.getString("maDonThuoc"), rows.getNString("loiDan")));
            }
        }
    }

    public List<DongChiTietDonThuoc> danhSachChiTiet(
            Connection connection, String maDonThuoc) throws SQLException {
        String sql = """
                SELECT ct.maThuoc, t.tenThuoc, t.donViTinh, ct.soLuong,
                       ct.lieuDung, ct.cachDung, ct.soNgayDung,
                       t.kiemSoatDacBiet, t.hoatChat
                FROM dbo.ChiTietDonThuoc AS ct
                INNER JOIN dbo.Thuoc AS t ON t.maThuoc = ct.maThuoc
                WHERE ct.maDonThuoc = ?
                ORDER BY t.tenThuoc, ct.maThuoc
                """;
        List<DongChiTietDonThuoc> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maDonThuoc);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new DongChiTietDonThuoc(
                            rows.getString("maThuoc"),
                            rows.getNString("tenThuoc"),
                            rows.getNString("donViTinh"),
                            rows.getInt("soLuong"),
                            rows.getNString("lieuDung"),
                            rows.getNString("cachDung"),
                            rows.getInt("soNgayDung"),
                            rows.getBoolean("kiemSoatDacBiet"),
                            rows.getNString("hoatChat")));
                }
            }
        }
        return List.copyOf(result);
    }

    public void taoDonThuoc(Connection connection, String maDonThuoc,
                            String maPhieuKham, String maBenhNhan,
                            String maBacSi, String loiDan) throws SQLException {
        String sql = """
                INSERT INTO dbo.DonThuoc
                    (maDonThuoc, maPhieuKham, maBenhNhan, maNhanVien, ngayKe, loiDan)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maDonThuoc);
            statement.setString(2, maPhieuKham);
            statement.setString(3, maBenhNhan);
            statement.setString(4, maBacSi);
            statement.setDate(5, Date.valueOf(java.time.LocalDate.now()));
            statement.setNString(6, loiDan == null || loiDan.isBlank() ? null : loiDan);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không tạo được đơn thuốc.");
            }
        }
    }

    public void taoChiTiet(Connection connection, String maDonThuoc,
                           DongChiTietDonThuoc item) throws SQLException {
        String sql = """
                INSERT INTO dbo.ChiTietDonThuoc
                    (maDonThuoc, maThuoc, soLuong, lieuDung, cachDung, soNgayDung)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maDonThuoc);
            statement.setString(2, item.maThuoc());
            statement.setInt(3, item.soLuong());
            statement.setNString(4, item.lieuDung());
            statement.setNString(5, item.cachDung());
            statement.setInt(6, item.soNgayDung());
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không lưu được thuốc " + item.maThuoc() + ".");
            }
        }
    }

    public void ghiNhatKy(Connection connection, String maTaiKhoan,
                          String maDonThuoc, String maBacSi,
                          String noiDung) throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyThaoTac
                    (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua,
                     doiTuong, maDoiTuong, noiDung, maNguoiChiuTrachNhiem)
                VALUES ('ACCOUNT', ?, N'DonThuoc', N'Kê đơn thuốc', 'SUCCESS',
                        'DonThuoc', ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maTaiKhoan);
            statement.setString(2, maDonThuoc);
            statement.setNString(3, noiDung);
            statement.setString(4, maBacSi);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Không ghi được nhật ký kê đơn thuốc.");
            }
        }
    }
}
