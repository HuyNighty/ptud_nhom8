package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.model.LuotKham;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class HoSoBenhAnDao {
    private static final int THOI_GIAN_CHO_TOI_DA = 15;

    private final BenhNhanDao.ConnectionProvider cachMoKetNoi;

    public HoSoBenhAnDao() {
        this(TeamDevConnection::open);
    }

    public HoSoBenhAnDao(BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.cachMoKetNoi = java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi");
    }

    public HoSoBenhAn timTheoBenhNhan(String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng "
                + "FROM dbo.HoSoBenhAn WHERE maBenhNhan = ?";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return new HoSoBenhAn(
                            ketQua.getString(1),
                            ketQua.getString(2),
                            ketQua.getDate(3).toLocalDate(),
                            ketQua.getNString(4),
                            ketQua.getNString(5));
                }
            }
        }
        return null;
    }

    public ArrayList<LuotKham> layDanhSachLuotKham(String maBenhNhan) throws SQLException {
        ArrayList<LuotKham> danhSachLuotKham = new ArrayList<>();
        String sql = "SELECT TOP 200 p.maPhieuKham, p.thoiGianTiepNhan, n.hoTen, "
                + "p.trieuChung, p.chanDoan, p.ketLuan, p.trangThai "
                + "FROM dbo.PhieuKham AS p "
                + "LEFT JOIN dbo.NhanVien AS n ON n.maNhanVien = p.maNhanVien "
                + "WHERE p.maBenhNhan = ? "
                + "ORDER BY p.thoiGianTiepNhan DESC";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    LocalDateTime thoiGianTiepNhan = null;
                    Timestamp thoiGianTrongDatabase = ketQua.getTimestamp(2);
                    if (thoiGianTrongDatabase != null) {
                        thoiGianTiepNhan = thoiGianTrongDatabase.toLocalDateTime();
                    }

                    LuotKham luotKham = new LuotKham(
                            ketQua.getString(1),
                            thoiGianTiepNhan,
                            ketQua.getNString(3),
                            ketQua.getNString(4),
                            ketQua.getNString(5),
                            ketQua.getNString(6),
                            ketQua.getString(7));
                    danhSachLuotKham.add(luotKham);
                }
            }
        }
        return danhSachLuotKham;
    }

    public void ghiNhatKyXem(String maTaiKhoan, String maBenhNhan) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'HoSoBenhAn', N'Xem hồ sơ bệnh án', 'SUCCESS', 'BenhNhan', ?)";
        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maTaiKhoan);
            cauLenh.setString(2, maBenhNhan);
            cauLenh.executeUpdate();
        }
    }

    public String timMaHoSo(Connection ketNoi, String maBenhNhan) throws SQLException {
        String sql = "SELECT maHoSo FROM dbo.HoSoBenhAn WITH (UPDLOCK, HOLDLOCK) WHERE maBenhNhan = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return ketQua.getString(1);
                }
                return null;
            }
        }
    }

    public boolean capNhatTienSu(Connection ketNoi, String maHoSo, String tienSuBenh, String diUng)
            throws SQLException {
        String sql = "UPDATE dbo.HoSoBenhAn SET tienSuBenh = ?, diUng = ? WHERE maHoSo = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setNString(1, tienSuBenh);
            cauLenh.setNString(2, diUng);
            cauLenh.setString(3, maHoSo);
            int soDongDaSua = cauLenh.executeUpdate();
            return soDongDaSua > 0;
        }
    }

    private PreparedStatement taoCauLenh(Connection ketNoi, String sql) throws SQLException {
        PreparedStatement cauLenh = ketNoi.prepareStatement(sql);
        try {
            cauLenh.setQueryTimeout(THOI_GIAN_CHO_TOI_DA);
            return cauLenh;
        } catch (SQLException | RuntimeException loi) {
            try {
                cauLenh.close();
            } catch (SQLException loiKhiDong) {
                loi.addSuppressed(loiKhiDong);
            }
            throw loi;
        }
    }
}
