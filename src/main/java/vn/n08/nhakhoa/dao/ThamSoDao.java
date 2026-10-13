package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Bảng ThamSoHeThong — tham số cấu hình được (số lần sai mật khẩu, ngưỡng hạn dùng…). */
public class ThamSoDao {
    public record ThamSo(String ten, String giaTri, String moTa) {}

    public String get(Connection c, String ten, String macDinh) throws SQLException {
        return Jdbc.one(c, "SELECT giaTri FROM ThamSoHeThong WHERE ten = ?", rs -> rs.getString(1), ten).orElse(macDinh);
    }

    public int getInt(Connection c, String ten, int macDinh) throws SQLException {
        String v = get(c, ten, null);
        try {
            return v == null ? macDinh : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return macDinh;
        }
    }

    public List<ThamSo> all(Connection c) throws SQLException {
        return Jdbc.list(c, "SELECT ten, giaTri, moTa FROM ThamSoHeThong ORDER BY ten",
                rs -> new ThamSo(rs.getString(1), rs.getString(2), rs.getString(3)));
    }

    public int set(Connection c, String ten, String giaTri) throws SQLException {
        return Jdbc.update(c, "UPDATE ThamSoHeThong SET giaTri = ? WHERE ten = ?", giaTri, ten);
    }
}
