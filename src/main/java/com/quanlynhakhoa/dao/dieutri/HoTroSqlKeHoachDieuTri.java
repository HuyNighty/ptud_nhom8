package com.quanlynhakhoa.dao.dieutri;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Local treatment-plan SQL helpers. Does not depend on inventory modules. */
public final class HoTroSqlKeHoachDieuTri {
    private HoTroSqlKeHoachDieuTri() { }

    public static String layMaNhanVien(Connection connection, String accountId) throws SQLException {
        String sql = "SELECT nv.maNhanVien FROM dbo.TaiKhoan tk "
                + "INNER JOIN dbo.NhanVien nv ON nv.maNhanVien=tk.maNhanVien "
                + "WHERE tk.maTaiKhoan=? AND tk.trangThai='HOAT_DONG' "
                + "AND nv.trangThai='DANG_LAM_VIEC' AND (nv.chucDanh='BS' OR tk.vaiTro='SYSTEM_ADMIN')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId);
            statement.setQueryTimeout(10);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("TÃ i khoáº£n cáº§n liÃªn káº¿t vá»›i bÃ¡c sÄ© Ä‘ang lÃ m viá»‡c.");
                }
                return result.getString(1);
            }
        }
    }

    public static void ghiNhatKy(Connection connection, String accountId,
                                 String action, String object, String objectId) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'KeHoachDieuTri', ?, 'SUCCESS', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId);
            statement.setNString(2, action);
            statement.setString(3, object);
            statement.setString(4, objectId);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("KhÃ´ng ghi Ä‘Æ°á»£c nháº­t kÃ½ káº¿ hoáº¡ch Ä‘iá»u trá»‹.");
            }
        }
    }
}
