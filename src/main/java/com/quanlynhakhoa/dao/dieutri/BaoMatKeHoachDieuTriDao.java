package com.quanlynhakhoa.dao.dieutri;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL-side restrictions for doctor-owned treatment plans. No inventory dependencies. */
public final class BaoMatKeHoachDieuTriDao {
    public List<Object[]> danhSachCuaBacSi(Connection con, String doctorId) throws SQLException {
        String sql = "SELECT TOP (300) k.maKeHoach,k.maHoSo,b.maBenhNhan,b.hoTen,"
                + "k.maPhieuKham,k.ngayLap,k.mucTieu,k.trangThai,nv.hoTen "
                + "FROM dbo.KeHoachDieuTri k JOIN dbo.HoSoBenhAn h ON h.maHoSo=k.maHoSo "
                + "JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan "
                + "JOIN dbo.NhanVien nv ON nv.maNhanVien=k.maNhanVien "
                + "WHERE k.maNhanVien=? ORDER BY k.ngayLap DESC,k.maKeHoach DESC";
        return rows(con, sql, 9, doctorId);
    }

    public List<Object[]> chiTietCuaBacSi(Connection con, String planId, String doctorId) throws SQLException {
        String sql = "SELECT d.maChiTiet,d.maDichVu,v.tenDichVu,d.viTriRang,d.ngayDuKien,"
                + "d.ngayThucHien,d.ketQua,d.trangThai,nv.hoTen,d.ghiChu "
                + "FROM dbo.ChiTietDieuTri d JOIN dbo.KeHoachDieuTri k ON k.maKeHoach=d.maKeHoach "
                + "JOIN dbo.DichVu v ON v.maDichVu=d.maDichVu "
                + "LEFT JOIN dbo.NhanVien nv ON nv.maNhanVien=d.maNhanVienThucHien "
                + "WHERE d.maKeHoach=? AND k.maNhanVien=? ORDER BY d.ngayDuKien,d.maChiTiet";
        return rows(con, sql, 10, planId, doctorId);
    }

    public void requireDoctorOwnsChart(Connection con, String chartId, String visitId,
                                        String doctorId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.HoSoBenhAn h "
                + "JOIN dbo.PhieuKham p WITH (UPDLOCK, HOLDLOCK) ON p.maHoSo=h.maHoSo "
                + "WHERE h.maHoSo=? AND p.maNhanVien=? AND p.trangThai <> 'DA_HUY' "
                + "AND (? IS NULL OR p.maPhieuKham=?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, chartId);
            stmt.setString(2, doctorId);
            stmt.setString(3, visitId);
            stmt.setString(4, visitId);
            stmt.setQueryTimeout(10);
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Hồ sơ hoặc phiếu khám chưa thuộc bác sĩ đang đăng nhập.");
                }
            }
        }
    }

    public void requireOwnedPlan(Connection con, String planId, String doctorId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.KeHoachDieuTri WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maKeHoach=? AND maNhanVien=?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, planId);
            stmt.setString(2, doctorId);
            stmt.setQueryTimeout(10);
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Bác sĩ không phụ trách kế hoạch điều trị này.");
            }
        }
    }

    public void requireOwnedStep(Connection con, int detailId, String doctorId) throws SQLException {
        String sql = "SELECT 1 FROM dbo.KeHoachDieuTri k WITH (UPDLOCK, HOLDLOCK) "
                + "JOIN dbo.ChiTietDieuTri d ON d.maKeHoach=k.maKeHoach "
                + "WHERE d.maChiTiet=? AND k.maNhanVien=?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, detailId);
            stmt.setString(2, doctorId);
            stmt.setQueryTimeout(10);
            try (ResultSet result = stmt.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Bác sĩ không phụ trách bước điều trị này.");
            }
        }
    }

    private static List<Object[]> rows(Connection con, String sql, int columns, String... args)
            throws SQLException {
        List<Object[]> data = new ArrayList<>();
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) stmt.setString(i + 1, args[i]);
            stmt.setQueryTimeout(10);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    Object[] row = new Object[columns];
                    for (int i = 0; i < columns; i++) row[i] = result.getObject(i + 1);
                    data.add(row);
                }
            }
        }
        return data;
    }
}
