package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.LockSource;
import com.quanlynhakhoa.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Uses only existing column-scoped account UPDATE permissions and audit INSERT. */
public final class AccountStateDao {
    static final int TIMEOUT_SECONDS = 10;
    static final String TARGET_SQL = """
            SELECT maTaiKhoan, vaiTro, trangThai, nguonKhoa
            FROM dbo.TaiKhoan WITH (UPDLOCK, HOLDLOCK)
            WHERE maTaiKhoan = ?
            """;
    static final String LOCK_SQL = """
            UPDATE dbo.TaiKhoan
            SET trangThai = 'BI_KHOA', nguonKhoa = 'ADMIN_ACTION',
                khoaLuc = SYSUTCDATETIME(), khoaDen = NULL, lyDoKhoa = ?,
                capNhatLuc = SYSUTCDATETIME()
            WHERE maTaiKhoan = ? AND trangThai = 'HOAT_DONG'
              AND vaiTro <> 'SYSTEM_ADMIN' AND maTaiKhoan <> ?
            """;
    static final String UNLOCK_SQL = """
            UPDATE dbo.TaiKhoan
            SET trangThai = 'HOAT_DONG', soLanSai = 0, lanSaiDau = NULL,
                khoaLuc = NULL, khoaDen = NULL, nguonKhoa = NULL, lyDoKhoa = NULL,
                capNhatLuc = SYSUTCDATETIME()
            WHERE maTaiKhoan = ? AND trangThai = 'BI_KHOA'
              AND nguonKhoa = 'ADMIN_ACTION' AND vaiTro <> 'SYSTEM_ADMIN'
            """;
    static final String AUDIT_SQL = """
            INSERT INTO dbo.NhatKyThaoTac
                (loaiTacNhan, maTaiKhoan, chucNang, hanhDong,
                 ketQua, doiTuong, maDoiTuong, noiDung)
            VALUES ('ACCOUNT', ?, N'TaiKhoan', ?, 'SUCCESS', 'TaiKhoan', ?, ?)
            """;

    public record Target(String accountId, Role role, AccountStatus status, LockSource lockSource) { }

    public Target lockTarget(Connection connection, String targetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TARGET_SQL)) {
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            statement.setString(1, targetId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
                String source = rows.getString("nguonKhoa");
                return new Target(rows.getString("maTaiKhoan"), Role.valueOf(rows.getString("vaiTro")),
                        AccountStatus.valueOf(rows.getString("trangThai")),
                        source == null ? null : LockSource.valueOf(source));
            }
        }
    }

    public void lock(Connection connection, String targetId, String actorId, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOCK_SQL)) {
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            statement.setNString(1, reason);
            statement.setString(2, targetId);
            statement.setString(3, actorId);
            requireOne(statement.executeUpdate());
        }
    }

    public void unlock(Connection connection, String targetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UNLOCK_SQL)) {
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            statement.setString(1, targetId);
            requireOne(statement.executeUpdate());
        }
    }

    public void audit(Connection connection, String actorId, String targetId, boolean locking, String reason)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(AUDIT_SQL)) {
            statement.setQueryTimeout(TIMEOUT_SECONDS);
            statement.setString(1, actorId);
            statement.setNString(2, locking ? "Khóa tài khoản" : "Mở khóa tài khoản");
            statement.setString(3, targetId);
            statement.setNString(4, locking ? reason : "Mở khóa do quản trị viên");
            requireOne(statement.executeUpdate());
        }
    }

    private static void requireOne(int count) throws SQLException {
        if (count != 1) throw new SQLException("Expected exactly one affected row");
    }
}
