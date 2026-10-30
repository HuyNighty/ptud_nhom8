package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.controller.TaoHoaDonController;
import com.quanlynhakhoa.billing.dao.InvoiceCreateDao;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.TaoHoaDonDialog;
import com.quanlynhakhoa.model.Role;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import javax.swing.*;

/** Controller step transitions only. In-memory fixtures never produce images or business SQL. */
public final class CreationStepsSelfTest {
    private static int checks;
    private static volatile boolean noPatients,noVisits,noCharges;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void edt(Runnable work)throws Exception{SwingUtilities.invokeAndWait(work);}
    private static void waitFor(TaoHoaDonDialog view,String text)throws Exception{long until=System.nanoTime()+10_000_000_000L;while(System.nanoTime()<until){boolean[] done={false};edt(new Runnable(){public void run(){done[0]=view.lblTrangThai.getText().contains(text)&&view.btnTimBenhNhan.isEnabled();}});if(done[0])return;Thread.sleep(20);}throw new AssertionError("Missing step: "+text);}
    public static void main(String[] args)throws Exception{
        var session=ServiceBillingSelfTest.session(Role.RECEPTIONIST);var jdbc=new FeatureJdbcDouble();
        InvoiceCreateDao dao=new InvoiceCreateDao(){
            @Override public List<BillingChoice> patients(Connection c,String text){return noPatients?List.of():List.of(new BillingChoice("BN1","Logic fixture"));}
            @Override public List<BillingChoice> visits(Connection c,String patient){return noVisits?List.of():List.of(new BillingChoice("PK1","Logic fixture"),new BillingChoice("PK2","Logic fixture"));}
            @Override public void validateVisit(Connection c,String patient,String visit){}
            @Override public String employee(Connection c,String account){return "NV1";}
            @Override public boolean serviceActive(Connection c,String id){return true;}
        };
        ClinicalBillingSource source=new ClinicalBillingSource(){
            public InvoiceDraft load(Connection c,String patient,String visit,boolean lock){if(noCharges)throw new IllegalArgumentException("Lượt khám không có dịch vụ đã thực hiện được phép tính phí.");return new InvoiceDraft(patient,visit,"logic",List.of(new InvoiceDraft.Line("DV1","Logic fixture",1,new BigDecimal("100000"))));}
            public void validateServiceState(Connection c,InvoiceDraft draft,String id,boolean active){}
        };
        HoaDonService service=new HoaDonService(session,jdbc);var field=HoaDonService.class.getDeclaredField("creation");field.setAccessible(true);field.set(service,new InvoiceCreationService(session,jdbc,source,new RandomInvoiceIdGenerator(),dao));
        TaoHoaDonDialog[] view={null};
        try{
            edt(new Runnable(){public void run(){view[0]=new TaoHoaDonDialog(null);new TaoHoaDonController(view[0],session,service,new TaoHoaDonController.Saved(){public void accept(String id){throw new AssertionError("Must not save in a step test");}});check(view[0].lblTrangThai.getText().equals("Tìm bệnh nhân để bắt đầu"),"Initial step");view[0].btnTimBenhNhan.doClick();}});
            waitFor(view[0],"Chọn bệnh nhân");edt(new Runnable(){public void run(){check(view[0].cmbBenhNhan.getSelectedIndex()==-1&&!view[0].btnTaiDichVu.isEnabled(),"No automatic first patient selection");view[0].cmbBenhNhan.setSelectedIndex(0);}});
            waitFor(view[0],"Chọn lượt khám");edt(new Runnable(){public void run(){check(view[0].cmbPhieuKham.getSelectedIndex()==-1&&!view[0].btnLuuHoaDon.isEnabled(),"Visit must be selected explicitly");view[0].cmbPhieuKham.setSelectedIndex(0);check(view[0].btnTaiDichVu.isEnabled(),"Selected visit enables loading");view[0].btnTaiDichVu.doClick();}});
            waitFor(view[0],"Kiểm tra dịch vụ");edt(new Runnable(){public void run(){check(view[0].btnLuuHoaDon.isEnabled(),"Trusted preview enables save");view[0].cmbPhieuKham.setSelectedIndex(1);check(!view[0].btnLuuHoaDon.isEnabled(),"Changing visit invalidates draft");noCharges=true;view[0].btnTaiDichVu.doClick();}});
            waitFor(view[0],"không có dịch vụ");edt(new Runnable(){public void run(){check(!view[0].btnLuuHoaDon.isEnabled(),"No charges cannot save");noVisits=true;view[0].cmbBenhNhan.setSelectedIndex(-1);view[0].cmbBenhNhan.setSelectedIndex(0);}});
            waitFor(view[0],"chưa có lượt khám");edt(new Runnable(){public void run(){check(!view[0].btnTaiDichVu.isEnabled(),"No visits disables load");noPatients=true;view[0].btnTimBenhNhan.doClick();}});
            waitFor(view[0],"Không tìm thấy bệnh nhân");edt(new Runnable(){public void run(){check(view[0].cmbBenhNhan.getItemCount()==0&&view[0].cmbPhieuKham.getItemCount()==0,"New search clears previous context");}});
            check(!jdbc.onEdt&&jdbc.updates==0,"Off-EDT reads only, no write attempted");
        }finally{session.logout();edt(new Runnable(){public void run(){if(view[0]!=null)view[0].dispose();}});}
        System.out.println("PASS: CreationStepsSelfTest "+checks+" checks; async steps/no matches/no visits/no charges/draft invalidation (logic fixtures, no screenshots or real SQL).");
    }
}
