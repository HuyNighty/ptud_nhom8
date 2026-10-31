package com.quanlynhakhoa.audit;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic JDBC only: no server, no credentials, no SQL writes. */
public final class AuditSelfTest {
    private static int checks;
    private static void check(boolean yes, String label) {
        checks++;
        if (!yes) throw new AssertionError("Audit check " + checks + ": " + label);
    }
    private static void reject(Class<? extends Throwable> type, Runnable work) {
        try { work.run(); }
        catch (Throwable ex) { check(type.isInstance(ex), "expected " + type.getSimpleName()); return; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
    private static final class FakeDb {
        final Map<Integer, Object> binds = new HashMap<>();
        final AtomicInteger opens = new AtomicInteger();
        int rowCount;
        int selectedRows;
        boolean closed;
        int queryTimeout;
        String sql;
        FakeDb(int rowCount) { this.rowCount = rowCount; }

        Connection open() {
            opens.incrementAndGet();
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "prepareStatement":
                        sql = (String) args[0];
                        return prepared();
                    case "close": closed = true; return null;
                    case "isClosed": return closed;
                    default: throw new AssertionError("Unexpected JDBC Connection call: " + method.getName());
                }
            };
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, handler);
        }
        private PreparedStatement prepared() {
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setTimestamp", "setNull", "setString", "setNString":
                        binds.put((Integer) args[0], args[1]);
                        return null;
                    case "setQueryTimeout": queryTimeout = (Integer) args[0]; return null;
                    case "executeQuery": return resultSet();
                    case "close": return null;
                    default: throw new AssertionError("Unexpected PreparedStatement call: " + method.getName());
                }
            };
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, handler);
        }
        private ResultSet resultSet() {
            InvocationHandler handler = new InvocationHandler() {
                int row;
                @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    if (method.getName().equals("next")) {
                        row++;
                        if (row <= rowCount) { selectedRows++; return true; }
                        return false;
                    }
                    if (method.getName().equals("close")) return null;
                    if (method.getName().equals("getTimestamp"))
                        return Timestamp.valueOf(LocalDateTime.of(2026, 10, 31, 6, 0));
                    if (method.getName().equals("getLong")) return (long) row;
                    if (method.getName().equals("getString") || method.getName().equals("getNString")) {
                        String field = (String) args[0];
                        return switch (field) {
                            case "loaiTacNhan" -> "ACCOUNT";
                            case "maTaiKhoan" -> "AD01";
                            case "chucNang" -> "TaiKhoan";
                            case "hanhDong" -> "Tạo tài khoản";
                            case "ketQua" -> "SUCCESS";
                            case "doiTuong" -> "TaiKhoan";
                            case "maDoiTuong" -> "TK01";
                            default -> throw new AssertionError("Unexpected audit column: " + field);
                        };
                    }
                    throw new AssertionError("Unexpected ResultSet call: " + method.getName());
                }
            };
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, handler);
        }
    }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }
    public static void main(String[] args) throws Exception {
        check(new AuditFilter(null, null, null, null).keyword().isEmpty(), "null keyword normalized");
        check(new AuditFilter(null, null, "  abc  ", "SUCCESS").keyword().equals("abc"), "trim");
        reject(IllegalArgumentException.class, () -> new AuditFilter(null, null, "x".repeat(81), null));
        reject(IllegalArgumentException.class, () -> new AuditFilter(
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1), "", null));
        reject(IllegalArgumentException.class, () -> new AuditFilter(null, null, "", "SPOOF"));
        check(new AuditFilter(null, null, "", "DENIED").result().equals("DENIED"), "denied supported");
        check(new AuditFilter(null, null, "", "FAILED").result().equals("FAILED"), "failed supported");

        AuditFilter filter = new AuditFilter(LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 10, 31), "TK01", "SUCCESS");
        FakeDb fake = new FakeDb(3);
        AuditService service = new AuditService(session(Role.SYSTEM_ADMIN), fake::open);
        AuditDao.Result result = service.search(filter);
        check(fake.opens.get() == 1, "one connection");
        check(fake.closed, "connection closed");
        check(result.rows().size() == 3 && !result.truncated(), "three rows");
        check(result.rows().get(0).id() == 1, "first row mapped");
        check(result.rows().get(0).accountId().equals("AD01"), "actor mapped");
        check(result.rows().get(0).result().equals("SUCCESS"), "outcome mapped");
        check(fake.sql.trim().startsWith("SELECT TOP (201)"), "bounded SELECT");
        check(fake.sql.contains("ORDER BY thoiGian DESC, maNhatKy DESC"), "stable order");
        check(!fake.sql.contains("noiDung") && !fake.sql.toUpperCase().contains("PASSWORD"), "redacted projection");
        check(fake.queryTimeout == 12, "JDBC timeout");
        check(fake.binds.size() == 12, "all 12 safe parameters bound");
        check(fake.binds.get(5).equals("SUCCESS"), "outcome bound");
        check(fake.binds.get(12).equals("TK01"), "search term bound");
        check(fake.binds.get(1).equals(Timestamp.valueOf("2026-10-30 17:00:00")), "Vietnam day UTC start");
        check(fake.binds.get(3).equals(Timestamp.valueOf("2026-10-31 17:00:00")), "exclusive UTC end");
        FakeDb hundred = new FakeDb(201);
        AuditDao.Result bounded = new AuditDao().search(hundred.open(), new AuditFilter(null,null,"",null));
        check(bounded.rows().size() == AuditDao.PAGE_LIMIT, "bounded to 200");
        check(bounded.truncated(), "indicates further data");
        check(hundred.selectedRows == 201, "only 201 rows examined");

        FakeDb forbidden = new FakeDb(1);
        AuditService denied = new AuditService(session(Role.DOCTOR), forbidden::open);
        reject(AccessDeniedException.class, () -> {
            try { denied.search(filter); } catch (java.sql.SQLException error) { throw new RuntimeException(error); }
        });
        check(forbidden.opens.get() == 0, "doctor cannot even open JDBC");
        FakeDb allowed = new FakeDb(1);
        AuditService manager = new AuditService(session(Role.CLINIC_MANAGER), allowed::open);
        check(manager.search(filter).rows().size() == 1, "clinic manager allowed");
        DesktopSession loggedOut = session(Role.SYSTEM_ADMIN);
        loggedOut.logout();
        FakeDb afterLogout = new FakeDb(1);
        AuditService logoutService = new AuditService(loggedOut, afterLogout::open);
        reject(AccessDeniedException.class, () -> {
            try { logoutService.search(filter); } catch (java.sql.SQLException error) { throw new RuntimeException(error); }
        });
        check(afterLogout.opens.get() == 0, "logout blocks JDBC");
        System.out.println("PASS: CORE-AUDIT-R1 (" + checks + " synthetic checks; NO DATABASE)");
    }
}
