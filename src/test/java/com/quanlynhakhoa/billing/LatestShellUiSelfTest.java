package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.controller.BillingChangeBus;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.DebtService;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import static com.quanlynhakhoa.billing.ServiceBillingSelfTest.*;

public final class LatestShellUiSelfTest {
    private static HoaDon invoice(String id,String patient,String total,String paid,TrangThaiHoaDon status){return new HoaDon(id,"NV1",patient,"Synthetic "+patient,null,LocalDateTime.now(),status,new BigDecimal(total),new BigDecimal(paid),BigDecimal.ZERO);}
    public static void main(String[] args)throws Exception {
        List<PatientDebt> grouped=DebtService.byPatient(List.of(invoice("H1","B1","300","100",TrangThaiHoaDon.THANH_TOAN_MOT_PHAN),invoice("H2","B1","100","0",TrangThaiHoaDon.CHUA_THANH_TOAN),invoice("H3","B2","100","0",TrangThaiHoaDon.DA_HUY),invoice("H4","B3","100","100",TrangThaiHoaDon.DA_THANH_TOAN)));
        check(grouped.size()==1&&grouped.getFirst().invoices().size()==2&&grouped.getFirst().debt().intValueExact()==300,"patient aggregation retains outstanding invoices and excludes cancelled/paid");
        final DesktopSession session=session(Role.SYSTEM_ADMIN);final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();
        final FeatureJdbcDouble unrelatedJdbc=new FeatureJdbcDouble();final DesktopSession unrelated=session(Role.CLINIC_MANAGER);
        final DebtPanel[] debt=new DebtPanel[1];final PaymentPanel[] payment=new PaymentPanel[1];
        final java.util.List<JPanel> cached=new java.util.ArrayList<>();
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){
            cached.add(new HoaDonPanel(session,jdbc));debt[0]=new DebtPanel(session,jdbc);payment[0]=new PaymentPanel(session,jdbc);cached.add(new CancellationPanel(session,jdbc));
            cached.add(new DebtPanel(unrelated,unrelatedJdbc));
            check(jdbc.opens==0&&unrelatedJdbc.opens==0,"new panels do not query in constructors");
            debt[0].showPatients(grouped);check(debt[0].tblBenhNhan.getRowCount()==1,"debt page groups by patient rather than copying invoice list");
            debt[0].tblBenhNhan.setRowSelectionInterval(0,0);check(debt[0].tblHoaDon.getRowCount()==2,"selected patient exposes underlying unpaid invoices");
            check(payment[0].cmbHoaDon.getItemCount()==0,"payment page uses compact picker, not a cloned invoice table");
            BillingChangeBus.forSession(session).publish();
        }});
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(jdbc.opens<4&&System.nanoTime()<deadline)Thread.sleep(20);
        check(jdbc.opens>=4&&!jdbc.onEdt,"invalidation reaches all four already-created cards, JDBC remains off EDT");
        check(unrelatedJdbc.opens==0,"changes are scoped to one authenticated desktop session");
        session.logout();final int before=jdbc.opens;
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){BillingChangeBus.forSession(session).publish();}});
        SwingUtilities.invokeAndWait(new Runnable(){public void run(){check(!debt[0].btnThuNo.isEnabled()&&!payment[0].btnTim.isEnabled(),"logged-out cached views are cleared and disabled");}});
        check(jdbc.opens==before,"logout refresh does not open connections");
        System.out.println("PASS: LatestShellUiSelfTest (patient debt grouping, distinct collection workspace, cached-card invalidation/session isolation; no real SQL).");
    }
}
