package com.quanlynhakhoa.quantri.nhanvien.dao;

import com.quanlynhakhoa.model.nhanvien.ThongTinNhanVien;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Optional;

/** R3-A1: read-only JDBC DAO. */
public final class EmployeeDao {
    private static final String FIND_BY_ID = """
            SELECT maNhanVien, hoTen, chucDanh, trangThai, ngayNghiViec
            FROM dbo.NhanVien
            WHERE maNhanVien = ?
            """;

    public Optional<ThongTinNhanVien> findById(Connection connection, String employeeId)
            throws SQLException {
        if (connection == null) throw new IllegalArgumentException("connection required");
        if (employeeId == null || employeeId.isBlank()) return Optional.empty();
        try (PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID)) {
            stmt.setString(1, employeeId);
            stmt.setQueryTimeout(10);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Date end = rs.getDate("ngayNghiViec");
                return Optional.of(new ThongTinNhanVien(
                        rs.getString("maNhanVien"),
                        rs.getString("hoTen"),
                        rs.getString("chucDanh"),
                        TrangThaiNhanVien.valueOf(rs.getString("trangThai")),
                        end == null ? null : end.toLocalDate()
                ));
            }
        }
    }
}
