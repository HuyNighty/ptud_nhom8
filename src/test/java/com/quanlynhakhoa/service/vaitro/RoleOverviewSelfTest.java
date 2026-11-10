package com.quanlynhakhoa.service.vaitro;

import com.quanlynhakhoa.model.vaitro.TongQuanVaiTro;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.config.tinhnang.FeatureBootstrap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** W2 role/RBAC, SQL SELECT contract, and connection lifecycle; no real database. */
public final class RoleOverviewSelfTest {
    private static int checks;
    private static void check(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError("W2 check " + checks + ": " + label);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void reject(Class<? extends Throwable> expected, Checked action) {
        try { action.run(); }
        catch (Throwable error) {
            check(expected.isInstance(error), "Reject with " + expected.getSimpleName());
            return;
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
    private static DesktopSession session(VaiTroHeThong role) {
        return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true, "SYNTHETIC", role, false));
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Connection fakeConnection(AtomicInteger closes, AtomicInteger queries,
                                              String[][] results) {
        ResultSet rs = proxy(ResultSet.class, new java.lang.reflect.InvocationHandler() {
            int index = -1;
            @Override public Object invoke(Object o, java.lang.reflect.Method m, Object[] args) {
                switch (m.getName()) {
                    case "next": return ++index < results.length;
                    case "getString": return results[index][0];
                    case "getInt": return Integer.parseInt(results[index][1]);
                    case "wasNull": return false;
                    case "close": return null;
                    default: throw new UnsupportedOperationException("ResultSet." + m.getName());
                }
            }
        });
        PreparedStatement stmt = proxy(PreparedStatement.class, (o, m, args) -> {
            return switch (m.getName()) {
                case "setQueryTimeout", "close" -> null;
                case "executeQuery" -> { queries.incrementAndGet(); yield rs; }
                default -> throw new UnsupportedOperationException("PreparedStatement." + m.getName());
            };
        });
        return proxy(Connection.class, (o, m, args) -> {
            return switch (m.getName()) {
                case "prepareStatement" -> {
                    String sql = (String) args[0];
                    check(sql.contains("SELECT") && sql.contains("GROUP BY vaiTro")
                            && !sql.contains("matKhauHash") && !sql.contains("UPDATE ")
                            && !sql.contains("INSERT ") && !sql.contains("DELETE "),
                            "Only aggregate SELECT SQL used");
                    yield stmt;
                }
                case "close" -> { closes.incrementAndGet(); yield null; }
                default -> throw new UnsupportedOperationException("Connection." + m.getName());
            };
        });
    }

    public static void main(String[] args) throws Exception {
        check(FeatureBootstrap.createRegistry().contains(Module.ROLES), "Roles panel registered");
        check(VaiTroHeThong.values().length == 6, "Six canonical roles");
        check(Quyen.values().length == 26, "26 declared permissions");
        RolePermissionPolicy policy = new RolePermissionPolicy();
        for (VaiTroHeThong role : VaiTroHeThong.values()) {
            check(!policy.permissionsFor(role).isEmpty(), "Policy grants for " + role);
        }
        check(policy.permissionsFor(VaiTroHeThong.SYSTEM_ADMIN).size() == Quyen.values().length,
                "System admin has all permissions");
        check(!policy.isAllowed(VaiTroHeThong.DOCTOR, Quyen.ROLE_MANAGE), "Doctor cannot read role console");
        check(policy.isAllowed(VaiTroHeThong.CLINIC_MANAGER, Quyen.ROLE_MANAGE), "Manager can read role console");
        AtomicInteger attempts = new AtomicInteger();
        var unreachable = (com.quanlynhakhoa.service.taikhoan.DanhSachTaiKhoanService.ConnectionProvider) () -> {
            attempts.incrementAndGet();
            throw new SQLException("synthetic no SQL");
        };
        DesktopSession receptionist = session(VaiTroHeThong.RECEPTIONIST);
        DesktopSession doctor = session(VaiTroHeThong.DOCTOR);
        reject(AccessDeniedException.class,
                () -> new TongQuanVaiTroService(receptionist, unreachable).load());
        reject(AccessDeniedException.class,
                () -> new TongQuanVaiTroService(doctor, unreachable).load());
        check(attempts.get() == 0, "Denied roles cannot open JDBC connections");

        AtomicInteger closes = new AtomicInteger();
        AtomicInteger queries = new AtomicInteger();
        String[][] counts = {
                {"SYSTEM_ADMIN", "1"}, {"RECEPTIONIST", "1"}, {"DOCTOR", "2"}
        };
        var provider = (com.quanlynhakhoa.service.taikhoan.DanhSachTaiKhoanService.ConnectionProvider) () -> {
            attempts.incrementAndGet();
            return fakeConnection(closes, queries, counts);
        };
        DesktopSession manager = session(VaiTroHeThong.CLINIC_MANAGER);
        List<TongQuanVaiTro> overviews = new TongQuanVaiTroService(manager, provider).load();
        check(overviews.size() == VaiTroHeThong.values().length, "Six summary rows");
        check(overviews.get(0).role() == VaiTroHeThong.RECEPTIONIST, "Order matches canonical Role enum");
        check(overviews.get(0).accountCount() == 1, "Receptionist count");
        check(overviews.get(1).accountCount() == 2, "Doctor count");
        check(overviews.get(2).accountCount() == 0, "Absent nurse count is zero");
        check(overviews.get(5).accountCount() == 1, "System admin count");
        check(overviews.get(1).permissions().equals(policy.permissionsFor(VaiTroHeThong.DOCTOR)),
                "Permissions match real policy");
        check(closes.get() == 1 && queries.get() == 1, "Exactly one SELECT; JDBC closed");
        reject(UnsupportedOperationException.class, () -> overviews.clear());
        reject(UnsupportedOperationException.class,
                () -> overviews.get(1).permissions().clear());
        reject(IllegalArgumentException.class,
                () -> new TongQuanVaiTro(VaiTroHeThong.DOCTOR, policy.permissionsFor(VaiTroHeThong.DOCTOR), -1));

        manager.logout();
        reject(AccessDeniedException.class, () -> new TongQuanVaiTroService(manager, provider).load());
        check(attempts.get() == 1, "Logged-out session does not open SQL");
        DesktopSession admin = session(VaiTroHeThong.SYSTEM_ADMIN);
        check(new TongQuanVaiTroService(admin, provider).load().size() == 6,
                "System admin permitted");
        check(closes.get() == 2, "Connection closed after administrator read");

        DesktopSession bad = session(VaiTroHeThong.CLINIC_MANAGER);
        var badProvider = (com.quanlynhakhoa.service.taikhoan.DanhSachTaiKhoanService.ConnectionProvider)
                () -> fakeConnection(closes, queries, new String[][]{{"INVENTED_ROLE", "1"}});
        reject(SQLException.class, () -> new TongQuanVaiTroService(bad, badProvider).load());
        check(closes.get() == 3, "Connection closed after malformed SQL result");

        System.out.println("PASS: CORE-W2 roles overview (" + checks
                + " checks; synthetic JDBC, no DB)");
    }
}
