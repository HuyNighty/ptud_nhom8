package vn.n08.nhakhoa.dao;

import vn.n08.nhakhoa.util.Jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Sinh mã nghiệp vụ (BN000011, HD000010…) từ bảng BoDemMa.
 * Phải gọi trong transaction của thao tác tạo bản ghi: lệnh UPDATE khóa dòng bộ đếm
 * đến khi commit nên hai người dùng đồng thời không nhận trùng mã.
 */
public class MaDao {
    public String next(Connection c, String loai) throws SQLException {
        int n = Jdbc.update(c, "UPDATE BoDemMa SET giaTri = giaTri + 1 WHERE loai = ?", loai);
        if (n == 0) throw new SQLException("Chưa cấu hình bộ đếm mã cho loại " + loai + " trong bảng BoDemMa");
        return Jdbc.one(c, "SELECT tienTo, doDai, giaTri FROM BoDemMa WHERE loai = ?",
                rs -> format(rs.getString("tienTo"), rs.getInt("doDai"), rs.getInt("giaTri")), loai).orElseThrow();
    }

    static String format(String prefix, int width, int value) {
        String digits = String.valueOf(value);
        if (digits.length() < width) digits = "0".repeat(width - digits.length()) + digits;
        return prefix + digits;
    }
}
