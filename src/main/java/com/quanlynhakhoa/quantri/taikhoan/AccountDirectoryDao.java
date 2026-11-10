package com.quanlynhakhoa.quantri.taikhoan;

import com.quanlynhakhoa.model.taikhoan.KetQuaDanhSachTaiKhoan;
import com.quanlynhakhoa.model.taikhoan.DongDanhSachTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AccountDirectoryDao {
    private static final int PAGE_LIMIT = 200;
    private static final String SEARCH_SQL = """
            SELECT TOP (201)
                   a.maTaiKhoan, a.tenDangNhap, a.tenHienThi, a.maNhanVien,
                   n.hoTen AS tenNhanVien, a.vaiTro, a.trangThai,
                   a.phaiDoiMatKhau, a.lanDangNhapCuoi, a.nguonKhoa
            FROM dbo.TaiKhoan AS a
            LEFT JOIN dbo.NhanVien AS n ON n.maNhanVien = a.maNhanVien
            WHERE (? = N''
                OR CHARINDEX(?, a.tenDangNhap) > 0
                OR CHARINDEX(?, a.tenHienThi) > 0
                OR CHARINDEX(?, COALESCE(n.hoTen, N'')) > 0
                OR CHARINDEX(?, a.maTaiKhoan) > 0)
              AND (? IS NULL OR a.vaiTro = ?)
              AND (? IS NULL OR a.trangThai = ?)
            ORDER BY a.maTaiKhoan
            """;



    public KetQuaDanhSachTaiKhoan search(Connection connection, String keyword, VaiTroHeThong role,
                         TrangThaiTaiKhoan status) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(keyword, "keyword");
        try (PreparedStatement statement = connection.prepareStatement(SEARCH_SQL)) {
            statement.setQueryTimeout(12);
            for (int i = 1; i <= 5; i++) statement.setNString(i, keyword);
            statement.setString(6, role == null ? null : role.name());
            statement.setString(7, role == null ? null : role.name());
            statement.setString(8, status == null ? null : status.name());
            statement.setString(9, status == null ? null : status.name());
            List<DongDanhSachTaiKhoan> rows = new ArrayList<>();
            boolean truncated = false;
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    if (rows.size() >= PAGE_LIMIT) {
                        truncated = true;
                        break;
                    }
                    Timestamp lastLogin = rs.getTimestamp("lanDangNhapCuoi");
                    String lockSource = rs.getString("nguonKhoa");
                    rows.add(new DongDanhSachTaiKhoan(
                            rs.getString("maTaiKhoan"),
                            rs.getString("tenDangNhap"),
                            rs.getString("tenHienThi"),
                            rs.getString("maNhanVien"),
                            rs.getString("tenNhanVien"),
                            VaiTroHeThong.valueOf(rs.getString("vaiTro")),
                            TrangThaiTaiKhoan.valueOf(rs.getString("trangThai")),
                            rs.getBoolean("phaiDoiMatKhau"),
                            lastLogin == null ? null : lastLogin.toLocalDateTime(),
                            lockSource == null ? null : NguonKhoaTaiKhoan.valueOf(lockSource)));
                }
            }
            return new KetQuaDanhSachTaiKhoan(rows, truncated);
        }
    }
}
