package com.quanlynhakhoa.workforce;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic tests: validation, RBAC, commit/rollback and no leaked account creation. */
public final class EmployeeCreateSelfTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError("W1B check " + checks + ": " + message);
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static void rejects(Class<? extends Throwable> type, Operation operation) {
        try {
            operation.run();
            throw new AssertionError("Expected " + type.getSimpleName());
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("Unexpected error", failure);
            check(true, "Rejected " + type.getSimpleName());
        }
    }

    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }

    private static EmployeeCreateRequest good() {
        return new EmployeeCreateRequest(" testbs01 ", " Bác sĩ Thử Nghiệm ", "0912345678", "bs",
                LocalDate.of(1999, 4, 5), "NAM", "doctor@example.test", LocalDate.now(), "Nha tổng quát");
    }

    private static EmployeeCreateRequest change(String id, String phone, String job) {
        EmployeeCreateRequest src = good();
        return new EmployeeCreateRequest(id, src.fullName(), phone, job, src.birthDate(), src.gender(),
                src.email(), src.joinedAt(), src.specialty());
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static final class SimulatedDb {
        int opens, commits, rollbacks, inserts, audits, checks;
        boolean duplicate, failAudit;

        Connection open() {
            opens++;
            return proxy(Connection.class, (object, method, args) -> {
                return switch (method.getName()) {
                    case "getAutoCommit" -> true;
                    case "setAutoCommit", "setTransactionIsolation", "close" -> null;
                    case "commit" -> { commits++; yield null; }
                    case "rollback" -> { rollbacks++; yield null; }
                    case "prepareStatement" -> statement((String) args[0]);
                    case "isClosed" -> false;
                    default -> throw new AssertionError("Unexpected Connection." + method.getName());
                };
            });
        }

        PreparedStatement statement(String sql) {
            return proxy(PreparedStatement.class, (object, method, args) -> switch (method.getName()) {
                case "setString", "setNString", "setDate", "setNull", "setQueryTimeout", "close" -> null;
                case "executeQuery" -> {
                    check(sql.contains("UPDLOCK") && sql.contains("NhanVien"), "duplicate query locked");
                    checks++;
                    yield proxy(ResultSet.class, (rs, m, a) -> switch (m.getName()) {
                        case "next" -> duplicate;
                        case "close" -> null;
                        default -> throw new AssertionError("Unexpected ResultSet." + m.getName());
                    });
                }
                case "executeUpdate" -> {
                    if (sql.contains("INSERT INTO dbo.NhanVien")) {
                        inserts++;
                        yield 1;
                    }
                    if (sql.contains("INSERT INTO dbo.NhatKyThaoTac")) {
                        audits++;
                        yield failAudit ? 0 : 1;
                    }
                    throw new AssertionError("Unexpected SQL UPDATE statement");
                }
                default -> throw new AssertionError("Unexpected PreparedStatement." + method.getName());
            });
        }
    }

    public static void main(String[] args) throws Exception {
        EmployeeCreateRequest normalized = EmployeeCreateService.validate(good());
        check("TESTBS01".equals(normalized.employeeId()), "normalized ID");
        check("BS".equals(normalized.jobTitle()), "normalized job title");
        check("Bác sĩ Thử Nghiệm".equals(normalized.fullName()), "trimmed name");
        check(normalized.joinedAt() != null, "join date present");
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(change("bad#id", "0912345678", "BS")));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(change("TESTBS01", "12", "BS")));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(change("TESTBS01", "0912345678", "AD")));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(change("TESTBS01", "0912345678", "WRONG")));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(new EmployeeCreateRequest(
                "TESTBS01", "", "0912345678", "BS", null, null, null, null, null)));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(new EmployeeCreateRequest(
                "TESTBS01", "Doctor", "0912345678", "BS", LocalDate.now().plusDays(1), null, null, null, null)));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(new EmployeeCreateRequest(
                "TESTBS01", "Doctor", "0912345678", "BS", null, null, "not-an-email", null, null)));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(new EmployeeCreateRequest(
                "TESTBS01", "Doctor", "0912345678", "BS", null, "X", null, null, null)));
        rejects(IllegalArgumentException.class, () -> EmployeeCreateService.validate(new EmployeeCreateRequest(
                "TESTBS01", "Doctor", "0912345678", "BS", null, null, null, LocalDate.now().plusDays(1), null)));

        AtomicInteger deniedConnections = new AtomicInteger();
        EmployeeDirectoryService.ConnectionProvider forbidden = () -> {
            deniedConnections.incrementAndGet();
            throw new SQLException("Should not connect");
        };
        rejects(AccessDeniedException.class, () -> new EmployeeCreateService(session(Role.RECEPTIONIST), forbidden).create(good()));
        rejects(AccessDeniedException.class, () -> new EmployeeCreateService(session(Role.DOCTOR), forbidden).create(good()));
        check(deniedConnections.get() == 0, "RBAC checked before JDBC connection");
        DesktopSession closed = session(Role.CLINIC_MANAGER);
        closed.logout();
        rejects(AccessDeniedException.class, () -> new EmployeeCreateService(closed, forbidden).create(good()));
        check(deniedConnections.get() == 0, "Logged out user cannot connect");

        SimulatedDb success = new SimulatedDb();
        String id = new EmployeeCreateService(session(Role.CLINIC_MANAGER), success::open).create(good());
        check("TESTBS01".equals(id), "manager created ID");
        check(success.opens == 1 && success.checks == 1, "one connection and duplicate preflight");
        check(success.inserts == 1 && success.audits == 1 && success.commits == 1, "employee and audit committed together");
        check(success.rollbacks == 0, "no rollback on success");

        SimulatedDb duplicate = new SimulatedDb();
        duplicate.duplicate = true;
        rejects(IllegalArgumentException.class, () -> new EmployeeCreateService(session(Role.SYSTEM_ADMIN), duplicate::open).create(good()));
        check(duplicate.inserts == 0 && duplicate.audits == 0, "duplicate never writes");
        check(duplicate.rollbacks == 1 && duplicate.commits == 0, "duplicate rolls back");

        SimulatedDb auditFailure = new SimulatedDb();
        auditFailure.failAudit = true;
        rejects(SQLException.class, () -> new EmployeeCreateService(session(Role.SYSTEM_ADMIN), auditFailure::open).create(good()));
        check(auditFailure.inserts == 1 && auditFailure.audits == 1, "audit failure follows insert");
        check(auditFailure.rollbacks == 1 && auditFailure.commits == 0, "audit failure rolls back transaction");

        System.out.println("PASS: W1B employee create (" + checks + " checks; synthetic JDBC, no DB)");
    }
}
