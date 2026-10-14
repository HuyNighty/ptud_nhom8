
package com.quanlynhakhoa.features.auth.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "n08.desktop.enabled=false"
)
class AuthSchemaNegativeIT {

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
                .truncatedTo(ChronoUnit.SECONDS);
    }

    private static String accountId() {
        return "T" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 9);
    }

    private static byte[] tokenHash() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static void execute(
            Connection connection,
            String sql,
            Object... parameters
    ) throws SQLException {
        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                Object value = parameters[i];
                int position = i + 1;

                if (value instanceof UUID uuid) {
                    ps.setString(position, uuid.toString());
                } else if (value instanceof LocalDateTime time) {
                    ps.setTimestamp(
                            position, Timestamp.valueOf(time));
                } else if (value instanceof byte[] bytes) {
                    ps.setBytes(position, bytes);
                } else if (value == null) {
                    ps.setNull(position, Types.VARCHAR);
                } else {
                    ps.setObject(position, value);
                }
            }

            ps.executeUpdate();
        }
    }

    private static String createAccount(
            Connection connection
    ) throws SQLException {
        String id = accountId();
        String username = "gate_" + UUID.randomUUID()
                .toString().replace("-", "");

        execute(connection, """
            INSERT INTO dbo.TaiKhoan (
                maTaiKhoan,
                tenDangNhap,
                tenHienThi,
                matKhauHash,
                vaiTro,
                matKhauCapNhatLuc
            )
            VALUES (?, ?, N'Schema Test',
                    'TEST_ONLY_NOT_A_REAL_HASH',
                    'SYSTEM_ADMIN', ?)
            """, id, username, now());

        return id;
    }

    private static void insertSession(
            Connection connection,
            UUID sessionId,
            String accountId,
            LocalDateTime created,
            LocalDateTime lastActivity,
            LocalDateTime expires,
            LocalDateTime revoked,
            String revokeReason
    ) throws SQLException {
        execute(connection, """
            INSERT INTO dbo.PhienDangNhap (
                sessionId, maTaiKhoan, kenh,
                taoLuc, hoatDongCuoiLuc, hetHanLuc,
                thuHoiLuc, lyDoThuHoi
            )
            VALUES (
                CONVERT(uniqueidentifier, ?),
                ?, 'API', ?, ?, ?, ?, ?
            )
            """,
                sessionId, accountId, created,
                lastActivity, expires, revoked, revokeReason);
    }

    private static UUID createValidSession(
            Connection connection,
            String accountId
    ) throws SQLException {
        UUID id = UUID.randomUUID();
        LocalDateTime t = now();

        insertSession(
                connection, id, accountId,
                t.minusMinutes(5), t, t.plusHours(8),
                null, null
        );

        return id;
    }

    private static void insertToken(
            Connection connection,
            UUID tokenId,
            UUID sessionId,
            byte[] hash,
            LocalDateTime issued,
            LocalDateTime expires,
            LocalDateTime consumed,
            UUID replacement
    ) throws SQLException {
        execute(connection, """
            INSERT INTO dbo.RefreshToken (
                tokenId, sessionId, tokenHash,
                phatHanhLuc, hetHanLuc,
                daSuDungLuc, tokenThayTheId
            )
            VALUES (
                CONVERT(uniqueidentifier, ?),
                CONVERT(uniqueidentifier, ?),
                ?, ?, ?, ?,
                CONVERT(uniqueidentifier, ?)
            )
            """,
                tokenId, sessionId, hash,
                issued, expires, consumed, replacement);
    }

    private static void assertRejected(
            int expectedErrorCode,
            String constraint,
            Executable operation
    ) {
        SQLException error =
                assertThrows(SQLException.class, operation);

        assertEquals(
                expectedErrorCode,
                error.getErrorCode(),
                error.getMessage()
        );

        assertTrue(
                error.getMessage().contains(constraint),
                "Wrong SQL constraint: " + error.getMessage()
        );
    }

    @Test
    void nt07_rejectsInvalidSessionExpiration()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);
            LocalDateTime t = now();

            assertRejected(
                    547,
                    "CK_PhienDangNhap_ThoiGian",
                    () -> insertSession(
                            connection, UUID.randomUUID(),
                            account, t, t, t, null, null
                    )
            );
        });
    }

    @Test
    void nt08_rejectsRevocationWithoutReason()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);
            LocalDateTime t = now();

            assertRejected(
                    547,
                    "CK_PhienDangNhap_ThuHoi",
                    () -> insertSession(
                            connection, UUID.randomUUID(),
                            account, t.minusMinutes(5), t,
                            t.plusHours(8), t, null
                    )
            );
        });
    }

    @Test
    void nt09_rejectsTwoCurrentTokens()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);
            UUID session = createValidSession(
                    connection, account);
            LocalDateTime t = now();

            insertToken(
                    connection, UUID.randomUUID(),
                    session, tokenHash(),
                    t.minusMinutes(1), t.plusHours(1),
                    null, null
            );

            assertRejected(
                    2601,
                    "UX_RefreshToken_OneCurrent",
                    () -> insertToken(
                            connection, UUID.randomUUID(),
                            session, tokenHash(),
                            t.minusMinutes(1), t.plusHours(1),
                            null, null
                    )
            );
        });
    }

    @Test
    void nt10_rejectsDuplicateTokenHash()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);

            UUID sessionA = createValidSession(
                    connection, account);
            UUID sessionB = createValidSession(
                    connection, account);

            byte[] sameHash = tokenHash();
            LocalDateTime t = now();

            insertToken(
                    connection, UUID.randomUUID(),
                    sessionA, sameHash,
                    t.minusMinutes(1), t.plusHours(1),
                    null, null
            );

            assertRejected(
                    2627,
                    "UQ_RefreshToken_Hash",
                    () -> insertToken(
                            connection, UUID.randomUUID(),
                            sessionB, sameHash,
                            t.minusMinutes(1), t.plusHours(1),
                            null, null
                    )
            );
        });
    }

    @Test
    void nt11_rejectsCrossSessionReplacement()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);

            UUID sessionA = createValidSession(
                    connection, account);
            UUID sessionB = createValidSession(
                    connection, account);

            UUID replacementInB = UUID.randomUUID();
            LocalDateTime t = now();

            insertToken(
                    connection, replacementInB,
                    sessionB, tokenHash(),
                    t.minusMinutes(1), t.plusHours(1),
                    null, null
            );

            assertRejected(
                    547,
                    "FK_RefreshToken_ThayTheCungPhien",
                    () -> insertToken(
                            connection, UUID.randomUUID(),
                            sessionA, tokenHash(),
                            t.minusMinutes(2), t.plusHours(1),
                            t.minusMinutes(1),
                            replacementInB
                    )
            );
        });
    }

    @Test
    void nt12_rejectsConsumedBeforeIssued()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);
            UUID session = createValidSession(
                    connection, account);
            LocalDateTime t = now();

            assertRejected(
                    547,
                    "CK_RefreshToken_ThoiGian",
                    () -> insertToken(
                            connection, UUID.randomUUID(),
                            session, tokenHash(),
                            t.minusMinutes(1), t.plusHours(1),
                            t.minusMinutes(2), null
                    )
            );
        });
    }

    @Test
    void pt01_acceptsValidTokenRotation()
            throws Exception {
        withRollback(connection -> {
            String account = createAccount(connection);
            UUID session = createValidSession(
                    connection, account);

            UUID tokenA = UUID.randomUUID();
            UUID tokenB = UUID.randomUUID();
            LocalDateTime t = now();

            insertToken(
                    connection, tokenA, session, tokenHash(),
                    t.minusMinutes(2), t.plusHours(1),
                    null, null
            );

            execute(connection, """
                UPDATE dbo.RefreshToken
                SET daSuDungLuc = ?
                WHERE tokenId = CONVERT(uniqueidentifier, ?)
                """, t.minusMinutes(1), tokenA);

            insertToken(
                    connection, tokenB, session, tokenHash(),
                    t.minusMinutes(1), t.plusHours(1),
                    null, null
            );

            execute(connection, """
                UPDATE dbo.RefreshToken
                SET tokenThayTheId =
                    CONVERT(uniqueidentifier, ?)
                WHERE tokenId = CONVERT(uniqueidentifier, ?)
                """, tokenB, tokenA);

            try (PreparedStatement ps =
                         connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM dbo.RefreshToken
                    WHERE sessionId =
                        CONVERT(uniqueidentifier, ?)
                    AND daSuDungLuc IS NULL
                    AND thuHoiLuc IS NULL
                    """)) {

                ps.setString(1, session.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        });
    }
}
