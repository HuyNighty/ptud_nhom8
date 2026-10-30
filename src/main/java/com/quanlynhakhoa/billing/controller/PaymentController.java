package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.security.DesktopSession;
import java.math.BigDecimal;
import java.awt.event.*;
import java.util.*;
import javax.swing.JOptionPane;

public final class PaymentController {
    private final PaymentForm view;private final DesktopSession session;private final HoaDonService invoices;private final PaymentService payments;
    private final String invoiceId;private final Runnable saved;private ChiTietBilling detail;private boolean busy;private PaymentRequest pending;
    private boolean pendingCancellation;
    private boolean reloadPending;
    public PaymentController(PaymentDialog view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this(view.form(),session,connections,invoiceId,saved);
    }
    public PaymentController(PaymentForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this.view=view;this.session=session;this.invoiceId=invoiceId;this.saved=saved;invoices=new HoaDonService(session,connections);payments=new PaymentService(session,connections);
        view.btnThuTien.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){collect();}});
        view.btnKiemTra.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});

        load();
    }
    private boolean allowed(){return !pendingCancellation&&detail!=null&&detail.hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY&&detail.hoaDon().conNo().signum()>0&&detail.hoaDon().canhBao().isEmpty();}
    private abstract class Task<T> extends SessionWorker<T> {
        Task(){super(PaymentController.this.session);}
        protected void finished(){busy=false;view.busy(false,allowed(),pending!=null);}
        protected void expired(){view.closeView();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(BillingUi.error(failure));}
    }
    private void load() {
        if(busy){reloadPending=true;return;}reloadPending=false;busy=true;detail=null;view.loading();view.busy(true,false,pending!=null);
        new Task<ChiTietBilling>() {
            private boolean awaitingCancellation;
            protected ChiTietBilling doInBackground()throws Exception{ChiTietBilling result=invoices.detail(invoiceId);awaitingCancellation=payments.pendingCancellation(invoiceId);return result;}
            protected void succeeded(ChiTietBilling result){
                detail=result;pendingCancellation=awaitingCancellation;view.showInvoice(result);
                if(pending!=null){
                    int found=0;boolean mismatch=false;
                    for(PaymentRequest.Part part:pending.parts())for(ThanhToan prior:result.thanhToan())if(part.id().equals(prior.maThanhToan())){
                        found++;
                        if(prior.trangThai()!=ThanhToan.TrangThai.THANH_CONG||prior.phuongThuc()!=part.method()
                                ||prior.soTien().compareTo(part.amount())!=0||!Objects.equals(prior.maGiaoDich(),part.reference())
                                ||(prior.tienKhachDua()==null?part.customerTender()!=null:part.customerTender()==null||prior.tienKhachDua().compareTo(part.customerTender())!=0))mismatch=true;
                    }
                    if(mismatch){view.lblTrangThai.setText("Mã lần thu tồn tại với dữ liệu khác/đã hủy; cần đối soát.");view.busy(false,false,true);return;}
                    if(found==pending.parts().size()){view.lblTrangThai.setText("Đã ghi nhận lần thu. Không thu lại.");complete();return;}
                    if(found>0){view.lblTrangThai.setText("Kết quả chưa đầy đủ, cần đối soát; khóa thu lại.");view.busy(false,false,true);return;}
                    pending=null;view.lblTrangThai.setText("Chưa thấy mã lần thu trước. Đã tải lại dữ liệu; kiểm tra trước khi xác nhận mới.");
                }else view.lblTrangThai.setText(pendingCancellation?"Hóa đơn đang chờ duyệt hủy: khóa ghi nhận thanh toán mới.":allowed()?"Có thể thu một phần hoặc toàn bộ, tiền mặt/chuyển khoản/phối hợp.":"Hóa đơn không còn phải thu hoặc cần đối soát.");
                view.busy(false,allowed(),false);
                if(reloadPending)load();
            }
        }.execute();
    }
    private void collect() {
        if(busy||pending!=null||!allowed()||!view.collectionContextAllowed())return;
        final PaymentRequest request;
        try {
            BigDecimal cash=view.cashAmount(),bank=view.bankAmount();
            List<PaymentRequest.Part> parts=new ArrayList<>();RandomInvoiceIdGenerator ids=new RandomInvoiceIdGenerator();
            if(cash.signum()>0)parts.add(new PaymentRequest.Part(ids.next(null),cash,ThanhToan.PhuongThuc.TIEN_MAT,null,BillingUi.moneyInput(view.txtKhachDua.getText())));
            if(bank.signum()>0)parts.add(new PaymentRequest.Part(ids.next(null),bank,ThanhToan.PhuongThuc.CHUYEN_KHOAN,view.txtMaGiaoDich.getText().trim(),null));
            request=new PaymentRequest(invoiceId,BillingUi.moneyInput(view.txtSoTien.getText()),parts,view.chkDaNhanChuyenKhoan.isSelected());PaymentService.validate(request);
        }catch(RuntimeException failure){view.lblTrangThai.setText(BillingUi.error(failure));return;}
        if(JOptionPane.showConfirmDialog(view,"Xác nhận đã nhận "+BillingUi.vnd(request.amount())+"?","Ghi nhận thu tiền",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        pending=request;busy=true;view.busy(true,false,true);
        new Task<Void>() {
            protected Void doInBackground()throws Exception{payments.collect(request);return null;}
            protected void succeeded(Void ignored){complete();}
            protected void failed(Throwable failure){
                if(failure instanceof PaymentService.CommittedPaymentException){succeeded(null);return;}
                view.lblTrangThai.setText(BillingUi.error(failure)+" Bấm Kiểm tra kết quả lần thu trước khi thử lại.");view.busy(false,false,true);
            }
        }.execute();
    }
    public void refresh(){load();}
    private void complete(){pending=null;view.clearAmounts();saved.run();BillingChangeBus.forSession(session).publish();if(!view.finish())load();}
}
