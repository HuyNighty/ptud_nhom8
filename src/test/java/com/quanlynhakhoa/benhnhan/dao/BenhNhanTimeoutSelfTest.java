package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Exercises the real DAO/service with synthetic JDBC; no driver, network or database. */
public final class BenhNhanTimeoutSelfTest {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static BenhNhan patient(String id) {
        return new BenhNhan(id, "Synthetic Patient", LocalDate.of(2000, 1, 1),
                "NAM", "0909123456", null, null, null);
    }

    private static BenhNhanService service(FakeJdbc jdbc) {
        DesktopSession session = DesktopSession.fromSuccessfulLogin(
                new AuthDecision(true, "SYNTHETIC", Role.RECEPTIONIST, false));
        return new BenhNhanService(session, jdbc::open);
    }

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }

    private static void expectTimeout(FakeJdbc jdbc, SqlAction action) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Expected SQLTimeoutException");
        } catch (SQLTimeoutException expected) {
            check(expected == jdbc.timeout, "original timeout propagates");
        }
        jdbc.checkClosed();
    }

    public static void main(String[] args) throws Exception {
        FakeJdbc all = new FakeJdbc();
        BenhNhanDao dao = new BenhNhanDao(all::open);
        dao.getAllBenhNhan();
        dao.timKiem("Synthetic", "NAM");
        dao.timTheoMa("BN000001");
        dao.timNghiTrung("0909123456", "Synthetic Patient", LocalDate.of(2000, 1, 1), null);
        try (Connection con = all.open()) {
            check("BN000001".equals(dao.taoMaBenhNhanMoi(con)), "patient ID generated");
            check("HS000001".equals(dao.taoMaHoSoMoi(con)), "record ID generated");
            dao.themBenhNhan(con, patient("BN000001"), "SYNTHETIC");
            dao.themHoSoBenhAn(con, "HS000001", "BN000001");
            check(dao.capNhatBenhNhan(con, patient("BN000001")), "update result retained");
            dao.ghiNhatKy(con, "SYNTHETIC", "Synthetic", "BN000001");
            check(all.closes == 4, "DAO leaves caller-owned transaction connection open");
        }
        check(all.statements.size() == 10 && all.executions == 10, "all ten DAO operations executed");
        all.checkClosed();

        // Each standalone read must propagate a timeout and close its statement/connection.
        for (int read = 0; read < 4; read++) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.timeoutAt = 1;
            BenhNhanService service = service(jdbc);
            int operation = read;
            expectTimeout(jdbc, () -> {
                switch (operation) {
                    case 0 -> service.timKiem("", "");
                    case 1 -> service.timKiem("Synthetic", "NAM");
                    case 2 -> service.xemChiTiet("BN000001");
                    case 3 -> service.timNghiTrung(patient(null));
                    default -> throw new AssertionError();
                }
            });
            check(jdbc.commits == 0 && jdbc.rollbacks == 0, "standalone read owns no transaction");
        }

        // Fail at every step: patient ID, INSERT, record ID, record INSERT, audit.
        for (int step = 1; step <= 5; step++) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.timeoutAt = step;
            BenhNhan bn = patient(null);
            expectTimeout(jdbc, () -> service(jdbc).themBenhNhan(bn));
            check(jdbc.opens == 1, "create uses one connection");
            check(jdbc.commits == 0 && jdbc.rollbacks == 1, "create timeout rolls back exactly once");
            check(jdbc.pendingWrites == 0 && jdbc.committedWrites == 0, "no partial create or audit");
            check(bn.getMaBenhNhan() == null, "failed generated ID cleared for retry");
            check(jdbc.executions == step, "create stops at failed statement");
        }
        // Fail either UPDATE or its audit; neither may commit alone.
        for (int step = 1; step <= 2; step++) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.timeoutAt = step;
            BenhNhan bn = patient("BN000001");
            expectTimeout(jdbc, () -> service(jdbc).capNhatBenhNhan(bn));
            check(jdbc.opens == 1 && jdbc.commits == 0 && jdbc.rollbacks == 1, "update timeout atomic");
            check(jdbc.pendingWrites == 0 && jdbc.committedWrites == 0, "no partial update or audit");
            check("BN000001".equals(bn.getMaBenhNhan()), "update ID preserved for retry");
        }
        FakeJdbc create = new FakeJdbc();
        check("BN000001".equals(service(create).themBenhNhan(patient(null))), "create succeeds");
        check(create.commits == 1 && create.rollbacks == 0 && create.committedWrites == 3,
                "patient, record and audit commit together");
        create.checkClosed();
        FakeJdbc update = new FakeJdbc();
        service(update).capNhatBenhNhan(patient("BN000001"));
        check(update.commits == 1 && update.rollbacks == 0 && update.committedWrites == 2,
                "update and audit commit together");
        update.checkClosed();

        FakeJdbc setupFailure = new FakeJdbc();
        setupFailure.failTimeoutSetup = true;
        expectTimeout(setupFailure, () -> service(setupFailure).themBenhNhan(patient(null)));
        check(setupFailure.executions == 0 && setupFailure.rollbacks == 1,
                "timeout setup failure closes statement and rolls back before execution");
        System.out.println("PASS: BENH NHAN JDBC TIMEOUT (" + checks + " checks; synthetic JDBC only)");
    }

    private static final class StatementState {
        int timeout;
        boolean closed;
    }

    private static final class FakeJdbc {
        final SQLTimeoutException timeout = new SQLTimeoutException("Synthetic timeout", "HYT00");
        final List<StatementState> statements = new ArrayList<>();
        int timeoutAt;
        boolean failTimeoutSetup;
        boolean autoCommit = true;
        int opens, closes, executions, results, closedResults, commits, rollbacks;
        int pendingWrites, committedWrites;

        Connection open() {
            opens++;
            return proxy(Connection.class, (p, method, args) -> {
                return switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit;
                    case "setAutoCommit" -> { autoCommit = (boolean) args[0]; yield null; }
                    case "prepareStatement" -> statement((String) args[0]);
                    case "commit" -> {
                        commits++;
                        committedWrites += pendingWrites;
                        pendingWrites = 0;
                        yield null;
                    }
                    case "rollback" -> { rollbacks++; pendingWrites = 0; yield null; }
                    case "close" -> { closes++; yield null; }
                    default -> throw new AssertionError("Unexpected Connection." + method.getName());
                };
            });
        }

        PreparedStatement statement(String sql) {
            StatementState state = new StatementState();
            statements.add(state);
            return proxy(PreparedStatement.class, (p, method, args) -> {
                String name = method.getName();
                if (name.equals("setQueryTimeout")) {
                    state.timeout = (int) args[0];
                    if (failTimeoutSetup) throw timeout;
                    return null;
                }
                if (name.equals("close")) { state.closed = true; return null; }
                if (name.startsWith("set")) return null;
                if (name.equals("executeQuery") || name.equals("executeUpdate")) {
                    check(state.timeout > 0 && state.timeout <= 30, "bounded timeout set before " + name);
                    check(!state.closed, "statement remains open during execution");
                    executions++;
                    if (timeoutAt == executions) throw timeout;
                    if (name.equals("executeQuery")) return resultSet(sql.contains("MAX("));
                    if (!autoCommit) pendingWrites++;
                    return 1;
                }
                throw new AssertionError("Unexpected PreparedStatement." + name);
            });
        }

        ResultSet resultSet(boolean generatedId) {
            results++;
            boolean[] served = {false};
            return proxy(ResultSet.class, (p, method, args) -> {
                return switch (method.getName()) {
                    case "next" -> {
                        boolean next = generatedId && !served[0];
                        served[0] = true;
                        yield next;
                    }
                    case "getInt" -> 1;
                    case "close" -> { closedResults++; yield null; }
                    default -> throw new AssertionError("Unexpected ResultSet." + method.getName());
                };
            });
        }

        void checkClosed() {
            check(opens == closes, "all owned connections closed");
            check(results == closedResults, "all result sets closed");
            check(statements.stream().allMatch(s -> s.closed), "all prepared statements closed");
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
