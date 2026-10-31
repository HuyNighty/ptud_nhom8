package com.quanlynhakhoa.inventory.vattu.dao;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.vattu.model.VatTu;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO chỉ truy cập SQL Server; mọi giá trị đầu vào được bind bằng PreparedStatement. */
public class VatTuDao {
    private static final String SELECT_VATTU = """
            SELECT TOP (500)
                   v.maVatTu, v.tenVatTu, v.donViTinh, v.moTa,
                   v.dinhMucToiThieu, v.daDatHang,
                   COALESCE(l.tongTon, CONVERT(BIGINT, 0)) AS tongTon,
                   l.hanGanNhat
            FROM dbo.VatTu AS v
            LEFT JOIN (
                SELECT maVatTu,
                       SUM(CONVERT(BIGINT, soLuongTon)) AS tongTon,
                       MIN(hanSuDung) AS hanGanNhat
                FROM dbo.LoHang
                WHERE maVatTu IS NOT NULL
                GROUP BY maVatTu
            ) AS l ON l.maVatTu = v.maVatTu
            """;

    public List<VatTu> timKiem(String tuKhoa) throws SQLException {
        String sql = SELECT_VATTU + """
                 WHERE (? = N'' OR v.maVatTu LIKE ? OR v.tenVatTu LIKE ?
                        OR COALESCE(v.moTa, N'') LIKE ?)
                 ORDER BY v.tenVatTu, v.maVatTu
                """;
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        String pattern = "%" + keyword + "%";
        List<VatTu> result = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setNString(1, keyword);
            stmt.setString(2, pattern);
            stmt.setNString(3, pattern);
            stmt.setNString(4, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(docMotDong(rs));
            }
        }
        return result;
    }

    public boolean tonTai(Connection con, String maVatTu) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT 1 FROM dbo.VatTu WHERE maVatTu = ?")) {
            stmt.setString(1, maVatTu);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void them(Connection con, VatTu vatTu) throws SQLException {
        String sql = """
                INSERT INTO dbo.VatTu
                    (maVatTu, tenVatTu, donViTinh, moTa, dinhMucToiThieu, daDatHang)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ganTruong(stmt, vatTu);
            stmt.executeUpdate();
        }
    }

    public boolean capNhat(Connection con, VatTu vatTu) throws SQLException {
        String sql = """
                UPDATE dbo.VatTu
                SET tenVatTu = ?, donViTinh = ?, moTa = ?,
                    dinhMucToiThieu = ?, daDatHang = ?
                WHERE maVatTu = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setNString(1, vatTu.getTenVatTu());
            stmt.setNString(2, vatTu.getDonViTinh());
            stmt.setNString(3, vatTu.getMoTa());
            stmt.setInt(4, vatTu.getDinhMucToiThieu());
            stmt.setBoolean(5, vatTu.isDaDatHang());
            stmt.setString(6, vatTu.getMaVatTu());
            return stmt.executeUpdate() > 0;
        }
    }

    public void ghiNhatKy(Connection con, String maTaiKhoan, String hanhDong, String maVatTu)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyThaoTac
                    (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong)
                VALUES ('ACCOUNT', ?, N'VatTu', ?, 'SUCCESS', 'VatTu', ?)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maTaiKhoan);
            stmt.setNString(2, hanhDong);
            stmt.setString(3, maVatTu);
            stmt.executeUpdate();
        }
    }

    private void ganTruong(PreparedStatement stmt, VatTu vatTu) throws SQLException {
        stmt.setString(1, vatTu.getMaVatTu());
        stmt.setNString(2, vatTu.getTenVatTu());
        stmt.setNString(3, vatTu.getDonViTinh());
        stmt.setNString(4, vatTu.getMoTa());
        stmt.setInt(5, vatTu.getDinhMucToiThieu());
        stmt.setBoolean(6, vatTu.isDaDatHang());
    }

    private VatTu docMotDong(ResultSet rs) throws SQLException {
        VatTu vatTu = new VatTu(
                rs.getString("maVatTu"),
                rs.getNString("tenVatTu"),
                rs.getNString("donViTinh"),
                rs.getNString("moTa"),
                rs.getInt("dinhMucToiThieu"),
                rs.getBoolean("daDatHang"));
        vatTu.setTongTon(rs.getLong("tongTon"));
        Date hanGanNhat = rs.getDate("hanGanNhat");
        vatTu.setHanSuDungGanNhat(hanGanNhat == null ? null : hanGanNhat.toLocalDate());
        return vatTu;
    }
}
