package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public final class AccountCreateDao {
    private static final String EMPLOYEE_SQL = """
            SELECT n.chucDanh, n.trangThai, a.maTaiKhoan
            FROM dbo.NhanVien AS n WITH (UPDLOCK, HOLDLOCK)
            LEFT JOIN dbo.TaiKhoan AS a WITH (UPDLOCK, HOLDLOCK)
                   ON a.maNhanVien = n.maNhanVien
            WHERE n.maNhanVien = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO dbo.TaiKhoan
                (maTaiKhoan, maNhanVien, tenDangNhap, tenHienThi,
                 matKhauHash, vaiTro, trangThai, soLanSai,
                 phaiDoiMatKhau, matKhauCapNhatLuc, capNhatLuc)
            VALUES (?, ?, ?, ?, ?, ?, 'HOAT_DONG', 0, 0,
                    SYSUTCDATETIME(), SYSUTCDATETIME())
            """;

    private static final String AUDIT_SQL = """
            INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong,
                 ketQua, doiTuong, maDoiTuong, noiDung)
            VALUES ('ACCOUNT', ?, N'TaiKhoan', N'Tạo tài khoản',
                    'SUCCESS', N'TaiKhoan', ?, N'Tạo tài khoản nhân viên')
            """;

    public void requireEligibleEmployee(Connection connection, String employeeId, Role role)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        try (PreparedStatement statement = connection.prepareStatement(EMPLOYEE_SQL)) {
            statement.setString(1, employeeId);
            statement.setQueryTimeout(10);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy nhân viên.");
                if (!"DANG_LAM_VIEC".equals(rs.getString("trangThai"))) {
                    throw new IllegalArgumentException("Nhân viên đã nghỉ việc.");
                }
                if (rs.getString("maTaiKhoan") != null) {
                    throw new IllegalArgumentException("Nhân viên đã có tài khoản.");
                }
                String job = rs.getString("chucDanh");
                String expected = switch (role) {
                    case RECEPTIONIST -> "LT";
                    case DOCTOR -> "BS";
                    case NURSE -> "YT";
                    case CLINIC_MANAGER -> "QL";
                    case INVENTORY_MANAGER -> "KHO";
                    case SYSTEM_ADMIN -> throw new IllegalArgumentException("Không tạo tài khoản quản trị hệ thống tại đây.");
                };
                if (!expected.equals(job)) {
                    throw new IllegalArgumentException("Vai trò không phù hợp với chức danh nhân viên.");
                }
            }
        }
    }

    public void insertAccount(Connection connection, String id, String employeeId,
                              String username, String displayName, String hash, Role role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, id);
            statement.setString(2, employeeId);
            statement.setString(3, username);
            statement.setNString(4, displayName);
            statement.setString(5, hash);
            statement.setString(6, role.name());
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one account insert");
            }
        }
    }

    public void auditCreated(Connection connection, String actorId, String newAccountId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AUDIT_SQL)) {
            statement.setString(1, actorId);
            statement.setString(2, newAccountId);
            statement.setQueryTimeout(10);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expected exactly one audit insert");
            }
        }
    }
}
