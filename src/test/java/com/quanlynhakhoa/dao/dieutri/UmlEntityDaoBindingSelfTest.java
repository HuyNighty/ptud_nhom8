package com.quanlynhakhoa.dao.dieutri;

import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Typed treatment entities must preserve existing SQL bindings, guards and transaction ownership. */
public final class UmlEntityDaoBindingSelfTest {
    private static int checks;
    private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    private static final class Call {
        final String sql;
        final Map<Integer,Object> values = new LinkedHashMap<>();
        boolean closed;
        Call(String sql) { this.sql = sql; }
    }
    private static final class Probe implements InvocationHandler {
        final List<Call> calls = new ArrayList<>();
        boolean rejectVisit, rejectDetail;
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, this);
        }
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (Set.of("commit", "rollback", "setAutoCommit", "setTransactionIsolation").contains(method.getName()))
                throw new AssertionError("DAO took ownership of the caller transaction");
            if (!method.getName().equals("prepareStatement")) throw new AssertionError(method.getName());
            Call call = new Call((String)args[0]); calls.add(call);
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    new InvocationHandler() {
                        public Object invoke(Object p, Method m, Object[] a) {
                            String name = m.getName();
                            if (Set.of("setString", "setNString", "setDate", "setInt").contains(name)) {
                                call.values.put((Integer)a[0], a[1]); return null;
                            }
                            if (name.equals("setQueryTimeout")) return null;
                            if (name.equals("close")) { call.closed = true; return null; }
                            if (name.equals("executeUpdate"))
                                return rejectDetail && call.sql.startsWith("INSERT INTO dbo.ChiTietDieuTri") ? 0 : 1;
                            if (name.equals("executeQuery")) return result(call);
                            throw new AssertionError(name);
                        }
                    });
        }
        Object result(Call call) {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    new InvocationHandler() {
                        int cursor;
                        public Object invoke(Object p, Method m, Object[] args) {
                            return switch (m.getName()) {
                                case "next" -> cursor++ == 0 && !call.sql.startsWith("SELECT maHoaDon")
                                        && !(rejectVisit && call.sql.startsWith("SELECT 1 FROM dbo.PhieuKham"));
                                case "getString" -> "QA_NV";
                                case "close" -> null;
                                default -> throw new AssertionError(m.getName());
                            };
                        }
                    });
        }
        Call find(String prefix) {
            for (Call c : calls) if (c.sql.startsWith(prefix)) return c;
            throw new AssertionError("Missing expected statement " + prefix);
        }
        boolean has(String prefix) {
            for (Call c : calls) if (c.sql.startsWith(prefix)) return true;
            return false;
        }
    }
    public static void main(String[] args) throws Exception {
        KeHoachDieuTriDao dao = new KeHoachDieuTriDao();
        LocalDate date = LocalDate.of(2026,11,10);
        Probe plan = new Probe();
        dao.taoKeHoach(plan.connection(), "QA_TK", "QA_KH", "QA_HS", "QA_PK", date, "Mục tiêu thử");
        Call inserted = plan.find("INSERT INTO dbo.KeHoachDieuTri");
        check(new ArrayList<>(inserted.values.values()).equals(List.of("QA_KH", "QA_HS", "QA_PK",
                java.sql.Date.valueOf(date), "Mục tiêu thử", "QA_NV")), "Plan binds retain their original order and JDBC types");
        Call ownership = plan.find("SELECT 1 FROM dbo.PhieuKham");
        check(ownership.values.equals(Map.of(1,"QA_PK",2,"QA_HS")), "Visit/chart ownership predicate retained");
        check(ownership.sql.contains("UPDLOCK,HOLDLOCK"), "Existing ownership lock retained");
        check(plan.find("SELECT maHoaDon").sql.contains("UPDLOCK,HOLDLOCK"), "Existing billing lock retained");
        check(plan.calls.indexOf(ownership) < plan.calls.indexOf(inserted), "Ownership checked before insert");
        check(plan.calls.indexOf(plan.find("SELECT maHoaDon")) < plan.calls.indexOf(inserted), "Billing finalization checked before insert");
        Call audit = plan.find("INSERT INTO dbo.NhatKyThaoTac");
        check(audit.values.get(1).equals("QA_TK") && audit.values.get(4).equals("QA_KH"), "Audit actor and target retained");
        for (Call c : plan.calls) check(c.closed, "Every statement is closed");

        Probe detail = new Probe();
        dao.themChiTiet(detail.connection(), "QA_TK", "QA_KH", "QA_DV", "R16", date, null);
        Call line = detail.find("INSERT INTO dbo.ChiTietDieuTri");
        check(line.values.size() == 7 && line.values.get(1).equals("QA_KH") && line.values.get(2).equals("QA_DV"), "Detail foreign keys retained");
        check(line.values.get(3).equals("R16") && line.values.get(4).equals(java.sql.Date.valueOf(date)), "Tooth and planned-date bindings retained");
        check(line.values.containsKey(5) && line.values.get(5) == null, "Nullable note remains SQL NULL");
        check(line.values.get(6).equals("QA_KH") && line.values.get(7).equals("QA_DV"), "Guard predicate bindings retained");
        check(detail.has("INSERT INTO dbo.NhatKyThaoTac"), "Successful detail insert is audited");

        Probe denied = new Probe(); denied.rejectVisit = true;
        try {
            dao.taoKeHoach(denied.connection(), "QA_TK", "QA_KH", "QA_HS", "QA_PK", date, "Mục tiêu");
            throw new AssertionError("Invalid visit ownership accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        check(!denied.has("INSERT INTO"), "Denied ownership must prevent all writes");
        Probe stale = new Probe(); stale.rejectDetail = true;
        try {
            dao.themChiTiet(stale.connection(), "QA_TK", "QA_KH", "QA_DV", "R16", date, "Ghi chú");
            throw new AssertionError("Failed plan/service predicate accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        check(!stale.has("INSERT INTO dbo.NhatKyThaoTac"), "Rejected detail does not produce success audit");
        System.out.println("PASS UmlEntityDaoBindingSelfTest: " + checks + " binding, ownership, locking and audit checks; no database.");
    }
}
