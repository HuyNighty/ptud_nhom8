package com.quanlynhakhoa.nghiepvu.hoadon.controller;

import com.quanlynhakhoa.nghiepvu.hoadon.ui.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.*;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.ConnectionProvider;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import javax.swing.*;
import java.awt.GridLayout;

/** Manager-only payment correction/refund. JDBC always stays off the EDT. */
public final class InvoiceAdjustmentController {
    private final InvoicePanel view;private final DesktopSession session;private final ConnectionProvider connections;private boolean busy;
    public InvoiceAdjustmentController(InvoicePanel view,DesktopSession session,ConnectionProvider connections){
        this.view=view;this.session=session;this.connections=connections;
        view.btnDieuChinh.addActionListener(e->reverse());
    }
    private void reverse(){
        if(busy)return;String invoice=view.selectedId();
        if(invoice==null){BillingUi.result(view,"Bạn chưa chọn hóa đơn. Bấm vào một dòng hóa đơn rồi chọn Điều chỉnh khoản thu.");return;}
        String payment=view.choosePaymentForAdjustment();if(payment==null)return;
        JComboBox<String> kind=new JComboBox<>(new String[]{"Hủy ghi nhận sai — không có tiền thực nhận","Hoàn toàn bộ khoản tiền đã nhận"});
        JTextField reason=new JTextField(),reference=new JTextField();JCheckBox confirmed=new JCheckBox("Đã đối chiếu tiền thực tế và bằng chứng; chịu trách nhiệm về xác nhận này");
        JPanel form=new JPanel(new GridLayout(0,1,4,6));form.add(new JLabel("Khoản thu "+payment+" · Hóa đơn "+invoice));form.add(kind);form.add(new JLabel("Lý do (tối đa 300 ký tự)"));form.add(reason);form.add(new JLabel("Bằng chứng / biên nhận hoàn tiền (tối đa 100 ký tự)"));form.add(reference);form.add(confirmed);
        if(JOptionPane.showConfirmDialog(view,form,"Điều chỉnh toàn bộ khoản thu",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.OK_OPTION)return;
        if(!confirmed.isSelected()){BillingUi.result(view,"Cần đánh dấu xác nhận đã đối chiếu tiền thực tế trước khi tiếp tục.");return;}
        var type=kind.getSelectedIndex()==0?PaymentReversalService.Kind.HUY_GHI_NHAN:PaymentReversalService.Kind.HOAN_TIEN;
        run(new Work(){public void execute()throws Exception{new PaymentReversalService(session,connections).reverse(invoice,payment,type,reason.getText(),reference.getText());}},"Đã điều chỉnh khoản thu. Lịch sử gốc được giữ lại.");
    }
    private interface Work{void execute()throws Exception;}
    private void run(Work work,String message){
        busy=true;view.setBusy(true);
        new SessionWorker<Void>(session){
            protected Void doInBackground()throws Exception{work.execute();return null;}
            protected void finished(){busy=false;view.setBusy(false);}
            protected void expired(){view.expired();}
            protected void succeeded(Void ignored){BillingUi.result(view,message);BillingChangeBus.forSession(session).publish();view.refreshData();}
            protected void failed(Throwable failure){
                if(failure instanceof PaymentReversalService.CommittedReversalException){succeeded(null);return;}
                BillingUi.result(view,BillingUi.error(failure)+" Tải lại lịch sử trước khi thao tác tiếp.","Kiểm tra kết quả",JOptionPane.WARNING_MESSAGE);view.refreshData();
            }
        }.execute();
    }
}
