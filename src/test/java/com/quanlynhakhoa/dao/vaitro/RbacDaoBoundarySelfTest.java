package com.quanlynhakhoa.dao.vaitro;

import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest;
import com.quanlynhakhoa.service.vaitro.PhanQuyenCoBanService;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/** Synthetic boundary/ownership checks; never executes SQL Server or grants privileges. */
public final class RbacDaoBoundarySelfTest {
    private static int checks;
    private static void check(boolean ok, String why) { checks++; if(!ok)throw new AssertionError(why); }
    private interface Action { void run() throws Exception; }
    private static void rejects(Action action) throws Exception {
        try { action.run(); throw new AssertionError("Expected SQL guard rejection"); }
        catch(SQLException expected) { checks++; }
    }
    private static final class Jdbc {
        List<Object[]> values=List.of();
        int affected=1, statementCloses, resultCloses;
        final Map<Integer,Object> bindings=new HashMap<>();
        final List<String> sql=new ArrayList<>();
        Connection connection() {
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},
                    new InvocationHandler() {
                        public Object invoke(Object p,Method m,Object[] a) {
                            check(m.getName().equals("prepareStatement"),"DAO must not open/close/commit/rollback caller connection");
                            String query=(String)a[0];sql.add(query);
                            check(!query.startsWith("GRANT ")&&!query.startsWith("ALTER "),"catalog DAO never expands SQL privileges");
                            return statement();
                        }
                    });
        }
        PreparedStatement statement() {
            return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},
                    new InvocationHandler() {
                        public Object invoke(Object p,Method m,Object[] a) {
                            return switch(m.getName()) {
                                case "setQueryTimeout" -> { check(a[0].equals(10),"query timeout preserved"); yield null; }
                                case "setString","setNString" -> { bindings.put((int)a[0],a[1]); yield null; }
                                case "executeQuery" -> rows();
                                case "executeUpdate" -> affected;
                                case "close" -> { statementCloses++; yield null; }
                                default -> throw new AssertionError(m.getName());
                            };
                        }
                    });
        }
        ResultSet rows() {
            return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},
                    new InvocationHandler() {
                        int row=-1; boolean wasNull;
                        public Object invoke(Object p,Method m,Object[] a) {
                            if(m.getName().equals("next"))return ++row<values.size();
                            if(m.getName().equals("close")){resultCloses++;return null;}
                            if(m.getName().equals("wasNull"))return wasNull;
                            Object value=values.get(row)[(int)a[0]-1];wasNull=value==null;
                            return switch(m.getName()) {
                                case "getString","getNString" -> value;
                                case "getBoolean" -> value!=null&&(boolean)value;
                                case "getLong" -> value==null?0L:((Number)value).longValue();
                                default -> throw new AssertionError(m.getName());
                            };
                        }
                    });
        }
    }
    public static void main(String[] args)throws Exception {
        PhanQuyenCoBanDao dao=new PhanQuyenCoBanDao();Jdbc db=new Jdbc();Connection c=db.connection();
        db.values=Collections.singletonList(new Object[]{2L});check(dao.epoch(c)==2,"valid epoch");
        for(final List<Object[]> invalid:List.of(List.<Object[]>of(),Collections.singletonList(new Object[]{0L}),
                Collections.singletonList(new Object[]{null}),List.of(new Object[]{1L},new Object[]{2L}))) {
            db.values=invalid;rejects(new Action(){public void run()throws Exception{dao.epoch(c);}});
        }
        db.values=Collections.singletonList(new Object[]{"SYSTEM_ADMIN","Admin",true,true});
        var roles=dao.listRoles(c);check(roles.size()==1&&roles.getFirst().code().equals("SYSTEM_ADMIN"),"role mapping");
        try{roles.clear();throw new AssertionError("mutable roles");}catch(UnsupportedOperationException expected){checks++;}
        db.values=List.of(new Object[]{"BILLING_READ","Billing"},new Object[]{"CUSTOM_READ","Custom"});
        var permissions=dao.listPermissions(c);
        check(permissions.getFirst().builtIn()&&!permissions.getLast().builtIn(),"built-in/custom metadata preserved");
        db.values=Collections.singletonList(new Object[]{"bad lower","Invalid"});
        rejects(new Action(){public void run()throws Exception{dao.listPermissions(c);}});
        dao.taoVaiTro(c,"CUSTOM","Custom","Description");
        check(db.bindings.get(1).equals("CUSTOM")&&db.bindings.get(2).equals("Custom")&&db.bindings.get(3).equals("Description"),"role parameter order");
        dao.ganQuyenBanDau(c,"CUSTOM",EnumSet.of(Quyen.BILLING_READ));
        check(db.bindings.get(1).equals("CUSTOM")&&db.bindings.get(2).equals("BILLING_READ"),"grant parameter order");
        dao.khaiBaoQuyen(c,"CUSTOM_READ","Custom read");
        db.affected=0;rejects(new Action(){public void run()throws Exception{dao.khaiBaoQuyen(c,"CUSTOM_READ","Custom read");}});
        check(db.statementCloses==db.sql.size(),"statements close on success and rejection");
        final int[] opened={0};var session=ServiceBillingSelfTest.session(VaiTroHeThong.SYSTEM_ADMIN);
        PhanQuyenCoBanService service=new PhanQuyenCoBanService(session,new PhanQuyenCoBanService.Connections(){
            public Connection open(){opened[0]++;return c;}
        });
        ServiceBillingSelfTest.expect(com.quanlynhakhoa.security.AccessDeniedException.class,
                new ServiceBillingSelfTest.Action(){public void run()throws Exception{service.listRoles();}});
        check(opened[0]==0,"unbound synthetic admin still cannot access catalog");session.logout();
        System.out.println("PASS RbacDaoBoundarySelfTest: "+checks+" checks; connection ownership, epoch/row-count guards, mapping, bindings and unbound-admin denial.");
    }
}
