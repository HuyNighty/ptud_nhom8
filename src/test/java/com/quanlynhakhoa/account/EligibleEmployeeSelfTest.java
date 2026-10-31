package com.quanlynhakhoa.account;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** No SQL Server needed. Verifies mapping, RBAC and a synthetic JDBC query. */
public final class EligibleEmployeeSelfTest {
    private static int checks;
    private static void check(boolean value, String note) {
        checks++;
        if (!value) throw new AssertionError("CORE-W3 check " + checks + ": " + note);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void reject(Class<? extends Throwable> expected, Action action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName());
        } catch (Throwable cause) {
            if (!expected.isInstance(cause)) throw new AssertionError("Wrong exception " + cause, cause);
            check(true, "Rejected " + expected.getSimpleName());
        }
    }
    private static DesktopSession user(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }

    private static Connection fakeConnection(List<String[]> data, AtomicInteger closed,
                                             AtomicBoolean queryVerified) {
        int[] index = {-1};
        ResultSet rs = (ResultSet) Proxy.newProxyInstance(EligibleEmployeeSelfTest.class.getClassLoader(),
                new Class<?>[]{ResultSet.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "next" -> ++index[0] < data.size();
                    case "getString", "getNString" -> {
                        String col = (String) args[0];
                        int column = switch (col) {
                            case "maNhanVien" -> 0;
                            case "hoTen" -> 1;
                            case "chucDanh" -> 2;
                            default -> throw new IllegalArgumentException("Unexpected column " + col);
                        };
                        yield data.get(index[0])[column];
                    }
                    case "close" -> null;
                    default -> throw new UnsupportedOperationException("ResultSet." + method.getName());
                });
        PreparedStatement stmt = (PreparedStatement) Proxy.newProxyInstance(
                EligibleEmployeeSelfTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setQueryTimeout" -> {
                        check((int) args[0] == 12, "Query timeout 12s");
                        yield null;
                    }
                    case "executeQuery" -> rs;
                    case "close" -> null;
                    default -> throw new UnsupportedOperationException("PreparedStatement." + method.getName());
                });
        return (Connection) Proxy.newProxyInstance(EligibleEmployeeSelfTest.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "prepareStatement" -> {
                        String sql = (String) args[0];
                        check(sql.contains("SELECT TOP (201)"), "Bounded query");
                        check(sql.contains("n.trangThai = 'DANG_LAM_VIEC'"), "Only active employees");
                        check(sql.contains("NOT EXISTS"), "No linked account");
                        check(sql.contains("a.maNhanVien = n.maNhanVien"), "Link employee account");
                        check(sql.contains("n.chucDanh IN ('LT', 'BS', 'YT', 'QL', 'KHO')"), "No AD employee");
                        check(!sql.contains("matKhauHash"), "No password hash query");
                        queryVerified.set(true);
                        yield stmt;
                    }
                    case "close" -> { closed.incrementAndGet(); yield null; }
                    default -> throw new UnsupportedOperationException("Connection." + method.getName());
                });
    }

    public static void main(String[] args) throws Exception {
        check(EligibleEmployee.roleForJob("LT") == Role.RECEPTIONIST, "Receptionist mapping");
        check(EligibleEmployee.roleForJob("BS") == Role.DOCTOR, "Doctor mapping");
        check(EligibleEmployee.roleForJob("YT") == Role.NURSE, "Nurse mapping");
        check(EligibleEmployee.roleForJob("QL") == Role.CLINIC_MANAGER, "Clinic manager mapping");
        check(EligibleEmployee.roleForJob("KHO") == Role.INVENTORY_MANAGER, "Inventory mapping");
        reject(IllegalArgumentException.class, () -> EligibleEmployee.roleForJob("AD"));
        reject(IllegalArgumentException.class, () -> EligibleEmployee.roleForJob("??"));
        reject(IllegalArgumentException.class, () -> EligibleEmployee.roleForJob(null));
        reject(IllegalArgumentException.class,
                () -> new EligibleEmployee("NV01", "Doctor", "BS", Role.RECEPTIONIST));
        reject(IllegalArgumentException.class,
                () -> new EligibleEmployee("bad id", "Doctor", "BS", Role.DOCTOR));
        reject(IllegalArgumentException.class,
                () -> new EligibleEmployee("NV01", " ", "BS", Role.DOCTOR));
        EligibleEmployee doctor = new EligibleEmployee("NV01", "Bác sĩ thử nghiệm", "BS", Role.DOCTOR);
        check(doctor.displayLabel().contains("NV01"), "Display includes ID");
        check(doctor.displayLabel().contains("Bác sĩ"), "Display includes name");

        var list = new ArrayList<EligibleEmployee>();
        list.add(doctor);
        EligibleEmployeeDao.Result snapshot = new EligibleEmployeeDao.Result(list, false);
        list.clear();
        check(snapshot.employees().size() == 1, "Defensive immutable copy");
        reject(UnsupportedOperationException.class, () -> snapshot.employees().clear());

        AtomicInteger opens = new AtomicInteger();
        EligibleEmployeeService.ConnectionProvider provider = () -> {
            opens.incrementAndGet();
            throw new SQLException("Synthetic: do not open SQL");
        };
        reject(AccessDeniedException.class,
                () -> new EligibleEmployeeService(user(Role.DOCTOR), provider).listEligible());
        reject(AccessDeniedException.class,
                () -> new EligibleEmployeeService(user(Role.RECEPTIONIST), provider).listEligible());
        reject(AccessDeniedException.class,
                () -> new EligibleEmployeeService(user(Role.NURSE), provider).listEligible());
        check(opens.get() == 0, "Denied roles never open connection");
        DesktopSession manager = user(Role.CLINIC_MANAGER);
        reject(SQLException.class, () -> new EligibleEmployeeService(manager, provider).listEligible());
        check(opens.get() == 1, "Manager permission reaches provider");
        manager.logout();
        reject(AccessDeniedException.class, () -> new EligibleEmployeeService(manager, provider).listEligible());
        check(opens.get() == 1, "Logged out cannot open connection");
        reject(SQLException.class, () -> new EligibleEmployeeService(user(Role.SYSTEM_ADMIN), provider).listEligible());
        check(opens.get() == 2, "Admin reaches provider");

        AtomicInteger closes = new AtomicInteger();
        AtomicBoolean verified = new AtomicBoolean();
        List<String[]> dbRows = List.of(
                new String[]{"TESTLT09", "Lễ tân kiểm thử", "LT"},
                new String[]{"TESTBS09", "Bác sĩ kiểm thử", "BS"});
        EligibleEmployeeService service = new EligibleEmployeeService(user(Role.SYSTEM_ADMIN),
                () -> fakeConnection(dbRows, closes, verified));
        EligibleEmployeeDao.Result found = service.listEligible();
        check(verified.get(), "SQL validated");
        check(closes.get() == 1, "Connection closed");
        check(found.employees().size() == 2, "Two eligible rows");
        check(found.employees().get(0).role() == Role.RECEPTIONIST, "Employee LT projected");
        check(found.employees().get(1).role() == Role.DOCTOR, "Employee BS projected");
        check(!found.truncated(), "Not truncated");

        AtomicInteger zeroClosed = new AtomicInteger();
        EligibleEmployeeDao.Result zero = new EligibleEmployeeService(user(Role.SYSTEM_ADMIN),
                () -> fakeConnection(List.of(), zeroClosed, new AtomicBoolean())).listEligible();
        check(zero.employees().isEmpty(), "Empty eligible list");
        check(zeroClosed.get() == 1, "Empty connection closed");

        var many = new ArrayList<String[]>();
        for (int i = 0; i < 201; i++) {
            many.add(new String[]{"NV" + String.format("%08d", i), "Name " + i, "BS"});
        }
        EligibleEmployeeDao.Result limited = new EligibleEmployeeService(user(Role.SYSTEM_ADMIN),
                () -> fakeConnection(many, new AtomicInteger(), new AtomicBoolean())).listEligible();
        check(limited.employees().size() == 200, "200 row limit");
        check(limited.truncated(), "Truncation signaled");
        System.out.println("PASS: CORE-W3 eligible employee picker (" + checks + " checks; synthetic JDBC, no DB)");
    }
}
