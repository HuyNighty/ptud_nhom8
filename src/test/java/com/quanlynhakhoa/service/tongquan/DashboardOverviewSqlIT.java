package com.quanlynhakhoa.service.tongquan;

import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.sql.*;
import java.math.BigDecimal;

/** Opt-in, SELECT-only verification against the approved local development database. */
public final class DashboardOverviewSqlIT {
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        if(args.length!=1||!"--read-only".equals(args[0])||!TeamDevConnection.hasLocalConfig())throw new IllegalArgumentException("Requires --read-only and local dev config");
        for(VaiTroHeThong role:VaiTroHeThong.values()){
            String account="TST_DASH";
            try(Connection c=TeamDevConnection.open();PreparedStatement s=c.prepareStatement("SELECT TOP (1) maTaiKhoan FROM dbo.TaiKhoan WHERE vaiTro=? AND trangThai='HOAT_DONG' ORDER BY maTaiKhoan")){
                s.setString(1,role.name());try(ResultSet rows=s.executeQuery()){if(rows.next())account=rows.getString(1);}
            }
            DesktopSession session=DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true,account,role,false));
            TongQuanService service=new TongQuanService(session,TeamDevConnection::open);
            var overview=service.loadOverview();
            check(overview.warnings().isEmpty(),role+" incomplete sections: "+overview.warnings());
            check(overview.days().size()==7,"seven calendar days");
            check(overview.days().get(0).date().plusDays(6).equals(overview.summary().date()),"continuous date range");
            if(!service.can(Quyen.APPOINTMENT_READ))check(overview.appointments().isEmpty(),"appointment data hidden");
            if(!service.can(Quyen.INVENTORY_READ))check(overview.alerts().isEmpty(),"inventory data hidden");
            for(var day:overview.days()){
                check((day.collected()!=null)==service.can(Quyen.REVENUE_REPORT_READ),"money permission");
                check((day.visits()!=null)==service.can(Quyen.MEDICAL_RECORD_READ),"visit permission");
            }
            if(role==VaiTroHeThong.SYSTEM_ADMIN){
                BigDecimal sum=BigDecimal.ZERO;for(var day:overview.days())sum=sum.add(day.collected());
                try(Connection c=TeamDevConnection.open();PreparedStatement s=c.prepareStatement("SELECT COALESCE(SUM(soTien),0) FROM dbo.ThanhToan WHERE thoiGian>=? AND thoiGian<? AND trangThai='THANH_CONG'")){
                    s.setObject(1,overview.summary().date().minusDays(6).atStartOfDay());s.setObject(2,overview.summary().date().plusDays(1).atStartOfDay());
                    try(ResultSet rows=s.executeQuery()){rows.next();check(sum.compareTo(rows.getBigDecimal(1))==0,"seven-day money matches source");}
                }
            }
            session.logout();boolean rejected=false;try{service.loadOverview();}catch(AccessDeniedException expected){rejected=true;}check(rejected,"expired session rejected");
            System.out.println("PASS dashboard SQL read-only: "+role+" ("+overview.work().size()+" work groups, "+overview.alerts().size()+" stock alerts)");
        }
    }
}
