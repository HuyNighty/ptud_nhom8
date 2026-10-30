package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Deliberately small JDBC protocol double, not evidence of running SQL Server. */
public final class FeatureJdbcDouble implements ConnectionProvider {
    public final List<String> sql = new ArrayList<>();
    public final List<Map<Integer, Object>> bindings = new ArrayList<>();
    public final List<Map<Integer, Object>> persisted = new ArrayList<>();
    public final List<Map<Integer, Object>> audits = new ArrayList<>();
    public volatile int opens, commits, rollbacks, closes, updates;
    public volatile boolean auditFailure, rollbackFailure, closeFailure, duplicate, existing, onEdt;
    public int affected = 1;
    public int isolation;
    public final CountDownLatch opened = new CountDownLatch(1);
    public CountDownLatch release;
    public List<Map<String, Object>> invoiceRows = List.of(), lineRows = List.of(), paymentRows = List.of();

    @Override public Connection open() throws SQLException {
        opens++; onEdt |= SwingUtilities.isEventDispatchThread(); opened.countDown();
        if (release != null) {
            try { if (!release.await(10, TimeUnit.SECONDS)) throw new SQLException("Test timed out"); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new SQLException(failure); }
        }
        return proxy(Connection.class, new InvocationHandler() {
            boolean autoCommit = true;
            final List<Map<Integer, Object>> pending = new ArrayList<>(), pendingAudits = new ArrayList<>();
            public Object invoke(Object instance, Method method, Object[] args) throws Throwable {
                return switch (method.getName()) {
                    case "getAutoCommit" -> autoCommit;
                    case "setAutoCommit" -> { autoCommit = (boolean) args[0]; yield null; }
                    case "setTransactionIsolation" -> { isolation = (int) args[0]; yield null; }
                    case "prepareStatement" -> statement((String) args[0], pending, pendingAudits);
                    case "commit" -> { commits++; persisted.addAll(pending); audits.addAll(pendingAudits); pending.clear(); pendingAudits.clear(); yield null; }
                    case "rollback" -> { rollbacks++; pending.clear(); pendingAudits.clear(); if (rollbackFailure) throw new SQLException("rollback"); yield null; }
                    case "close" -> { closes++; if (closeFailure) throw new SQLException("close"); yield null; }
                    case "toString" -> "Synthetic JDBC connection";
                    default -> throw new AssertionError("Unexpected Connection call: " + method.getName());
                };
            }
        });
    }

    private PreparedStatement statement(final String query, final List<Map<Integer, Object>> pending,
                                         final List<Map<Integer, Object>> pendingAudits) {
        sql.add(query);
        final Map<Integer, Object> params = new HashMap<>(); bindings.add(params);
        return proxy(PreparedStatement.class, new InvocationHandler() {
            boolean timeout;
            public Object invoke(Object instance, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.equals("setQueryTimeout")) { timeout = (int) args[0] > 0; return null; }
                if (name.startsWith("set")) { params.put((int) args[0], name.equals("setNull") ? null : args[1]); return null; }
                if (name.equals("close")) return null;
                if (!timeout) throw new AssertionError("Query timeout missing");
                if (name.equals("executeUpdate")) {
                    updates++;
                    if (query.contains("INSERT INTO dbo.DichVu") && duplicate) throw new SQLException("duplicate", "23000", 2627);
                    if (query.contains("NhatKyThaoTac")) {
                        if (auditFailure) throw new SQLException("audit", "42000", 229);
                        pendingAudits.add(new HashMap<>(params));
                    } else pending.add(new HashMap<>(params));
                    return affected;
                }
                if (name.equals("executeQuery")) {
                    List<Map<String, Object>> values = List.of();
                    if (query.startsWith("WITH Lines")) values = invoiceRows;
                    else if (query.contains("FROM dbo.ChiTietHoaDon WHERE")) values = lineRows;
                    else if (query.contains("FROM dbo.ThanhToan WHERE")) values = paymentRows;
                    else if (query.startsWith("SELECT maDichVu FROM") && existing) values = List.of(Map.of("maDichVu", "TST1"));
                    return rows(values);
                }
                throw new AssertionError("Unexpected PreparedStatement call: " + name);
            }
        });
    }
    private static ResultSet rows(final List<Map<String, Object>> values) {
        return proxy(ResultSet.class, new InvocationHandler() {
            int index = -1;
            public Object invoke(Object instance, Method method, Object[] args) {
                String name = method.getName();
                if (name.equals("next")) return ++index < values.size();
                if (name.equals("close")) return null;
                if (name.startsWith("get")) {
                    Object value = values.get(index).get((String) args[0]);
                    if (value == null && name.equals("getInt")) return 0;
                    if (value == null && name.equals("getBoolean")) return false;
                    return value;
                }
                throw new AssertionError("Unexpected ResultSet call: " + name);
            }
        });
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
