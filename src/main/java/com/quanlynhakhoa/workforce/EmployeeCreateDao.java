package com.quanlynhakhoa.workforce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/** Workforce SQL writes. Caller owns the JDBC transaction and authorization. */
public final class EmployeeCreateDao {
    private static final String CHECK_DUPLICATE_SQL = """
            SELECT maNhanVien
            FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
            WHERE maNhanVien = ? OR soDienThoai = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO dbo.NhanVien
                (maNhanVien, hoTen, soDienThoai, chucDanh, trangThai,
                 ngaySinh, gioiTinh, email, ngayVaoLam, chuyenMon, ngayNghiViec)
            VALUES (?, ?, ?, ?, 'DANG_LAM_VIEC', ?, ?, ?, ?, ?, NULL)
            """;

    private static final String AUDIT_SQL = """
            INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong,
                 ketQua, doiTuong, maDoiTuong, noiDung)
            VALUES ('ACCOUNT', ?, N'NhanVien', N'Tạo nhân viên',
                    'SUCCESS', N'NhanVien', ?, N'Tạo hồ sơ nhân viên')
            """;

    public void requireUnique(Connection connection, EmployeeCreateRequest request) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        try (PreparedStatement statement = connection.prepareStatement(CHECK_DUPLICATE_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, request.employeeId());
            statement.setString(2, request.phone());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
                }
            }
        }
    }

    public void insert(Connection connection, EmployeeCreateRequest request) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, request.employeeId());
            statement.setNString(2, request.fullName());
            statement.setString(3, request.phone());
            statement.setString(4, request.jobTitle());
            if (request.birthDate() == null) statement.setNull(5, Types.DATE);
            else statement.setDate(5, java.sql.Date.valueOf(request.birthDate()));
            if (request.gender() == null) statement.setNull(6, Types.VARCHAR);
            else statement.setString(6, request.gender());
            if (request.email() == null) statement.setNull(7, Types.VARCHAR);
            else statement.setString(7, request.email());
            statement.setDate(8, java.sql.Date.valueOf(request.joinedAt()));
            if (request.specialty() == null) statement.setNull(9, Types.NVARCHAR);
            else statement.setNString(9, request.specialty());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one employee insert");
            }
        }
    }

    public void auditCreated(Connection connection, String actorId, String employeeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AUDIT_SQL)) {
            statement.setQueryTimeout(12);
            statement.setString(1, actorId);
            statement.setString(2, employeeId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one employee audit insert");
            }
        }
    }
}
