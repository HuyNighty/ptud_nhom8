package com.quanlynhakhoa.quantri.baocao;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import static com.quanlynhakhoa.quantri.baocao.RevenueReportSelfTest.*;

/** Read-only reconciliation against existing local SQL data. */
public final class RevenueReportSqlIT {
    public static void main(String[] args)throws Exception{
        if(args.length!=1||!args[0].equals("--read-only")||!TeamDevConnection.hasLocalConfig())throw new IllegalArgumentException("Local read-only mode required");
        var session=session(VaiTroHeThong.CLINIC_MANAGER);var service=new RevenueReportService(session);
        LocalDate end=LocalDate.now(),from=end.withDayOfMonth(1);
        var daily=service.load(from,end,false);var monthly=service.load(from,end,true);
        check(daily.revenue().compareTo(monthly.revenue())==0&&daily.collected().compareTo(monthly.collected())==0&&daily.closingDebt().compareTo(monthly.closingDebt())==0,"daily/monthly reconcile");
        try(Connection c=TeamDevConnection.open()){
            BigDecimal revenue=BigDecimal.ZERO,paid=BigDecimal.ZERO,debt=BigDecimal.ZERO;int invoices=0;
            try(var s=c.prepareStatement("SELECT h.ngayLap,h.trangThai,(SELECT MAX(y.thoiGianDuyet) FROM dbo.YeuCauHuyHoaDon y WHERE y.maHoaDon=h.maHoaDon AND y.trangThai='DA_DUYET'),COALESCE((SELECT SUM(CAST(d.soLuong AS decimal(10,0))*d.donGiaApDung) FROM dbo.ChiTietHoaDon d WHERE d.maHoaDon=h.maHoaDon),0),COALESCE((SELECT SUM(t.soTien) FROM dbo.ThanhToan t WHERE t.maHoaDon=h.maHoaDon AND t.thoiGian<? AND (t.trangThai='THANH_CONG' OR t.thoiGianHuy>=?)),0) FROM dbo.HoaDon h WHERE h.ngayLap<?")){
                for(int i=1;i<=3;i++)s.setTimestamp(i,Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                try(var r=s.executeQuery()){while(r.next()){
                    boolean active=!r.getString(2).equals("DA_HUY")||(r.getTimestamp(3)!=null&&!r.getTimestamp(3).toLocalDateTime().isBefore(end.plusDays(1).atStartOfDay()));
                    if(active){debt=debt.add(r.getBigDecimal(4).subtract(r.getBigDecimal(5)));if(!r.getTimestamp(1).toLocalDateTime().isBefore(from.atStartOfDay())){revenue=revenue.add(r.getBigDecimal(4));invoices++;}}
                }}
            }
            try(var s=c.prepareStatement("SELECT COALESCE(SUM(soTien),0) FROM dbo.ThanhToan WHERE thoiGian>=? AND thoiGian<? AND (trangThai='THANH_CONG' OR (trangThai='DA_HUY' AND thoiGianHuy>=?))")){
                s.setTimestamp(1,Timestamp.valueOf(from.atStartOfDay()));s.setTimestamp(2,Timestamp.valueOf(end.plusDays(1).atStartOfDay()));s.setTimestamp(3,Timestamp.valueOf(end.plusDays(1).atStartOfDay()));try(var r=s.executeQuery()){r.next();paid=r.getBigDecimal(1);}
            }
            check(daily.revenue().compareTo(revenue)==0&&daily.collected().compareTo(paid)==0&&daily.closingDebt().compareTo(debt)==0&&daily.invoices()==invoices,"report reconciles with invoice/receipt records");
        }finally{session.logout();}
        System.out.println("PASS: SQL read-only report totals, receipt-date collections, end-period receivables and daily/monthly grouping.");
        System.out.println("Revenue="+daily.revenue()+", collected="+daily.collected()+", closingDebt="+daily.closingDebt()+", invoices="+daily.invoices());
    }
}
