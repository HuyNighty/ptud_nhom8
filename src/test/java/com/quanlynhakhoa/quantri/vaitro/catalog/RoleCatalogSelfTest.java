package com.quanlynhakhoa.quantri.vaitro.catalog;

import com.quanlynhakhoa.entity.VaiTro;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Synthetic JDBC contract tests only: no driver/connection/database opened. */
public final class RoleCatalogSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> type, Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "Unexpected exception: " + failure);
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static VaiTro role(String code, String name, String description, boolean active,
                                   boolean system, boolean protectedRole, byte[] version) {
        return new VaiTro(code, name, description, active, system, protectedRole, version);
    }
    private static Map<String, Object> row(String code, boolean active) {
        Map<String, Object> row = new HashMap<>();
        row.put("roleCode", code); row.put("roleName", "Vai trò thử nghiệm");
        row.put("description", ""); row.put("enabled", active); row.put("isSystem", false);
        row.put("isProtected", false); row.put("revision", new byte[8]);
        return row;
    }
    private static final class FakeJdbc {
        final List<Map<String, Object>> rows;
        String sql, bound;
        int statements, statementCloses, resultCloses, timeout;
        boolean failExecute;
        FakeJdbc(List<Map<String, Object>> rows) { this.rows = rows; }
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (p, m, a) -> {
                        if (!m.getName().equals("prepareStatement"))
                            throw new AssertionError("Caller-owned connection touched: " + m.getName());
                        sql = (String) a[0]; statements++;
                        check(sql.startsWith("SELECT "), "SELECT-only");
                        return Proxy.newProxyInstance(getClass().getClassLoader(),
                                new Class<?>[]{PreparedStatement.class}, (sp, sm, sa) -> {
                                    return switch (sm.getName()) {
                                        case "setQueryTimeout" -> { timeout = (int) sa[0]; yield null; }
                                        case "setString" -> {
                                            check(sa[0].equals(1), "Code parameter index");
                                            bound = (String) sa[1]; yield null;
                                        }
                                        case "close" -> { statementCloses++; yield null; }
                                        case "executeQuery" -> {
                                            if (failExecute) throw new SQLException("Synthetic failure");
                                            yield results();
                                        }
                                        default -> throw new AssertionError(sm.getName());
                                    };
                                });
                    });
        }
        ResultSet results() {
            int[] index = {-1};
            boolean[] wasNull = {false};
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, (p, m, a) -> {
                        if (m.getName().equals("next")) return ++index[0] < rows.size();
                        if (m.getName().equals("close")) { resultCloses++; return null; }
                        if (m.getName().equals("wasNull")) return wasNull[0];
                        Object value = rows.get(index[0]).get((String) a[0]);
                        wasNull[0] = value == null;
                        return switch (m.getName()) {
                            case "getString", "getBytes" -> value;
                            case "getBoolean" -> value != null && (Boolean) value;
                            default -> throw new AssertionError(m.getName());
                        };
                    });
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] revision = new byte[8];
        VaiTro a = role("NEW_ROLE", "Tên tiếng Việt", "", true, false, false, revision);
        revision[0] = 9;
        check(a.revision()[0] == 0, "Constructor defensive copy");
        byte[] returned = a.revision(); returned[0] = 8;
        check(a.revision()[0] == 0, "Accessor defensive copy");
        VaiTro same = role("NEW_ROLE", "Tên khác", "Khác", false, false, false, new byte[8]);
        check(a.equals(same) && a.hashCode() == same.hashCode(), "Identity by exact code");
        check(!a.equals(null) && !a.equals("NEW_ROLE"), "Typed identity");
        check(a.displayName().equals("Tên tiếng Việt") && a.active() && !a.system(), "Fields");
        check(!a.equals(role("OTHER", "Khác", "", true, false, false, new byte[8])), "Different code");
        for (String code : new String[]{"A", "A" + "0".repeat(31), "CUSTOM_123"})
            VaiTro.requireCode(code);
        for (String code : new String[]{null, "", "lower", "1BAD", " ROLE", "ROLE ", "R\t", "R-X", "Á", "A".repeat(33)})
            rejects(IllegalArgumentException.class, () -> VaiTro.requireCode(code));
        for (String name : new String[]{null, "", " ", "N".repeat(101)})
            rejects(IllegalArgumentException.class, () -> role("CUSTOM", name, "", true, false, false, new byte[8]));
        for (String description : new String[]{null, "D".repeat(501)})
            rejects(IllegalArgumentException.class, () -> role("CUSTOM", "Tên", description, true, false, false, new byte[8]));
        for (byte[] invalid : new byte[][]{null, new byte[0], new byte[7], new byte[9]})
            rejects(IllegalArgumentException.class, () -> role("CUSTOM", "Tên", "", true, false, false, invalid));
        for (String code : new String[]{"RECEPTIONIST","DOCTOR","NURSE","CLINIC_MANAGER","INVENTORY_MANAGER","SYSTEM_ADMIN"}) {
            VaiTro legacy = role(code, "Legacy", "", true, true, code.equals("SYSTEM_ADMIN"), new byte[8]);
            check(legacy.system(), "Legacy system metadata");
            rejects(IllegalArgumentException.class, () -> role(code, "Legacy", "", true, false, false, new byte[8]));
        }
        rejects(IllegalArgumentException.class, () -> role("SYSTEM_ADMIN", "Admin", "", false, true, true, new byte[8]));
        rejects(IllegalArgumentException.class, () -> role("SYSTEM_ADMIN", "Admin", "", true, true, false, new byte[8]));
        rejects(IllegalArgumentException.class, () -> role("CUSTOM", "Admin", "", true, true, false, new byte[8]));
        rejects(IllegalArgumentException.class, () -> role("CUSTOM", "Admin", "", true, false, true, new byte[8]));

        RoleRepository repository = new JdbcRoleRepository();
        FakeJdbc found = new FakeJdbc(List.of(row("CUSTOM", true)));
        check(repository.findByCode(found.connection(), "CUSTOM").orElseThrow().code().equals("CUSTOM"), "Custom mapping");
        check(found.bound.equals("CUSTOM") && found.sql.contains("WHERE roleCode = ?"), "Bind code, no interpolation");
        check(found.timeout == 10 && found.statementCloses == 1 && found.resultCloses == 1, "Timeout/resources");
        FakeJdbc empty = new FakeJdbc(List.of());
        check(repository.findByCode(empty.connection(), "MISSING").isEmpty(), "Missing is empty, no fallback");
        FakeJdbc all = new FakeJdbc(List.of(row("A", true), row("B", false)));
        List<VaiTro> list = repository.findAll(all.connection());
        check(list.size() == 2 && !list.get(1).active() && all.sql.endsWith("ORDER BY roleCode"), "All includes inactive");
        rejects(UnsupportedOperationException.class, () -> list.add(a));
        check(repository.findAll(new FakeJdbc(List.of()).connection()).isEmpty(), "Empty catalog");
        FakeJdbc untouched = new FakeJdbc(List.of());
        rejects(IllegalArgumentException.class, () -> repository.findByCode(untouched.connection(), "bad"));
        check(untouched.statements == 0, "Reject invalid code before I/O");
        rejects(NullPointerException.class, () -> repository.findAll(null));
        for (String column : new String[]{"roleCode","roleName","description","enabled","isSystem","isProtected","revision"}) {
            Map<String, Object> invalid = row("CUSTOM", true); invalid.put(column, null);
            FakeJdbc bad = new FakeJdbc(List.of(invalid));
            rejects(SQLException.class, () -> repository.findAll(bad.connection()));
            check(bad.statementCloses == 1 && bad.resultCloses == 1, "Invalid row cleanup");
        }
        for (Map<String, Object> invalid : List.of(row("bad", true), row("SYSTEM_ADMIN", true))) {
            rejects(SQLException.class, () -> repository.findAll(new FakeJdbc(List.of(invalid)).connection()));
        }
        FakeJdbc duplicate = new FakeJdbc(List.of(row("CUSTOM", true), row("CUSTOM", true)));
        rejects(SQLException.class, () -> repository.findAll(duplicate.connection()));
        rejects(SQLException.class, () -> repository.findByCode(new FakeJdbc(List.of(row("OTHER", true))).connection(), "CUSTOM"));
        rejects(SQLException.class, () -> repository.findByCode(duplicate.connection(), "CUSTOM"));
        FakeJdbc failure = new FakeJdbc(List.of()); failure.failExecute = true;
        rejects(SQLException.class, () -> repository.findAll(failure.connection()));
        check(failure.statementCloses == 1, "SQL error cleanup, no fabricated empty result");
        FakeJdbc edt = new FakeJdbc(List.of());
        SwingUtilities.invokeAndWait(() -> rejects(SQLException.class, () -> repository.findAll(edt.connection())));
        check(edt.statements == 0, "No query on EDT");
        System.out.println("PASS: Role catalog foundation (" + checks + " checks; synthetic JDBC, no SQL Server).");
    }
}

