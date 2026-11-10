package com.quanlynhakhoa.hethong.tongquan;




import com.quanlynhakhoa.hethong.phanquyen.*;
import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** Synthetic JDBC; checks permission query boundaries, zero days and partial failures. */
public final class DashboardOverviewSelfTest {
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    private static Object zero(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
    private static Connection connection(List<String> queries,boolean failStock){
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("prepareStatement")){
                String sql=(String)a[0];queries.add(sql);
                if(failStock&&sql.contains("dbo.LoHang"))throw new SQLException("Synthetic section failure");
                return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(sp,sm,sa)->{
                    if(sm.getName().equals("executeQuery")){
                        boolean scalar=!sql.contains("GROUP BY")&&(sql.contains("COUNT_BIG")||sql.contains("COALESCE(SUM"));int[] cursor={0};
                        return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),new Class<?>[]{ResultSet.class},(rp,rm,ra)->{
                            if(rm.getName().equals("next"))return scalar&&cursor[0]++==0;
                            if(rm.getName().equals("getLong"))return 0L;
                            if(rm.getName().equals("getBigDecimal"))return BigDecimal.ZERO;
                            return zero(rm.getReturnType());
                        });
                    }
                    return zero(sm.getReturnType());
                });
            }
            return zero(m.getReturnType());
        });
    }
    private static javax.swing.JButton button(java.awt.Container root,String text){
        for(java.awt.Component child:root.getComponents()){
            if(child instanceof javax.swing.JButton b&&b.getText()!=null&&b.getText().contains(text))return b;
            if(child instanceof java.awt.Container nested){var found=button(nested,text);if(found!=null)return found;}
        }return null;
    }
    private static void layout(java.awt.Container root){root.doLayout();for(java.awt.Component child:root.getComponents())if(child instanceof java.awt.Container nested)layout(nested);}
    public static void main(String[] args)throws Exception{
        System.setProperty("java.awt.headless","true");
        for(Role role:Role.values()){
            var session=DesktopSession.fromSuccessfulLogin(new AuthDecision(true,"TEST",role,false));
            List<String> queries=new ArrayList<>();DashboardService service=new DashboardService(session,()->connection(queries,false));
            var result=service.loadOverview();check(result.warnings().isEmpty(),"no synthetic failures");check(result.days().size()==7,"seven zero-filled days");
            for(var day:result.days()){
                check((day.visits()!=null)==service.can(Permission.MEDICAL_RECORD_READ),"visit permission");
                check((day.collected()!=null)==service.can(Permission.REVENUE_REPORT_READ),"collection permission");
            }
            check(queries.stream().anyMatch(q->q.contains("dbo.LoHang"))==service.can(Permission.INVENTORY_READ),"no unauthorized inventory query");
            check(queries.stream().anyMatch(q->q.contains("SELECT TOP (8) l.maLichHen"))==service.can(Permission.APPOINTMENT_READ),"no unauthorized appointment query");
            check(queries.stream().anyMatch(q->q.contains("GROUP BY CAST(thoiGian"))==service.can(Permission.REVENUE_REPORT_READ),"no unauthorized money trend query");
            if(role==Role.DOCTOR)for(String sql:queries)if(sql.contains("dbo.PhieuKham"))check(sql.contains("maTaiKhoan=?"),"doctor aggregate scoped by account");
            int before=queries.size();session.logout();boolean denied=false;try{service.loadOverview();}catch(AccessDeniedException expected){denied=true;}check(denied&&before==queries.size(),"logout rejects before SQL");
        }
        var admin=DesktopSession.fromSuccessfulLogin(new AuthDecision(true,"TEST",Role.SYSTEM_ADMIN,false));
        var partial=new DashboardService(admin,()->connection(new ArrayList<>(),true)).loadOverview();
        check(partial.warnings().contains("Chưa tải được cảnh báo kho"),"stock error reported");
        check(partial.work().size()==3&&partial.days().get(0).collected()!=null,"unrelated sections survive");
        var panel=new com.quanlynhakhoa.hethong.giaodien.DashboardPanel[1];
        var destination=new String[1];
        javax.swing.SwingUtilities.invokeAndWait(()->{
            panel[0]=new com.quanlynhakhoa.hethong.giaodien.DashboardPanel(admin,new DashboardService(admin,()->connection(new ArrayList<>(),false)));
            panel[0].setNavigation((module,context)->destination[0]=module.name()+":"+context);
        });
        for(int i=0;i<100;i++){
            boolean[] ready={false};javax.swing.SwingUtilities.invokeAndWait(()->ready[0]=button(panel[0],"Hủy hóa đơn chờ")!=null);
            if(ready[0])break;Thread.sleep(30);
        }
        javax.swing.SwingUtilities.invokeAndWait(()->{
            panel[0].setSize(1100,1300);layout(panel[0]);
            check(panel[0].getPreferredSize().width<=1100,"dashboard fits typical content width");
            button(panel[0],"Hủy hóa đơn chờ").doClick();check("INVOICE_CANCELLATIONS:".equals(destination[0]),"cancellation route");
            button(panel[0],"Cấp phát chờ").doClick();check("INVENTORY:supply".equals(destination[0]),"supply route");
            button(panel[0],"Kiểm kê chờ").doClick();check("INVENTORY:stocktake".equals(destination[0]),"stocktake route");
            admin.logout();destination[0]=null;button(panel[0],"Kiểm kê chờ").doClick();check(destination[0]==null,"expired UI cannot navigate");
        });
        System.out.println("PASS dashboard synthetic: all roles, query permissions, doctor scope, zero days, expired session, isolated section failure.");
    }
}
