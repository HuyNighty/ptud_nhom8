package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Fault injection at the DAO boundary; no database or payment is created. */
public final class JdbcExtractionSelfTest {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    private static final class Jdbc implements ConnectionProvider {
        final List<String> events = new ArrayList<>();
        final boolean mismatch, failClose;
        int opens;
        Jdbc(boolean mismatch, boolean failClose) { this.mismatch=mismatch; this.failClose=failClose; }
        @Override public Connection open() {
            opens++;
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            return switch (method.getName()) {
                                case "setTransactionIsolation" -> {
                                    check(args[0].equals(Connection.TRANSACTION_SERIALIZABLE), "original isolation");
                                    yield null;
                                }
                                case "setAutoCommit" -> { check(args[0].equals(false), "Service starts transaction"); yield null; }
                                case "prepareStatement" -> statement((String)args[0]);
                                case "commit", "rollback", "close" -> { events.add(method.getName()); yield null; }
                                default -> throw new AssertionError(method.getName());
                            };
                        }
                    });
        }
        private PreparedStatement statement(String sql) {
            boolean attempt = sql.contains("maTaiKhoan,soTien");
            check(sql.contains("HAS_PERMS_BY_NAME") || sql.contains("maHoaDon") || attempt, "expected lock/read only");
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            return switch(method.getName()) {
                                case "setQueryTimeout" -> { check(args[0].equals(15), "timeout retained"); yield null; }
                                case "setString" -> null;
                                case "executeQuery" -> rows(attempt);
                                case "close" -> { if(attempt)events.add("statement-close"); yield null; }
                                default -> throw new AssertionError("unexpected JDBC write: "+method.getName());
                            };
                        }
                    });
        }
        private ResultSet rows(boolean attempt) {
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    new InvocationHandler() {
                        int index;
                        public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
                            return switch(method.getName()) {
                                case "next" -> index++ == 0;
                                case "getInt" -> 1;
                                case "getString" -> switch((int)args[0]) {
                                    case 1 -> "H1";
                                    case 2 -> "TST_ACC";
                                    case 4 -> "TIEN_MAT";
                                    case 5 -> null;
                                    case 7 -> "CHO_XU_LY";
                                    default -> throw new AssertionError("string column");
                                };
                                case "getBigDecimal" -> new BigDecimal((int)args[0] == 6 ? "200" : mismatch ? "101" : "100");
                                case "close" -> {
                                    if(attempt) {
                                        events.add("rows-close");
                                        if(failClose)throw new SQLException("injected result close");
                                    }
                                    yield null;
                                }
                                default -> throw new AssertionError(method.getName());
                            };
                        }
                    });
        }
    }

    public static void main(String[] args) throws Exception {
        YeuCauThanhToan request = new YeuCauThanhToan("H1",new BigDecimal("100"),List.of(
                new PhanThanhToan("P1",new BigDecimal("100"),PhuongThucThanhToan.TIEN_MAT,null,new BigDecimal("200"))),false);
        for(boolean failClose:new boolean[]{false,true}) {
            Jdbc jdbc=new Jdbc(false,failClose);
            var session=ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST);
            try {
                new LanThanhToanService(session,jdbc).reserve(request);
                check(!failClose,"close failure must propagate");
            } catch(SQLException expected) {
                check(failClose && expected.getMessage().equals("injected result close"),"original close exception");
            } finally { session.logout(); }
            check(jdbc.events.indexOf("commit")>=0,"matched attempt committed once");
            check(jdbc.events.indexOf("commit")<jdbc.events.indexOf("rows-close"),"commit stays inside locked read scope");
            check(Collections.frequency(jdbc.events,"commit")==1,"single commit");
            check(jdbc.events.contains("rollback")==failClose,"original rollback path on close failure");
            check(jdbc.events.contains("statement-close") && jdbc.events.contains("close"),"all resources closed");
        }
        Jdbc mismatch=new Jdbc(true,false);
        var session=ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST);
        ServiceBillingSelfTest.expect(IllegalArgumentException.class,()->new LanThanhToanService(session,mismatch).reserve(request));
        check(!mismatch.events.contains("commit") && mismatch.events.contains("rollback"),"mismatched request never commits");
        session.logout();
        int before=mismatch.opens;
        ServiceBillingSelfTest.expect(com.quanlynhakhoa.security.AccessDeniedException.class,
                ()->new LanThanhToanService(session,mismatch).reserve(request));
        check(mismatch.opens==before,"expired session denied before JDBC");
        System.out.println("PASS JdbcExtractionSelfTest: "+checks+" checks; commit/close failure ordering, mismatch rollback and expired session.");
    }
}
