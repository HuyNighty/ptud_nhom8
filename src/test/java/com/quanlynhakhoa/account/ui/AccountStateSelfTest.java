package com.quanlynhakhoa.account.ui;

import com.quanlynhakhoa.account.AccountDirectoryService;
import com.quanlynhakhoa.account.AccountStateService;
import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/** Real account service/DAO and headless SwingWorker UI, backed only by synthetic JDBC. */
public final class AccountStateSelfTest {
    private static int checks;
    private static final LocalDateTime UTC_NOW = LocalDateTime.of(2026, 10, 30, 10, 0);
    private static final String TARGET = "TARGET";

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "ACTOR", role, false));
    }
    private static AccountStateService service(FakeJdbc jdbc, DesktopSession actor) {
        return new AccountStateService(actor, jdbc::open);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getSimpleName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static void atomicFailure(FakeJdbc jdbc, Action action, Class<? extends Throwable> type) throws Exception {
        Map<String, Object> before = new HashMap<>(jdbc.row);
        expect(type, action);
        check(jdbc.commits == 0 && jdbc.rollbacks == 1, "failure rolls back exactly once");
        check(jdbc.row.equals(before) && jdbc.audits == 0, "state and audit roll back together");
        jdbc.checkClosed();
    }

    public static void main(String[] args) throws Exception {
        serviceRules();
        transactionFailures();
        feedback();
        uiFlow();
        System.out.println("PASS: ACCOUNT STATE (" + checks + " checks; synthetic JDBC, headless Swing)");
    }

    private static void serviceRules() throws Exception {
        for (Role role : Role.values()) {
            FakeJdbc jdbc = new FakeJdbc();
            DesktopSession actor = session(role);
            AccountStateService service = service(jdbc, actor);
            if (role != Role.CLINIC_MANAGER && role != Role.SYSTEM_ADMIN) {
                expect(AccessDeniedException.class, () -> service.lock(TARGET, "Reason"));
                expect(AccessDeniedException.class, () -> service.unlock(TARGET));
                check(jdbc.opens == 0, "unauthorized role never opens JDBC");
            }
        }
        FakeJdbc noSql = new FakeJdbc();
        DesktopSession actor = session(Role.CLINIC_MANAGER);
        AccountStateService service = service(noSql, actor);
        for (String reason : new String[]{null, "", " ", "\t\n", "x".repeat(301)}) {
            expect(IllegalArgumentException.class, () -> service.lock(TARGET, reason));
        }
        for (String id : new String[]{null, "", " ", "x".repeat(11)}) {
            expect(IllegalArgumentException.class, () -> service.lock(id, "Reason"));
            expect(IllegalArgumentException.class, () -> service.unlock(id));
        }
        expect(IllegalArgumentException.class, () -> service.lock(" actor ", "Reason"));
        check(noSql.opens == 0, "validation and case-insensitive self-lock reject before JDBC");
        actor.logout();
        expect(AccessDeniedException.class, () -> service.lock(TARGET, "Reason"));
        expect(AccessDeniedException.class, () -> service.unlock(TARGET));
        check(noSql.opens == 0, "inactive session never opens JDBC");

        for (boolean unlocking : new boolean[]{false, true}) {
            FakeJdbc admin = new FakeJdbc();
            admin.row.put("vaiTro", "SYSTEM_ADMIN");
            if (unlocking) admin.adminLocked();
            AccountStateService blocked = service(admin, session(Role.SYSTEM_ADMIN));
            atomicFailure(admin, () -> { if (unlocking) blocked.unlock(TARGET); else blocked.lock(TARGET, "Reason"); },
                    IllegalArgumentException.class);
            check(admin.updates == 0, "SYSTEM_ADMIN target never updated");
        }
        for (String state : List.of("BI_KHOA", "VO_HIEU")) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.row.put("trangThai", state);
            atomicFailure(jdbc, () -> service(jdbc, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"),
                    IllegalArgumentException.class);
        }
        for (String state : List.of("HOAT_DONG", "VO_HIEU", "FAILED_LOGIN", "MISSING_SOURCE")) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.row.put("trangThai", state.equals("FAILED_LOGIN") || state.equals("MISSING_SOURCE") ? "BI_KHOA" : state);
            jdbc.row.put("nguonKhoa", state.equals("FAILED_LOGIN") ? "FAILED_LOGIN" : null);
            atomicFailure(jdbc, () -> service(jdbc, session(Role.CLINIC_MANAGER)).unlock(TARGET),
                    IllegalArgumentException.class);
            check(jdbc.updates == 0, "unlock rejects non-admin-lock state");
        }
        FakeJdbc absent = new FakeJdbc();
        absent.present = false;
        atomicFailure(absent, () -> service(absent, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"),
                IllegalArgumentException.class);
        FakeJdbc canonicalSelf = new FakeJdbc();
        canonicalSelf.row.put("maTaiKhoan", "ACTOR");
        atomicFailure(canonicalSelf, () -> service(canonicalSelf, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"),
                IllegalArgumentException.class);

        FakeJdbc lock = new FakeJdbc();
        DesktopSession manager = session(Role.CLINIC_MANAGER);
        service(lock, manager).lock(" TARGET ", "  Lý do hợp lệ  ");
        check(lock.row.get("trangThai").equals("BI_KHOA") && lock.row.get("nguonKhoa").equals("ADMIN_ACTION"),
                "active account becomes administrator locked");
        check(UTC_NOW.equals(lock.row.get("khoaLuc")) && lock.row.get("khoaDen") == null, "UTC indefinite lock");
        check("Lý do hợp lệ".equals(lock.row.get("lyDoKhoa")), "trimmed Unicode reason bound");
        check(lock.commits == 1 && lock.rollbacks == 0 && lock.audits == 1, "state and audit committed once");
        check(lock.auditBindings.get(1).equals("ACTOR") && lock.auditBindings.get(3).equals(TARGET), "audit actor and target");
        check(lock.auditBindings.get(2).equals("Khóa tài khoản") && lock.auditBindings.get(4).equals("Lý do hợp lệ"),
                "audit action and reason");
        check(manager.isActive(), "actor session is not revoked");
        lock.checkClosed();

        FakeJdbc unlock = new FakeJdbc();
        unlock.adminLocked();
        service(unlock, session(Role.SYSTEM_ADMIN)).unlock(TARGET);
        check("HOAT_DONG".equals(unlock.row.get("trangThai")), "administrator lock becomes active");
        check(Integer.valueOf(0).equals(unlock.row.get("soLanSai")), "failed attempts reset");
        for (String column : List.of("lanSaiDau", "khoaLuc", "khoaDen", "nguonKhoa", "lyDoKhoa")) {
            check(unlock.row.get(column) == null, "unlock clears " + column);
        }
        check(unlock.commits == 1 && unlock.audits == 1 && unlock.rollbacks == 0, "unlock with audit atomic");
        unlock.checkClosed();
        FakeJdbc boundary = new FakeJdbc();
        service(boundary, session(Role.CLINIC_MANAGER)).lock(TARGET, "x".repeat(300));
        check(((String) boundary.row.get("lyDoKhoa")).length() == 300, "NVARCHAR(300) boundary accepted");
    }

    private static void transactionFailures() throws Exception {
        for (boolean unlocking : new boolean[]{false, true}) {
            for (int step = 1; step <= 3; step++) {
                FakeJdbc jdbc = new FakeJdbc();
                if (unlocking) jdbc.adminLocked();
                jdbc.failAt = step;
                AccountStateService service = service(jdbc, session(Role.CLINIC_MANAGER));
                atomicFailure(jdbc, () -> { if (unlocking) service.unlock(TARGET); else service.lock(TARGET, "Reason"); },
                        SQLTimeoutException.class);
                check(jdbc.steps == step, "timeout stops subsequent SQL");
            }
            for (int step = 1; step <= 3; step++) {
                FakeJdbc jdbc = new FakeJdbc();
                if (unlocking) jdbc.adminLocked();
                DesktopSession actor = session(Role.CLINIC_MANAGER);
                int logoutAt = step;
                jdbc.afterStep = current -> { if (current == logoutAt) actor.logout(); };
                AccountStateService service = service(jdbc, actor);
                atomicFailure(jdbc, () -> { if (unlocking) service.unlock(TARGET); else service.lock(TARGET, "Reason"); },
                        AccessDeniedException.class);
                check(jdbc.steps == step, "logout checked after each blocking JDBC operation");
            }
        }
        for (int updateCount : new int[]{0, 2}) {
            FakeJdbc jdbc = new FakeJdbc();
            jdbc.updateCount = updateCount;
            atomicFailure(jdbc, () -> service(jdbc, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"), SQLException.class);
        }
        FakeJdbc audit = new FakeJdbc();
        audit.auditCount = 0;
        atomicFailure(audit, () -> service(audit, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"), SQLException.class);
        FakeJdbc commit = new FakeJdbc();
        commit.failCommit = true;
        atomicFailure(commit, () -> service(commit, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"), SQLException.class);
        FakeJdbc rollback = new FakeJdbc();
        rollback.failAt = 2;
        rollback.failRollback = true;
        SQLTimeoutException original = expect(SQLTimeoutException.class,
                () -> service(rollback, session(Role.CLINIC_MANAGER)).lock(TARGET, "Reason"));
        check(original.getSuppressed().length == 1, "rollback failure suppressed on original timeout");
        rollback.checkClosed();
    }

    private static void feedback() {
        check(AccountStateFeedback.forFailure(new SQLTimeoutException("secret SQL")).contains("phản hồi quá lâu"),
                "friendly timeout");
        check(AccountStateFeedback.forFailure(new AccessDeniedException()).contains("quyền"), "friendly permission/session error");
        check(!AccountStateFeedback.forFailure(new SQLException("secret SQL")).contains("secret"), "SQL details hidden");
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get(8, TimeUnit.SECONDS);
    }
    private static Object field(AccountPanel panel, String name) throws Exception {
        Field field = AccountPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(panel);
    }
    private static boolean busy(AccountPanel panel) {
        try { return (boolean) field(panel, "loading") || (boolean) field(panel, "changingState"); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!edt(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) throw new AssertionError("SwingWorker did not complete");
            Thread.sleep(10);
        }
    }
    private static JButton button(AccountPanel panel, String name) throws Exception { return (JButton) field(panel, name); }
    private static void apply(AccountPanel panel, boolean locking) throws Exception {
        edt(() -> {
            Method selected = AccountPanel.class.getDeclaredMethod("selectedAccount");
            selected.setAccessible(true);
            Method method = AccountPanel.class.getDeclaredMethod("applyStateChange",
                    com.quanlynhakhoa.account.AccountDirectoryRow.class, boolean.class, String.class);
            method.setAccessible(true);
            method.invoke(panel, selected.invoke(panel), locking, "UI reason");
            return null;
        });
    }
    private static AccountPanel panel(FakeJdbc jdbc, DesktopSession actor) throws Exception {
        AccountPanel panel = edt(() -> new AccountPanel(actor,
                new AccountDirectoryService(actor, jdbc::open), service(jdbc, actor)));
        await(() -> !busy(panel));
        edt(() -> { ((JTable) field(panel, "tblAccounts")).setRowSelectionInterval(0, 0); return null; });
        return panel;
    }
    private static void uiFlow() throws Exception {
        for (String ineligible : List.of("SYSTEM_ADMIN", "FAILED_LOGIN", "SELF", "VO_HIEU")) {
            FakeJdbc jdbc = new FakeJdbc();
            if (ineligible.equals("SYSTEM_ADMIN")) jdbc.row.put("vaiTro", "SYSTEM_ADMIN");
            if (ineligible.equals("FAILED_LOGIN")) { jdbc.adminLocked(); jdbc.row.put("nguonKhoa", "FAILED_LOGIN"); }
            if (ineligible.equals("SELF")) jdbc.row.put("maTaiKhoan", "ACTOR");
            if (ineligible.equals("VO_HIEU")) jdbc.row.put("trangThai", "VO_HIEU");
            AccountPanel panel = panel(jdbc, session(Role.CLINIC_MANAGER));
            check(edt(() -> !button(panel, "btnLock").isEnabled() && !button(panel, "btnUnlock").isEnabled()),
                    "UI blocks " + ineligible);
        }
        FakeJdbc jdbc = new FakeJdbc();
        DesktopSession actor = session(Role.CLINIC_MANAGER);
        AccountPanel panel = panel(jdbc, actor);
        check(edt(() -> button(panel, "btnLock").isEnabled() && !button(panel, "btnUnlock").isEnabled()), "UI active target eligibility");
        jdbc.failAt = 2;
        jdbc.updateStarted = new CountDownLatch(1);
        jdbc.releaseUpdate = new CountDownLatch(1);
        apply(panel, true);
        check(jdbc.updateStarted.await(8, TimeUnit.SECONDS), "state worker reached JDBC");
        check(edt(() -> !button(panel, "btnLock").isEnabled() && !button(panel, "btnCreate").isEnabled()
                && !button(panel, "btnRefresh").isEnabled()), "busy UI guarded; EDT responsive");
        int opens = jdbc.opens;
        apply(panel, true);
        check(jdbc.opens == opens, "double state submission rejected");
        jdbc.releaseUpdate.countDown();
        await(() -> !busy(panel));
        check(edt(() -> button(panel, "btnLock").isEnabled()), "timeout releases busy UI");
        check(edt(() -> ((JLabel) field(panel, "lblStatus")).getText().contains("phản hồi quá lâu")), "UI shows friendly timeout");
        jdbc.failAt = 0;
        jdbc.steps = 0;
        apply(panel, true);
        await(() -> !busy(panel));
        edt(() -> { ((JTable) field(panel, "tblAccounts")).setRowSelectionInterval(0, 0); return null; });
        check(edt(() -> !button(panel, "btnLock").isEnabled() && button(panel, "btnUnlock").isEnabled()), "successful lock reloads source/status");
        apply(panel, false);
        await(() -> !busy(panel));
        check("HOAT_DONG".equals(jdbc.row.get("trangThai")) && jdbc.audits == 2, "UI unlock completes with audit");
        check(!jdbc.usedEdt, "all UI database calls stay off EDT");
        jdbc.checkClosed();
    }

    private static final class Capture {
        final String sql;
        final Map<Integer, Object> bindings = new HashMap<>();
        int timeout;
        boolean closed;
        Capture(String sql) { this.sql = sql; }
    }
    private static final class FakeJdbc {
        final Map<String, Object> row = new HashMap<>();
        final List<Capture> statements = new ArrayList<>();
        Map<String, Object> snapshot;
        Map<Integer, Object> auditBindings = Map.of();
        int opens, closes, commits, rollbacks, audits, snapshotAudits, steps, updates, resultSets, closedResultSets;
        int failAt, updateCount = 1, auditCount = 1;
        boolean present = true, failCommit, failRollback, usedEdt;
        IntConsumer afterStep = step -> { };
        CountDownLatch updateStarted, releaseUpdate;
        FakeJdbc() {
            row.put("maTaiKhoan", TARGET);
            row.put("vaiTro", "RECEPTIONIST");
            row.put("trangThai", "HOAT_DONG");
            row.put("nguonKhoa", null);
            row.put("lyDoKhoa", null);
            row.put("khoaLuc", null);
            row.put("khoaDen", null);
            row.put("soLanSai", 3);
            row.put("lanSaiDau", UTC_NOW.minusMinutes(1));
        }
        void adminLocked() {
            row.put("trangThai", "BI_KHOA");
            row.put("nguonKhoa", "ADMIN_ACTION");
            row.put("khoaLuc", UTC_NOW.minusHours(1));
            row.put("lyDoKhoa", "Existing reason");
        }
        Connection open() {
            usedEdt |= SwingUtilities.isEventDispatchThread();
            opens++;
            boolean[] auto = {true};
            return proxy(Connection.class, (p, method, args) -> {
                return switch (method.getName()) {
                    case "getAutoCommit" -> auto[0];
                    case "setTransactionIsolation" -> {
                        check((int) args[0] == Connection.TRANSACTION_READ_COMMITTED, "read committed transaction");
                        yield null;
                    }
                    case "setAutoCommit" -> {
                        auto[0] = (boolean) args[0];
                        snapshot = new HashMap<>(row);
                        snapshotAudits = audits;
                        yield null;
                    }
                    case "prepareStatement" -> statement((String) args[0], auto);
                    case "commit" -> {
                        if (failCommit) throw new SQLException("Synthetic commit failure");
                        check(audits == snapshotAudits + 1, "audit before commit");
                        commits++;
                        yield null;
                    }
                    case "rollback" -> {
                        rollbacks++;
                        if (failRollback) throw new SQLException("Synthetic rollback failure");
                        row.clear(); row.putAll(snapshot); audits = snapshotAudits;
                        yield null;
                    }
                    case "close" -> { closes++; yield null; }
                    default -> throw new AssertionError("Unexpected Connection." + method.getName());
                };
            });
        }
        PreparedStatement statement(String sql, boolean[] auto) {
            Capture capture = new Capture(sql);
            statements.add(capture);
            return proxy(PreparedStatement.class, (p, method, args) -> {
                String name = method.getName();
                if (name.equals("close")) { capture.closed = true; return null; }
                if (name.equals("setQueryTimeout")) { capture.timeout = (int) args[0]; return null; }
                if (name.startsWith("set")) { capture.bindings.put((int) args[0], args[1]); return null; }
                check(capture.timeout > 0 && capture.timeout <= 15, "statement timeout set before execution");
                boolean directory = sql.contains("TOP (201)");
                if (!directory) {
                    check(!auto[0], "target read, update and audit inside one transaction");
                    steps++;
                    if (name.equals("executeUpdate") && sql.contains("UPDATE dbo.TaiKhoan") && releaseUpdate != null) {
                        updateStarted.countDown();
                        if (!releaseUpdate.await(8, TimeUnit.SECONDS)) throw new AssertionError("UI update gate timeout");
                    }
                    if (steps == failAt) throw new SQLTimeoutException("secret SQL timeout", "HYT00");
                }
                if (name.equals("executeQuery")) {
                    if (!directory) check(sql.contains("WITH (UPDLOCK, HOLDLOCK)"), "target row locks retained through transaction");
                    afterStep.accept(directory ? 0 : steps);
                    return resultSet();
                }
                if (!name.equals("executeUpdate")) throw new AssertionError("Unexpected PreparedStatement." + name);
                if (sql.contains("UPDATE dbo.TaiKhoan")) {
                    updates++;
                    String assignments = sql.substring(sql.indexOf("SET ") + 4, sql.indexOf("WHERE "));
                    var matcher = Pattern.compile("(\\w+)\\s*=").matcher(assignments);
                    Set<String> allowed = Set.of("trangThai", "soLanSai", "lanSaiDau", "khoaLuc", "khoaDen",
                            "nguonKhoa", "lyDoKhoa", "lanDangNhapCuoi", "capNhatLuc");
                    while (matcher.find()) check(allowed.contains(matcher.group(1)), "uses only permitted UPDATE columns");
                    check(!sql.contains("matKhau") && !sql.contains("voHieuLuc ="), "no password or disabled metadata writes");
                    check(sql.contains("vaiTro <> 'SYSTEM_ADMIN'"), "SQL defends SYSTEM_ADMIN targets");
                    check(sql.contains("capNhatLuc = SYSUTCDATETIME()"), "UTC modification time");
                    if (sql.contains("SET trangThai = 'BI_KHOA'")) {
                        check(sql.contains("khoaLuc = SYSUTCDATETIME()") && sql.contains("khoaDen = NULL"), "UTC indefinite admin lock SQL");
                        check(sql.contains("AND trangThai = 'HOAT_DONG'") && sql.contains("AND maTaiKhoan <> ?"), "SQL lock transition and self guard");
                        row.put("trangThai", "BI_KHOA"); row.put("nguonKhoa", "ADMIN_ACTION");
                        row.put("khoaLuc", UTC_NOW); row.put("khoaDen", null); row.put("lyDoKhoa", capture.bindings.get(1));
                        check(capture.bindings.get(2).equals(row.get("maTaiKhoan")) && capture.bindings.get(3).equals("ACTOR"), "lock IDs bound");
                    } else {
                        check(sql.contains("AND trangThai = 'BI_KHOA'") && sql.contains("AND nguonKhoa = 'ADMIN_ACTION'"), "SQL unlock transition guard");
                        for (String column : List.of("lanSaiDau", "khoaLuc", "khoaDen", "nguonKhoa", "lyDoKhoa")) {
                            check(sql.contains(column + " = NULL"), "SQL clears " + column);
                            row.put(column, null);
                        }
                        check(sql.contains("soLanSai = 0"), "SQL resets failed count");
                        row.put("soLanSai", 0); row.put("trangThai", "HOAT_DONG");
                        check(capture.bindings.get(1).equals(row.get("maTaiKhoan")), "unlock ID bound");
                    }
                    afterStep.accept(steps);
                    return updateCount;
                }
                check(sql.contains("INSERT INTO dbo.NhatKyThaoTac"), "audit insert");
                check(sql.contains("'ACCOUNT'") && sql.contains("'SUCCESS'"), "valid audit actor/result");
                audits++;
                auditBindings = Map.copyOf(capture.bindings);
                afterStep.accept(steps);
                return auditCount;
            });
        }
        ResultSet resultSet() {
            resultSets++;
            boolean[] served = {false};
            return proxy(ResultSet.class, (p, method, args) -> {
                return switch (method.getName()) {
                    case "next" -> { boolean next = present && !served[0]; served[0] = true; yield next; }
                    case "getString" -> {
                        String column = (String) args[0];
                        yield switch (column) {
                            case "tenDangNhap" -> "synthetic";
                            case "tenHienThi", "tenNhanVien" -> "Synthetic";
                            case "maNhanVien" -> "NV001";
                            default -> row.get(column);
                        };
                    }
                    case "getTimestamp" -> null;
                    case "getBoolean" -> false;
                    case "close" -> { closedResultSets++; yield null; }
                    default -> throw new AssertionError("Unexpected ResultSet." + method.getName());
                };
            });
        }
        void checkClosed() {
            check(opens == closes, "all connections closed");
            check(resultSets == closedResultSets, "all result sets closed");
            check(statements.stream().allMatch(s -> s.closed), "all statements closed");
        }
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
