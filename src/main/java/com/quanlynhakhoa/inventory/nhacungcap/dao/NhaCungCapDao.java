package com.quanlynhakhoa.inventory.nhacungcap.dao;

import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.inventory.nhacungcap.model.NhaCungCap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO truy cập dbo.NhaCungCap bằng PreparedStatement; không xóa vật lý dữ liệu. */
public class NhaCungCapDao {
    private static final String COT = "maNhaCungCap, tenNhaCungCap, diaChi, soDienThoai, email, trangThai ";

    public List<NhaCungCap> timKiem(String tuKhoa, String trangThai) throws SQLException {
        String sql = "SELECT TOP (500) " + COT
                + "FROM dbo.NhaCungCap "
                + "WHERE (? = '' OR maNhaCungCap LIKE ? OR tenNhaCungCap LIKE ? "
                + "OR soDienThoai LIKE ? OR COALESCE(email, '') LIKE ?) "
                + "AND (? = '' OR trangThai = ?) "
                + "ORDER BY tenNhaCungCap, maNhaCungCap";
        String keyword = tuKhoa == null ? "" : tuKhoa.trim();
        String pattern = "%" + keyword + "%";
        String state = trangThai == null ? "" : trangThai.trim();
        List<NhaCungCap> rows = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, keyword);
            stmt.setString(2, pattern);
            stmt.setNString(3, pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            stmt.setString(6, state);
            stmt.setString(7, state);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) rows.add(docMotDong(rs));
            }
        }
        return rows;
    }

    public boolean tonTai(Connection con, String maNhaCungCap) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "SELECT 1 FROM dbo.NhaCungCap WHERE maNhaCungCap = ?")) {
            stmt.setString(1, maNhaCungCap);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next(); }
        }
    }

    public void them(Connection con, NhaCungCap ncc) throws SQLException {
        String sql = "INSERT INTO dbo.NhaCungCap "
                + "(maNhaCungCap, tenNhaCungCap, diaChi, soDienThoai, email, trangThai) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, ncc.getMaNhaCungCap());
            stmt.setNString(2, ncc.getTenNhaCungCap());
            stmt.setNString(3, ncc.getDiaChi());
            stmt.setString(4, ncc.getSoDienThoai());
            stmt.setString(5, ncc.getEmail());
            stmt.setString(6, NhaCungCap.DANG_HOP_TAC);
            stmt.executeUpdate();
        }
    }

    public boolean capNhatThongTin(Connection con, NhaCungCap ncc) throws SQLException {
        String sql = "UPDATE dbo.NhaCungCap SET tenNhaCungCap = ?, diaChi = ?, "
                + "soDienThoai = ?, email = ? WHERE maNhaCungCap = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setNString(1, ncc.getTenNhaCungCap());
            stmt.setNString(2, ncc.getDiaChi());
            stmt.setString(3, ncc.getSoDienThoai());
            stmt.setString(4, ncc.getEmail());
            stmt.setString(5, ncc.getMaNhaCungCap());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean capNhatTrangThai(Connection con, String maNhaCungCap, String trangThai)
            throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "UPDATE dbo.NhaCungCap SET trangThai = ? WHERE maNhaCungCap = ?")) {
            stmt.setString(1, trangThai);
            stmt.setString(2, maNhaCungCap);
            return stmt.executeUpdate() > 0;
        }
    }

    public void ghiNhatKy(Connection con, String maTaiKhoan, String hanhDong, String maNhaCungCap)
            throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong) "
                + "VALUES ('ACCOUNT', ?, N'NhaCungCap', ?, 'SUCCESS', 'NhaCungCap', ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, maTaiKhoan);
            stmt.setNString(2, hanhDong);
            stmt.setString(3, maNhaCungCap);
            stmt.executeUpdate();
        }
    }

    private NhaCungCap docMotDong(ResultSet rs) throws SQLException {
        return new NhaCungCap(
                rs.getString("maNhaCungCap"),
                rs.getNString("tenNhaCungCap"),
                rs.getNString("diaChi"),
                rs.getString("soDienThoai"),
                rs.getString("email"),
                rs.getString("trangThai"));
    }
}
