package com.quanlynhakhoa.dichvu.dao;

import com.quanlynhakhoa.dichvu.model.DichVu;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Caller owns connection and transaction. No physical delete is provided. */
public class DichVuDao {
    public List<DichVu> find(Connection connection, String keyword, Boolean active) throws SQLException {
        String sql = """
                SELECT maDichVu, tenDichVu, donGia, moTa, laCanLamSang, dangApDung
                FROM dbo.DichVu
                WHERE (CHARINDEX(?, maDichVu) > 0 OR CHARINDEX(?, tenDichVu) > 0)
                  AND (? IS NULL OR dangApDung = ?)
                ORDER BY maDichVu
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            statement.setNString(1, keyword);
            statement.setNString(2, keyword);
            if (active == null) {
                statement.setNull(3, Types.BIT);
                statement.setNull(4, Types.BIT);
            } else {
                statement.setBoolean(3, active);
                statement.setBoolean(4, active);
            }
            List<DichVu> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new DichVu(rows.getString("maDichVu"),
                        rows.getNString("tenDichVu"), rows.getBigDecimal("donGia"),
                        rows.getNString("moTa"), rows.getBoolean("laCanLamSang"), rows.getBoolean("dangApDung")));
            }
            return List.copyOf(result);
        }
    }

    public boolean exists(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT maDichVu FROM dbo.DichVu WHERE maDichVu = ?")) {
            statement.setQueryTimeout(15);
            statement.setString(1, id);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    public void insert(Connection connection, DichVu value) throws SQLException {
        write(connection, """
                INSERT INTO dbo.DichVu (tenDichVu, donGia, moTa, laCanLamSang, dangApDung, maDichVu)
                VALUES (?, ?, ?, ?, ?, ?)
                """, value);
    }

    public void update(Connection connection, DichVu value) throws SQLException {
        write(connection, """
                UPDATE dbo.DichVu SET tenDichVu=?, donGia=?, moTa=?, laCanLamSang=?, dangApDung=?
                WHERE maDichVu=?
                """, value);
    }

    private void write(Connection connection, String sql, DichVu value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(15);
            statement.setNString(1, value.tenDichVu());
            statement.setBigDecimal(2, value.donGia());
            if (value.moTa() == null) statement.setNull(3, Types.NVARCHAR);
            else statement.setNString(3, value.moTa());
            statement.setBoolean(4, value.laCanLamSang());
            statement.setBoolean(5, value.dangApDung());
            statement.setString(6, value.maDichVu());
            requireOne(statement.executeUpdate());
        }
    }

    public void changeActive(Connection connection, String id, boolean active) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE dbo.DichVu SET dangApDung=? WHERE maDichVu=?")) {
            statement.setQueryTimeout(15);
            statement.setBoolean(1, active);
            statement.setString(2, id);
            requireOne(statement.executeUpdate());
        }
    }
    public void updateInformation(Connection c,DichVu value)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.DichVu SET tenDichVu=?,moTa=?,laCanLamSang=?,dangApDung=? WHERE maDichVu=?")) {
            s.setQueryTimeout(15);s.setNString(1,value.tenDichVu());s.setNString(2,value.moTa());s.setBoolean(3,value.laCanLamSang());s.setBoolean(4,value.dangApDung());s.setString(5,value.maDichVu());requireOne(s.executeUpdate());
        }
    }
    public void updatePrice(Connection c,DichVu value)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.DichVu SET donGia=?,dangApDung=? WHERE maDichVu=?")) {
            s.setQueryTimeout(15);s.setBigDecimal(1,value.donGia());s.setBoolean(2,value.dangApDung());s.setString(3,value.maDichVu());requireOne(s.executeUpdate());
        }
    }

    public void audit(Connection connection, String accountId, String id, String action) throws SQLException {
        // DesktopSession is local, not a PhienDangNhap row: sessionId deliberately omitted.
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong)
                VALUES ('ACCOUNT', ?, N'Bảng giá dịch vụ', ?, 'SUCCESS', 'DichVu', ?)
                """)) {
            statement.setQueryTimeout(15);
            statement.setString(1, accountId);
            statement.setNString(2, action);
            statement.setString(3, id);
            requireOne(statement.executeUpdate());
        }
    }

    private static void requireOne(int count) throws SQLException {
        if (count != 1) throw new SQLException("Cần đúng một bản ghi; dữ liệu có thể đã thay đổi.");
    }
}
