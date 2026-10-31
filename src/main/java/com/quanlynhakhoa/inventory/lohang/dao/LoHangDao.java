package com.quanlynhakhoa.inventory.lohang.dao;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.lohang.model.LoHang;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO chỉ đọc dbo.LoHang; tồn kho không được cập nhật trực tiếp ở màn hình tra cứu. */
public class LoHangDao {
    private static final String BASE_QUERY = """
            SELECT TOP (1000)
                   l.maLo, l.soLo, l.ngaySanXuat, l.hanSuDung,
                   l.maThuoc, l.maVatTu,
                   COALESCE(t.tenThuoc, v.tenVatTu) AS tenHang,
                   l.maNhaCungCap, ncc.tenNhaCungCap, l.soLuongTon
            FROM dbo.LoHang AS l
            LEFT JOIN dbo.Thuoc AS t ON t.maThuoc = l.maThuoc
            LEFT JOIN dbo.VatTu AS v ON v.maVatTu = l.maVatTu
            INNER JOIN dbo.NhaCungCap AS ncc ON ncc.maNhaCungCap = l.maNhaCungCap
            WHERE (? = '' OR l.maLo LIKE ? OR l.soLo LIKE ? OR l.maThuoc LIKE ?
                   OR l.maVatTu LIKE ? OR t.tenThuoc LIKE ? OR v.tenVatTu LIKE ?
                   OR l.maNhaCungCap LIKE ? OR ncc.tenNhaCungCap LIKE ?)
              AND (? = 'ALL' OR (? = 'THUOC' AND l.maThuoc IS NOT NULL)
                   OR (? = 'VATTU' AND l.maVatTu IS NOT NULL))
              AND (? = 'ALL'
                   OR (? = 'HET_HAN' AND l.hanSuDung < CAST(GETDATE() AS DATE))
                   OR (? = 'CON_HAN' AND (l.hanSuDung IS NULL OR l.hanSuDung >= CAST(GETDATE() AS DATE))))
            """;

    public List<LoHang> timKiem(String tuKhoa, String loaiHang, String hanSuDung)
            throws SQLException {
        String sql = BASE_QUERY + " ORDER BY CASE WHEN l.hanSuDung IS NULL THEN 1 ELSE 0 END, "
                + "l.hanSuDung, l.maLo";
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        String like = "%" + keyword + "%";
        String kind = loaiHang == null ? LoHang.LOAI_TAT_CA : loaiHang;
        String expiry = hanSuDung == null ? LoHang.HAN_TAT_CA : hanSuDung;
        List<LoHang> result = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            for (int i = 2; i <= 9; i++) {
                if (i == 6 || i == 7 || i == 9) stmt.setNString(i, like);
                else stmt.setString(i, like);
            }
            stmt.setString(10, kind);
            stmt.setString(11, kind);
            stmt.setString(12, kind);
            stmt.setString(13, expiry);
            stmt.setString(14, expiry);
            stmt.setString(15, expiry);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(docMotDong(rs));
            }
        }
        return result;
    }

    private LoHang docMotDong(ResultSet rs) throws SQLException {
        Date ngaySanXuat = rs.getDate("ngaySanXuat");
        Date hanSuDung = rs.getDate("hanSuDung");
        return new LoHang(
                rs.getString("maLo"),
                rs.getString("soLo"),
                ngaySanXuat == null ? null : ngaySanXuat.toLocalDate(),
                hanSuDung == null ? null : hanSuDung.toLocalDate(),
                rs.getString("maThuoc"),
                rs.getString("maVatTu"),
                rs.getNString("tenHang"),
                rs.getString("maNhaCungCap"),
                rs.getNString("tenNhaCungCap"),
                rs.getInt("soLuongTon"));
    }
}
