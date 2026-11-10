package com.quanlynhakhoa.security;

import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.security.KeHoachDangNhap;
import com.quanlynhakhoa.dao.taikhoan.TaiKhoanDao;
import com.quanlynhakhoa.dao.nhanvien.NhanVienDao;
import com.quanlynhakhoa.entity.TaiKhoan;
import com.quanlynhakhoa.model.nhanvien.ThongTinNhanVien;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

/**
 * R3-A2B implementation candidate. NOT wired to GUI, no DB integration test yet.
 * Caller must explicitly instantiate and supply a trusted ConnectionSource.
 * Every login opens one transaction and holds account row locks through commit.
 */
public final class JdbcAuthService {
    @FunctionalInterface
    public interface ConnectionSource {
        Connection open() throws SQLException;
    }

    // Indexed username lookup ensures concurrent logins for an existing account serialize.
    static final String LOCK_ACCOUNT_SQL = """
            SELECT maTaiKhoan
            FROM dbo.TaiKhoan WITH (UPDLOCK, HOLDLOCK)
            WHERE tenDangNhap = ?
            """;
    static final String LOCK_EMPLOYEE_SQL = """
            SELECT maNhanVien
            FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
            WHERE maNhanVien = ?
            """;
    static final String UPDATE_ACCOUNT_SQL = """
            UPDATE dbo.TaiKhoan
            SET trangThai = ?, soLanSai = ?, lanSaiDau = ?,
                khoaLuc = ?, khoaDen = ?, nguonKhoa = ?, lyDoKhoa = ?,
                lanDangNhapCuoi = ?, capNhatLuc = ?
            WHERE maTaiKhoan = ?
            """;

    private final ConnectionSource connectionSource;
    private final TaiKhoanDao taiKhoan;
    private final NhanVienDao nhanVien;
    private final LoginPolicy policy;
    private final Clock clock;

    public JdbcAuthService(ConnectionSource connectionSource, LoginPolicy policy, Clock clock) {
        this.connectionSource = Objects.requireNonNull(connectionSource, "connectionSource");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.taiKhoan = new TaiKhoanDao();
        this.nhanVien = new NhanVienDao();
    }

    /**
     * Makes real SQL writes when invoked against a writable DB. No production wiring in R3-A2B.
     * Never log username, password, hash, or raw SQLException (may contain private details).
     */
    public void bindSession(com.quanlynhakhoa.security.DesktopSession session) {
        session.bindCurrentAccountCheck(new com.quanlynhakhoa.security.JdbcSessionVerifier(connectionSource));
    }

    public KetQuaDangNhap authenticate(String username, String rawPassword) throws SQLException {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return KetQuaDangNhap.rejected();
        }
        try (Connection connection = connectionSource.open()) {
            // A connection from a pool with altered transaction state is out of scope for now.
            if (!connection.getAutoCommit()) {
                throw new SQLException("Expected a fresh auto-commit JDBC connection");
            }
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                if (!lockExistingRow(connection, LOCK_ACCOUNT_SQL, username)) {
                    connection.commit();
                    return KetQuaDangNhap.rejected();
                }
                Optional<TaiKhoan> found = taiKhoan.findByUsername(connection, username);
                if (found.isEmpty()) {
                    throw new SQLException("Locked account was not visible in the same transaction");
                }
                TaiKhoan account = found.get();
                ThongTinNhanVien employee = null;
                if (account.employeeId() != null) {
                    if (lockExistingRow(connection, LOCK_EMPLOYEE_SQL, account.employeeId())) {
                        employee = nhanVien.findById(connection, account.employeeId()).orElse(null);
                    }
                }
                LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
                KeHoachDangNhap plan = policy.evaluate(account, employee, rawPassword, nowUtc);
                if (plan.stateChanged()) {
                    try (PreparedStatement statement = connection.prepareStatement(UPDATE_ACCOUNT_SQL)) {
                        bindUpdate(statement, plan, account.id());
                        statement.setQueryTimeout(10);
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Expected exactly one updated account row");
                        }
                    }
                }
                connection.commit();
                return plan.authenticated()
                        ? new KetQuaDangNhap(true, account.id(), account.role(), plan.forcePasswordChange())
                        : KetQuaDangNhap.rejected();
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    private static boolean lockExistingRow(Connection connection, String sql, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    static void bindUpdate(PreparedStatement statement, KeHoachDangNhap plan, String accountId)
            throws SQLException {
        statement.setString(1, plan.nextStatus().name());
        statement.setInt(2, plan.nextFailedAttempts());
        setDateTime(statement, 3, plan.nextFirstFailureAt());
        setDateTime(statement, 4, plan.nextLockedAt());
        setDateTime(statement, 5, plan.nextLockedUntil());
        if (plan.nextLockSource() == null) statement.setNull(6, Types.VARCHAR);
        else statement.setString(6, plan.nextLockSource().name());
        if (plan.nextLockReason() == null) statement.setNull(7, Types.NVARCHAR);
        else statement.setNString(7, plan.nextLockReason());
        setDateTime(statement, 8, plan.nextLastLoginAt());
        setDateTime(statement, 9, plan.nextUpdatedAt());
        statement.setString(10, accountId);
    }

    private static void setDateTime(PreparedStatement statement, int position,
                                    LocalDateTime value) throws SQLException {
        if (value == null) statement.setNull(position, Types.TIMESTAMP);
        else statement.setTimestamp(position, Timestamp.valueOf(value));
    }
}
