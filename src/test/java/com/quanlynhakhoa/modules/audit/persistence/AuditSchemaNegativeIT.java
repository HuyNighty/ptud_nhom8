
package com.quanlynhakhoa.modules.audit.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "n08.desktop.enabled=false"
)
class AuditSchemaNegativeIT {

    @Autowired
    private DataSource dataSource;

    @FunctionalInterface
    interface SqlWork {
        void run(Connection connection) throws Exception;
    }

    private void withRollback(SqlWork work) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC)
                .withNano(0);
    }

    private static void reject(
            int expectedCode,
            String expectedConstraint,
            Executable action
    ) {
        SQLException error =
                assertThrows(SQLException.class, action);

        for (SQLException current = error;
             current != null;
             current = current.getNextException()) {

            if (current.getErrorCode() == expectedCode
                    && (expectedConstraint == null
                    || current.getMessage()
                    .contains(expectedConstraint))) {
                return;
            }
        }

        fail("Wrong SQL error. Expected code "
                + expectedCode + ", constraint "
                + expectedConstraint + ". Actual: " + error);
    }

    private String createAccount(Connection connection)
            throws SQLException {

        String accountId = "T"
                + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 9);

        String username = "audit_"
                + UUID.randomUUID().toString()
                .replace("-", "");

        String sql = """
            INSERT INTO dbo.TaiKhoan (
                maTaiKhoan, tenDangNhap,
                tenHienThi, matKhauHash,
                vaiTro, matKhauCapNhatLuc
            )
            VALUES (?, ?, N'Audit Test',
                    'TEST_ONLY_NOT_A_REAL_HASH',
                    'SYSTEM_ADMIN', ?)
            """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.setString(2, username);
            ps.setTimestamp(3, Timestamp.valueOf(now()));
            ps.executeUpdate();
        }

        return accountId;
    }

    private UUID createSession(
            Connection connection,
            String accountId
    ) throws SQLException {

        UUID sessionId = UUID.randomUUID();
        LocalDateTime t = now();

        String sql = """
            INSERT INTO dbo.PhienDangNhap (
                sessionId, maTaiKhoan, kenh,
                taoLuc, hoatDongCuoiLuc, hetHanLuc
            )
            VALUES (
                CONVERT(uniqueidentifier, ?),
                ?, 'API', ?, ?, ?
            )
            """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {
            ps.setString(1, sessionId.toString());
            ps.setString(2, accountId);
            ps.setTimestamp(3,
                    Timestamp.valueOf(t.minusMinutes(1)));
            ps.setTimestamp(4, Timestamp.valueOf(t));
            ps.setTimestamp(5,
                    Timestamp.valueOf(t.plusHours(1)));
            ps.executeUpdate();
        }

        return sessionId;
    }

    private long insertAudit(
            Connection connection,
            String actorType,
            String accountId,
            UUID sessionId,
            String actorName,
            String outcome,
            String responsibleEmployee
    ) throws SQLException {

        String sql = """
            INSERT INTO dbo.NhatKyThaoTac (
                loaiTacNhan, maTaiKhoan,
                sessionId, dinhDanhTacNhan,
                chucNang, hanhDong, ketQua,
                doiTuong, maDoiTuong,
                noiDung, maNguoiChiuTrachNhiem
            )
            OUTPUT INSERTED.maNhatKy
            VALUES (
                ?, ?, CONVERT(uniqueidentifier, ?), ?,
                N'IDENTITY', N'AUDIT_INTEGRATION_TEST',
                ?, 'TaiKhoan', ?,
                N'Temporary integration test',
                ?
            )
            """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, actorType);
            ps.setString(2, accountId);
            ps.setString(3, sessionId == null
                    ? null : sessionId.toString());
            ps.setString(4, actorName);
            ps.setString(5, outcome);
            ps.setString(6, accountId);
            ps.setString(7, responsibleEmployee);

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }

    @Test
    void at01_accountActorRequiresAccount()
            throws Exception {
        withRollback(c -> reject(
                547, "CK_NhatKy_TacNhan",
                () -> insertAudit(
                        c, "ACCOUNT", null, null,
                        null, "SUCCESS", null
                )
        ));
    }

    @Test
    void at02_anonymousCannotClaimAccount()
            throws Exception {
        withRollback(c -> {
            String account = createAccount(c);

            reject(547, "CK_NhatKy_TacNhan",
                    () -> insertAudit(
                            c, "ANONYMOUS", account,
                            null, null, "DENIED", null
                    ));
        });
    }

    @Test
    void at03_maintenanceRequiresActorIdentity()
            throws Exception {
        withRollback(c -> reject(
                547, "CK_NhatKy_TacNhan",
                () -> insertAudit(
                        c, "MAINTENANCE", null, null,
                        null, "SUCCESS", null
                )
        ));
    }

    @Test
    void at04_rejectsUnknownOutcome()
            throws Exception {
        withRollback(c -> reject(
                547, "CK_NhatKy_KetQua",
                () -> insertAudit(
                        c, "MAINTENANCE", null, null,
                        "LOCAL_TEST", "UNKNOWN", null
                )
        ));
    }

    @Test
    void at05_rejectsSessionOwnedByAnotherAccount()
            throws Exception {
        withRollback(c -> {
            String accountA = createAccount(c);
            String accountB = createAccount(c);
            UUID sessionB = createSession(c, accountB);

            reject(547, "FK_NhatKy_SessionAccount",
                    () -> insertAudit(
                            c, "ACCOUNT", accountA,
                            sessionB, null, "SUCCESS", null
                    ));
        });
    }

    @Test
    void at06_rejectsAuditUpdate()
            throws Exception {
        withRollback(c -> {
            long auditId = insertAudit(
                    c, "MAINTENANCE", null, null,
                    "LOCAL_TEST", "SUCCESS", null
            );

            reject(51001, null, () -> {
                try (PreparedStatement ps =
                             c.prepareStatement("""
                        UPDATE dbo.NhatKyThaoTac
                        SET noiDung = N'Changed'
                        WHERE maNhatKy = ?
                        """)) {
                    ps.setLong(1, auditId);
                    ps.executeUpdate();
                }
            });
        });
    }

    @Test
    void at07_rejectsAuditDelete()
            throws Exception {
        withRollback(c -> {
            long auditId = insertAudit(
                    c, "MAINTENANCE", null, null,
                    "LOCAL_TEST", "SUCCESS", null
            );

            reject(51001, null, () -> {
                try (PreparedStatement ps =
                             c.prepareStatement("""
                        DELETE FROM dbo.NhatKyThaoTac
                        WHERE maNhatKy = ?
                        """)) {
                    ps.setLong(1, auditId);
                    ps.executeUpdate();
                }
            });
        });
    }

    @Test
    void at08_rejectsUnknownResponsibleEmployee()
            throws Exception {
        withRollback(c -> reject(
                547, "FK_NhatKy_NguoiChiuTrachNhiem",
                () -> insertAudit(
                        c, "MAINTENANCE", null, null,
                        "LOCAL_TEST", "SUCCESS",
                        "NV_UNKNOWN"
                )
        ));
    }

    @Test
    void pt02_acceptsMatchingAccountAndSession()
            throws Exception {
        withRollback(c -> {
            String account = createAccount(c);
            UUID session = createSession(c, account);

            long auditId = insertAudit(
                    c, "ACCOUNT", account, session,
                    null, "SUCCESS", null
            );

            String sql = """
                SELECT maTaiKhoan, sessionId,
                       loaiTacNhan, ketQua
                FROM dbo.NhatKyThaoTac
                WHERE maNhatKy = ?
                """;

            try (PreparedStatement ps =
                         c.prepareStatement(sql)) {
                ps.setLong(1, auditId);

                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(
                            account,
                            rs.getString("maTaiKhoan")
                    );
                    assertEquals(
                            session,
                            UUID.fromString(
                                    rs.getString("sessionId")
                            )
                    );
                    assertEquals(
                            "ACCOUNT",
                            rs.getString("loaiTacNhan")
                    );
                    assertEquals(
                            "SUCCESS",
                            rs.getString("ketQua")
                    );
                    assertFalse(rs.next());
                }
            }
        });
    }
}
