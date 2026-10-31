package com.quanlynhakhoa.examination;

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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic JDBC contract checks: never connects to SQL Server or writes data. */
public final class ExamQueueSelfTest {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError("INT4-A check " + checks + ": " + label);
    }
    private static void reject(Class<? extends Throwable> expected, Checked call) {
        boolean thrown = false;
        try { call.run(); }
        catch (Throwable error) {
            if (!expected.isInstance(error)) throw new AssertionError(error);
            thrown = true;
        }
        check(thrown, "reject " + expected.getSimpleName());
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "TKTEST", role, false));
    }
    private static final LocalDate DAY = LocalDate.of(2026, 10, 31);

    private static Map<String, Object> row(int n, String state) {
        Map<String, Object> map = new HashMap<>();
        map.put("maPhieuKham", String.format("PK%06d", n));
        map.put("maBenhNhan", "BN000001");
        map.put("tenBenhNhan", "Bệnh nhân thử nghiệm");
        map.put("maBacSi", "TESTBS01");
        map.put("tenBacSi", "Bác sĩ thử nghiệm");
        map.put("maLichHen", "LH000001");
        map.put("thoiGianTiepNhan", Timestamp.valueOf(DAY.atTime(10, 30)));
        map.put("trangThai", state);
        return map;
    }
    private static <T> T proxy(Class<T> type, InvocationHandler impl) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, impl));
    }
    private static Object fallback(String name, Class<?> returnType) {
        if (name.equals("toString")) return "FakeJdbc";
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
    private static final class Jdbc {
        final List<Map<String, Object>> rows = new ArrayList<>();
        final Map<Integer, Object> binds = new HashMap<>();
        int openedStatements, executedQueries, resultClosed, statementClosed, connectionClosed;
        boolean failQuery;
        String sql;
        Connection connection() {
            return proxy(Connection.class, (p, method, args) -> {
                String name = method.getName();
                if (name.equals("prepareStatement")) {
                    sql = (String)args[0];
                    openedStatements++;
                    return statement();
                }
                if (name.equals("close")) { connectionClosed++; return null; }
                if (name.equals("isClosed")) return connectionClosed > 0;
                return fallback(name, method.getReturnType());
            });
        }
        PreparedStatement statement() {
            return proxy(PreparedStatement.class, (p, method, args) -> {
                String name = method.getName();
                if (name.startsWith("set") && args != null && args.length == 2 && args[0] instanceof Integer i) {
                    binds.put(i, args[1]); return null;
                }
                if (name.equals("executeQuery")) {
                    executedQueries++;
                    if (failQuery) throw new SQLException("synthetic read failure");
                    return result();
                }
                if (name.equals("executeUpdate") || name.equals("execute") || name.equals("addBatch")) {
                    throw new AssertionError("Read-only DAO tried a SQL write");
                }
                if (name.equals("close")) { statementClosed++; return null; }
                return fallback(name, method.getReturnType());
            });
        }
        ResultSet result() {
            AtomicInteger index = new AtomicInteger(-1);
            return proxy(ResultSet.class, (p, method, args) -> {
                String name = method.getName();
                if (name.equals("next")) return index.incrementAndGet() < rows.size();
                if (name.equals("close")) { resultClosed++; return null; }
                if (name.equals("getString") || name.equals("getNString") || name.equals("getTimestamp")) {
                    return rows.get(index.get()).get(args[0]);
                }
                return fallback(name, method.getReturnType());
            });
        }
    }

    public static void main(String[] args) throws Exception {
        check(ExamQueueDao.LIMIT == 200, "bounded query limit");
        ExamQueueRow row = new ExamQueueRow("PK000001", "BN000001", "Name",
                "TESTBS01", "Doctor", "LH000001", DAY.atStartOfDay(), "CHO_KHAM");
        check(row.statusText().equals("Chờ khám"), "waiting display");
        ExamQueueRow active = new ExamQueueRow("PK000002", "BN000002", "Name",
                "TESTBS01", "Doctor", null, DAY.atStartOfDay(), "DANG_KHAM");
        check(active.statusText().equals("Đang khám"), "in-progress display");
        reject(IllegalArgumentException.class, () -> new ExamQueueRow("PK3", "BN3", "Name",
                "BS", "Doctor", null, DAY.atStartOfDay(), "DA_KET_LUAN"));
        reject(NullPointerException.class, () -> new ExamQueueRow("PK3", "BN3", "Name",
                "BS", "Doctor", null, null, "CHO_KHAM"));

        Jdbc doctor = new Jdbc();
        doctor.rows.add(row(1, "CHO_KHAM"));
        doctor.rows.add(row(2, "DANG_KHAM"));
        ExamQueueService doctorService = new ExamQueueService(session(Role.DOCTOR), doctor::connection);
        ExamQueueDao.Result found = doctorService.load(DAY);
        check(found.rows().size() == 2, "two open examinations returned");
        check(!found.truncated(), "small result not truncated");
        check(found.rows().get(1).status().equals("DANG_KHAM"), "status mapping");
        check(doctor.sql.contains("dbo.PhieuKham") && doctor.sql.contains("dbo.TaiKhoan"), "trusted account scoped SQL");
        check(doctor.sql.contains("CHO_KHAM") && doctor.sql.contains("DANG_KHAM"), "only two open states");
        check(doctor.sql.contains("SELECT TOP (201)"), "query capped at 201");
        check(doctor.sql.stripLeading().startsWith("SELECT") && !doctor.sql.contains("DELETE FROM"), "read-only query");
        check(doctor.binds.get(1).equals(Timestamp.valueOf(DAY.atStartOfDay())), "start of day inclusive");
        check(doctor.binds.get(2).equals(Timestamp.valueOf(DAY.plusDays(1).atStartOfDay())), "next day exclusive");
        check(doctor.binds.get(3).equals(0), "doctor cannot bypass SQL scope");
        check(doctor.binds.get(4).equals("TKTEST"), "bound authenticated account identity");
        check(doctor.openedStatements == 1 && doctor.executedQueries == 1, "one prepared SELECT");
        check(doctor.resultClosed == 1 && doctor.statementClosed == 1 && doctor.connectionClosed == 1, "all JDBC resources closed");
        reject(UnsupportedOperationException.class, () -> found.rows().clear());

        Jdbc admin = new Jdbc();
        admin.rows.add(row(7, "CHO_KHAM"));
        ExamQueueDao.Result all = new ExamQueueService(session(Role.SYSTEM_ADMIN), admin::connection).load(DAY);
        check(all.rows().size() == 1, "admin can read queue");
        check(admin.binds.get(3).equals(1), "admin SQL scope flag");
        check(admin.connectionClosed == 1, "admin connection closed");

        AtomicInteger unauthorizedConnection = new AtomicInteger();
        ExamQueueService receptionist = new ExamQueueService(session(Role.RECEPTIONIST), () -> {
            unauthorizedConnection.incrementAndGet(); return new Jdbc().connection();
        });
        reject(AccessDeniedException.class, () -> receptionist.load(DAY));
        check(unauthorizedConnection.get() == 0, "receptionist rejected before JDBC");
        ExamQueueService manager = new ExamQueueService(session(Role.CLINIC_MANAGER), () -> {
            unauthorizedConnection.incrementAndGet(); return new Jdbc().connection();
        });
        reject(AccessDeniedException.class, () -> manager.load(DAY));
        check(unauthorizedConnection.get() == 0, "manager rejected before JDBC");
        DesktopSession loggedOut = session(Role.DOCTOR);
        loggedOut.logout();
        ExamQueueService loggedOutService = new ExamQueueService(loggedOut, () -> {
            unauthorizedConnection.incrementAndGet(); return new Jdbc().connection();
        });
        reject(AccessDeniedException.class, () -> loggedOutService.load(DAY));
        check(unauthorizedConnection.get() == 0, "logout rejected before JDBC");
        reject(NullPointerException.class, () -> doctorService.load(null));

        Jdbc failed = new Jdbc();
        failed.failQuery = true;
        reject(SQLException.class, () -> new ExamQueueService(session(Role.DOCTOR), failed::connection).load(DAY));
        check(failed.statementClosed == 1 && failed.connectionClosed == 1, "failure closes JDBC");
        check(failed.resultClosed == 0, "no result to close after failed query");

        Jdbc big = new Jdbc();
        for (int i = 1; i <= 201; i++) big.rows.add(row(i, "CHO_KHAM"));
        ExamQueueDao.Result limited = new ExamQueueService(session(Role.DOCTOR), big::connection).load(DAY);
        check(limited.rows().size() == 200, "only 200 exposed");
        check(limited.truncated(), "truncation reported");
        check(big.connectionClosed == 1 && big.resultClosed == 1, "bounded result closes JDBC");

        System.out.println("PASS: M1-INT4-A exam queue (" + checks + " checks; synthetic JDBC, no database)");
    }
}
