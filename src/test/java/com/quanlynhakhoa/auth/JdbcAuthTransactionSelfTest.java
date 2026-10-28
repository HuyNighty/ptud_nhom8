package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.LockSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Synthetic JDBC proxies; never loads a JDBC driver or connects to SQL Server. */
public final class JdbcAuthTransactionSelfTest {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDateTime UTC_NOW = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final LoginPolicy POLICY = new LoginPolicy((raw, hash) -> "correct".equals(raw));

    private static void check(boolean good, String message) {
        checks++;
        if (!good) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        check(JdbcAuthService.LOCK_ACCOUNT_SQL.contains("WITH (UPDLOCK, HOLDLOCK)"), "account locked");
        check(JdbcAuthService.LOCK_EMPLOYEE_SQL.contains("WITH (UPDLOCK, HOLDLOCK)"), "employee locked");
        check(JdbcAuthService.UPDATE_ACCOUNT_SQL.contains("WHERE maTaiKhoan = ?"), "update targets one id");

        FakeJdbc success = new FakeJdbc();
        AuthDecision ok = service(success).authenticate("synthetic-admin", "correct");
        check(ok.authenticated() && ok.role() != null && !ok.forcePasswordChange(), "valid login");
        check(success.commits == 1 && success.rollbacks == 0, "success commit");
        check(success.updates == 1, "success update");
        check(success.events.indexOf("tx-start") < success.events.indexOf("account-lock"), "lock inside tx");
        check(success.events.indexOf("account-lock") < success.events.indexOf("account-read"), "read after lock");
        check(success.events.indexOf("account-update") < success.events.indexOf("commit"), "commit after write");
        check("HOAT_DONG".equals(success.bound.get(1)), "status bound");
        check(Integer.valueOf(0).equals(success.bound.get(2)), "reset count");
        check(success.bound.get(3) == null && success.bound.get(6) == null, "clear lock metadata");
        check(Timestamp.valueOf(UTC_NOW).equals(success.bound.get(8)), "UTC last login");
        check("T0001".equals(success.bound.get(10)), "update account id");

        FakeJdbc failed = new FakeJdbc();
        AuthDecision bad = service(failed).authenticate("synthetic-admin", "bad");
        check(!bad.authenticated(), "bad password rejected");
        check(failed.commits == 1 && failed.updates == 1, "failed attempt persisted");
        check(Integer.valueOf(1).equals(failed.bound.get(2)), "failure count updated");
        check(failed.bound.get(3) instanceof Timestamp, "first failure timestamp");
        check(failed.bound.get(4) == null && failed.bound.get(6) == null, "no early lock");

        FakeJdbc fifth = new FakeJdbc();
        fifth.failedAttempts = 4;
        fifth.firstFailureAt = UTC_NOW.minusMinutes(4);
        AuthDecision locked = service(fifth).authenticate("synthetic-admin", "bad");
        check(!locked.authenticated(), "fifth rejected");
        check("BI_KHOA".equals(fifth.bound.get(1)), "fifth locks account");
        check(Integer.valueOf(5).equals(fifth.bound.get(2)), "five failures");
        check("FAILED_LOGIN".equals(fifth.bound.get(6)), "lock source");
        check(Timestamp.valueOf(UTC_NOW.plusMinutes(15)).equals(fifth.bound.get(5)), "lock time");

        FakeJdbc unknown = new FakeJdbc();
        unknown.present = false;
        check(!service(unknown).authenticate("unknown", "correct").authenticated(), "unknown rejects");
        check(unknown.commits == 1 && unknown.updates == 0, "unknown no write");

        FakeJdbc disabled = new FakeJdbc();
        disabled.status = "VO_HIEU";
        check(!service(disabled).authenticate("synthetic-admin", "correct").authenticated(), "disabled rejects");
        check(disabled.commits == 1 && disabled.updates == 0, "disabled no update");

        FakeJdbc lockedAdmin = new FakeJdbc();
        lockedAdmin.status = "BI_KHOA";
        lockedAdmin.lockSource = "ADMIN_ACTION";
        check(!service(lockedAdmin).authenticate("synthetic-admin", "correct").authenticated(), "admin lock refuses");
        check(lockedAdmin.updates == 0, "admin lock unchanged");

        FakeJdbc stale = new FakeJdbc();
        stale.updateCount = 0;
        boolean threw = false;
        try { service(stale).authenticate("synthetic-admin", "correct"); }
        catch (SQLException expected) { threw = true; }
        check(threw && stale.rollbacks == 1 && stale.commits == 0, "zero row update rollback");

        FakeJdbc blank = new FakeJdbc();
        check(!service(blank).authenticate(" ", "correct").authenticated(), "blank rejects");
        check(blank.opens == 0, "blank never accesses database");

        check(AccountStatus.HOAT_DONG != null && LockSource.FAILED_LOGIN != null,
                "compile-time enum linkage");
        System.out.println("PASS: R3-A2B TRANSACTION SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("FAKE JDBC ONLY; NO REAL DB CONNECTION OR WRITES");
    }

    private static JdbcAuthService service(FakeJdbc fake) {
        return new JdbcAuthService(fake::open, POLICY, CLOCK);
    }

    private static final class FakeJdbc {
        boolean present = true;
        String status = "HOAT_DONG";
        String lockSource = null;
        int failedAttempts = 0;
        LocalDateTime firstFailureAt;
        int updateCount = 1;
        int commits;
        int rollbacks;
        int updates;
        int opens;
        final List<String> events = new ArrayList<>();
        Map<Integer,Object> bound = new HashMap<>();

        Connection open() {
            opens++;
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getAutoCommit": return true;
                    case "setTransactionIsolation": return null;
                    case "setAutoCommit": events.add("tx-start"); return null;
                    case "commit": commits++; events.add("commit"); return null;
                    case "rollback": rollbacks++; events.add("rollback"); return null;
                    case "close": return null;
                    case "prepareStatement": return statement((String) args[0]);
                    case "isClosed": return false;
                    case "toString": return "SyntheticConnection";
                    default: throw new UnsupportedOperationException("Connection." + method.getName());
                }
            };
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class[]{Connection.class}, handler);
        }

        PreparedStatement statement(String sql) {
            Map<Integer,Object> bindings = new HashMap<>();
            InvocationHandler h = (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("setQueryTimeout") || name.equals("close")) return null;
                if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                    bindings.put((Integer) args[0], name.equals("setNull") ? null : args[1]);
                    return null;
                }
                if (name.equals("executeQuery")) {
                    if (sql.contains("WITH (UPDLOCK, HOLDLOCK)")) {
                        events.add(sql.contains("dbo.TaiKhoan") ? "account-lock" : "employee-lock");
                        return resultSet(sql.contains("dbo.TaiKhoan") && present, true);
                    }
                    if (sql.contains("FROM dbo.TaiKhoan")) {
                        events.add("account-read");
                        return resultSet(present, false);
                    }
                    throw new AssertionError("Unexpected SELECT");
                }
                if (name.equals("executeUpdate")) {
                    updates++;
                    bound = new HashMap<>(bindings);
                    events.add("account-update");
                    return updateCount;
                }
                if (name.equals("toString")) return "SyntheticPreparedStatement";
                throw new UnsupportedOperationException("PreparedStatement." + name);
            };
            return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                    new Class[]{PreparedStatement.class}, h);
        }

        ResultSet resultSet(boolean hasRow, boolean lockOnly) {
            final boolean[] served = {false};
            InvocationHandler h = (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("close")) return null;
                if (name.equals("next")) {
                    boolean next = hasRow && !served[0];
                    served[0] = true;
                    return next;
                }
                if (name.equals("getString")) {
                    String col = (String) args[0];
                    if (col.equals("maTaiKhoan")) return "T0001";
                    if (col.equals("tenDangNhap")) return "synthetic-admin";
                    if (col.equals("tenHienThi")) return "Synthetic";
                    if (col.equals("matKhauHash")) return "synthetic-hash";
                    if (col.equals("vaiTro")) return "SYSTEM_ADMIN";
                    if (col.equals("trangThai")) return status;
                    if (col.equals("nguonKhoa")) return lockSource;
                    if (col.equals("lyDoKhoa")) return lockSource == null ? null : "Synthetic admin lock";
                    return null;
                }
                if (name.equals("getInt")) return failedAttempts;
                if (name.equals("getBoolean")) return false;
                if (name.equals("getTimestamp")) {
                    String col = (String) args[0];
                    if (col.equals("lanSaiDau"))
                        return firstFailureAt == null ? null : Timestamp.valueOf(firstFailureAt);
                    if (col.equals("khoaLuc") && lockSource != null)
                        return Timestamp.valueOf(UTC_NOW.minusMinutes(5));
                    if (col.equals("matKhauCapNhatLuc") || col.equals("taoLuc") || col.equals("capNhatLuc"))
                        return Timestamp.valueOf(UTC_NOW.minusDays(1));
                    return null;
                }
                if (name.equals("toString")) return "SyntheticResultSet";
                throw new UnsupportedOperationException("ResultSet." + name);
            };
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
                    new Class[]{ResultSet.class}, h);
        }
    }
}
