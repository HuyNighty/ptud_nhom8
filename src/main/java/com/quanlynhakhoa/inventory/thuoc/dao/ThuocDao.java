package com.quanlynhakhoa.inventory.thuoc.dao;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.thuoc.model.Thuoc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/** DAO chỉ truy cập SQL; mọi giá trị người dùng đều truyền bằng PreparedStatement. */
public class ThuocDao {
    private static final String SELECT_THUOC = """
            SELECT TOP (500)
                   t.maThuoc, t.tenThuoc, t.donViTinh, t.kiemSoatDacBiet,
                   t.hamLuong, t.hoatChat, t.dinhMucToiThieu, t.daDatHang,
                   COALESCE(l.tongTon, CONVERT(BIGINT, 0)) AS tongTon,
                   l.hanGanNhat
            FROM dbo.Thuoc AS t
            LEFT JOIN (
                SELECT maThuoc,
                       SUM(CONVERT(BIGINT, soLuongTon)) AS tongTon,
                       MIN(hanSuDung) AS hanGanNhat
                FROM dbo.LoHang
                WHERE maThuoc IS NOT NULL
                GROUP BY maThuoc
            ) AS l ON l.maThuoc = t.maThuoc
            """;

    public List<Thuoc> timKiem(String tuKhoa) throws SQLException {
        String sql = SELECT_THUOC + """
                 WHERE (? = N'' OR t.maThuoc LIKE ? OR t.tenThuoc LIKE ?
                        OR COALESCE(t.hoatChat, N'') LIKE ?)
                 ORDER BY t.tenThuoc, t.maThuoc
                """;
        String tuKhoaChuan = tuKhoa == null ? "" : tuKhoa.trim();
        String mau = "%" + tuKhoaChuan + "%";
        List<Thuoc> ketQua = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setNString(1, tuKhoaChuan);
            stmt.setString(2, mau);
            stmt.setNString(3, mau);
            stmt.setNString(4, mau);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ketQua.add(docMotDong(rs));
            }
        }
        return ketQua;
    }

    public boolean tonTai(Connection con, String maThuoc) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT 1 FROM dbo.Thuoc WHERE maThuoc = ?")) {
            stmt.setString(1, maThuoc);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void them(Connection con, Thuoc thuoc) throws SQLException {
        String sql = """
                INSERT INTO dbo.Thuoc
                    (maThuoc, tenThuoc, donViTinh, kiemSoatDacBiet,
                     hamLuong, hoatChat, dinhMucToiThieu, daDatHang)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ganTruong(stmt, thuoc);
            stmt.executeUpdate();
        }
    }

    public boolean capNhat(Connection con, Thuoc thuoc) throws SQLException {
        String sql = """
                UPDATE dbo.Thuoc
                SET tenThuoc = ?, donViTinh = ?, kiemSoatDacBiet = ?,
                    hamLuong = ?, hoatChat = ?, dinhMucToiThieu = ?, daDatHang = ?
                WHERE maThuoc = ?
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setNString(1, thuoc.getTenThuoc());
            stmt.setNString(2, thuoc.getDonViTinh());
            stmt.setBoolean(3, thuoc.isKiemSoatDacBiet());
            stmt.setNString(4, thuoc.getHamLuong());
            stmt.setNString(5, thuoc.getHoatChat());
            stmt.setInt(6, thuoc.getDinhMucToiThieu());
            stmt.setBoolean(7, thuoc.isDaDatHang());
            stmt.setString(8, thuoc.getMaThuoc());
            return stmt.executeUpdate() > 0;
        }
    }

    public void ghiNhatKy(Connection con, String maTaiKhoan, String hanhDong, String maThuoc)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyThaoTac
                    (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong)
                VALUES ('ACCOUNT', ?, N'Thuoc', ?, 'SUCCESS', 'Thuoc', ?)
                """;
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maTaiKhoan);
            stmt.setNString(2, hanhDong);
            stmt.setString(3, maThuoc);
            stmt.executeUpdate();
        }
    }

    private void ganTruong(PreparedStatement stmt, Thuoc thuoc) throws SQLException {
        stmt.setString(1, thuoc.getMaThuoc());
        stmt.setNString(2, thuoc.getTenThuoc());
        stmt.setNString(3, thuoc.getDonViTinh());
        stmt.setBoolean(4, thuoc.isKiemSoatDacBiet());
        stmt.setNString(5, thuoc.getHamLuong());
        stmt.setNString(6, thuoc.getHoatChat());
        stmt.setInt(7, thuoc.getDinhMucToiThieu());
        stmt.setBoolean(8, thuoc.isDaDatHang());
    }

    private Thuoc docMotDong(ResultSet rs) throws SQLException {
        Thuoc thuoc = new Thuoc(
                rs.getString("maThuoc"),
                rs.getNString("tenThuoc"),
                rs.getNString("donViTinh"),
                rs.getBoolean("kiemSoatDacBiet"),
                rs.getNString("hamLuong"),
                rs.getNString("hoatChat"),
                rs.getInt("dinhMucToiThieu"),
                rs.getBoolean("daDatHang"));
        thuoc.setTongTon(rs.getLong("tongTon"));
        Date hanGanNhat = rs.getDate("hanGanNhat");
        thuoc.setHanSuDungGanNhat(hanGanNhat == null ? null : hanGanNhat.toLocalDate());
        return thuoc;
    }
}
