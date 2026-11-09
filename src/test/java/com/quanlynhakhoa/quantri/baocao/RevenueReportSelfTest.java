package com.quanlynhakhoa.quantri.baocao;

import com.quanlynhakhoa.model.baocao.DongBaoCaoDoanhThu;
import com.quanlynhakhoa.model.baocao.BaoCaoDoanhThu;
import com.quanlynhakhoa.hethong.phanquyen.*;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.hethong.dieuhuong.NavigationPolicy;
import com.quanlynhakhoa.hethong.tinhnang.FeatureBootstrap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

public final class RevenueReportSelfTest {
    static DesktopSession session(VaiTroHeThong role) {return DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true,"REPORT_TEST",role,false));}
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        LocalDate day=LocalDate.of(2026,11,6);
        try{RevenueReportService.validate(day,day.minusDays(1));throw new AssertionError("invalid date accepted");}
        catch(IllegalArgumentException expected){ }
        check(FeatureBootstrap.createRegistry().contains(Module.REVENUE_REPORTS),"report registered");
        for(VaiTroHeThong role:VaiTroHeThong.values()){
            var session=session(role);boolean allowed=role==VaiTroHeThong.CLINIC_MANAGER||role==VaiTroHeThong.SYSTEM_ADMIN;
            check(new NavigationPolicy().canOpen(session,Module.REVENUE_REPORTS)==allowed,"report role "+role);
            AtomicInteger opens=new AtomicInteger();
            var service=new RevenueReportService(session,()->{opens.incrementAndGet();throw new java.sql.SQLException("synthetic offline");});
            if(!allowed){try{service.load(day,day,false);throw new AssertionError("access allowed");}catch(AccessDeniedException expected){ }check(opens.get()==0,"denied before SQL");}
            session.logout();try{service.load(day,day,false);throw new AssertionError("logged out access");}catch(AccessDeniedException expected){ }
        }
        var manager=session(VaiTroHeThong.CLINIC_MANAGER);
        BaoCaoDoanhThu report=new BaoCaoDoanhThu(day,day,false,List.of(new DongBaoCaoDoanhThu(day,2,new BigDecimal("400000"),new BigDecimal("150000"))),new BigDecimal("400000"),new BigDecimal("150000"),new BigDecimal("250000"),2);
        SwingUtilities.invokeAndWait(()->{
            var panel=new RevenueReportPanel(manager,new RevenueReportService(manager,()->{throw new AssertionError("preview must not open SQL");}),false);
            panel.showReport(report);panel.setSize(1200,760);layout(panel);
            var image=new java.awt.image.BufferedImage(1200,760,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
        });
        System.out.println("PASS: report validation, role isolation, registration and headless Swing rendering.");
    }
    static void layout(java.awt.Container parent){parent.doLayout();for(var child:parent.getComponents())if(child instanceof java.awt.Container container)layout(container);}
}
