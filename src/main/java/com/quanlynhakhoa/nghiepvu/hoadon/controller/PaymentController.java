package com.quanlynhakhoa.nghiepvu.hoadon.controller;

import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.ThanhToanDangCho;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.model.hoadon.DuLieuThanhToanDaTai;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.ConnectionProvider;
import com.quanlynhakhoa.nghiepvu.hoadon.service.*;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.*;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.math.BigDecimal;
import java.awt.event.*;
import java.util.*;
import javax.swing.JOptionPane;

public final class PaymentController {
    private final PaymentForm view;private final DesktopSession session;private final InvoiceService invoices;private final PaymentService payments;
    private final String invoiceId;private final Runnable saved;private DuLieuChiTietHoaDon detail;private boolean busy;private YeuCauThanhToan pending;
    private final PendingPaymentStore journal;
    private final PaymentAttemptService attempts;
    private final java.util.function.Predicate<YeuCauThanhToan> confirmation;
    private boolean retryAllowed;
    private boolean pendingCancellation;
    private boolean reloadPending;
    public PaymentController(PaymentDialog view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this(view.form(),session,connections,invoiceId,saved);
    }
    public PaymentController(PaymentForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this(view,session,connections,invoiceId,saved,PendingPaymentStore.local());
    }
    public PaymentController(PaymentForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved,PendingPaymentStore journal) {
        this(view,session,connections,invoiceId,saved,journal,new java.util.function.Predicate<YeuCauThanhToan>(){
            public boolean test(YeuCauThanhToan request){return JOptionPane.showConfirmDialog(view,
                    "Xác nhận đã nhận "+BillingUi.vnd(request.amount())+" cho hóa đơn "+request.invoiceId()+"?",
                    "Ghi nhận thu tiền",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
        });
    }
    public PaymentController(PaymentForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved,
            PendingPaymentStore journal,java.util.function.Predicate<YeuCauThanhToan> confirmation) {
        this.journal=journal;this.confirmation=confirmation;attempts=new PaymentAttemptService(session,connections);
        this.view=view;this.session=session;this.invoiceId=invoiceId;this.saved=saved;invoices=new InvoiceService(session,connections);payments=new PaymentService(session,connections);
        view.btnThuTien.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){collect();}});
        view.btnThuLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){collect();}});
        view.btnKiemTra.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});

        load();
    }
    private boolean allowed(){return !pendingCancellation&&detail!=null&&detail.hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY&&detail.hoaDon().conNo().signum()>0&&detail.hoaDon().canhBao().isEmpty();}
    private abstract class Task<T> extends SessionWorker<T> {
        Task(){super(PaymentController.this.session);}
        protected void finished(){busy=false;view.busy(false,allowed(),pending!=null);}
        protected void expired(){view.closeView();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(failure instanceof java.io.IOException?"Không đọc được lần thu chờ đối soát. Chưa thể thu mới; liên hệ quản trị viên.":BillingUi.error(failure));}
    }

    private void load() {
        if(busy){reloadPending=true;return;}reloadPending=false;busy=true;retryAllowed=false;view.allowRetry(false);detail=null;view.loading();view.busy(true,false,pending!=null);
        new Task<DuLieuThanhToanDaTai>() {
            protected DuLieuThanhToanDaTai doInBackground()throws Exception{
                YeuCauThanhToan local=journal.read(invoiceId);
                ThanhToanDangCho shared=attempts.pending(invoiceId);
                if(local!=null&&shared!=null&&!local.equals(shared.request()))throw new java.io.IOException("Lần thu trên máy khác không khớp nhật ký cục bộ; cần quản lý đối soát.");
                YeuCauThanhToan restored=shared==null?local:shared.request();
                DuLieuChiTietHoaDon result=invoices.detail(invoiceId);
                return new DuLieuThanhToanDaTai(result,restored,payments.pendingCancellation(invoiceId),shared==null?session.accountId():shared.owner());
            }
            protected void succeeded(DuLieuThanhToanDaTai loaded){
                DuLieuChiTietHoaDon result=loaded.invoice();pending=loaded.pending();
                detail=result;pendingCancellation=loaded.cancellation();view.showInvoice(result);
                if(pending!=null){
                    view.restoreRequest(pending);
                    int found=0;boolean mismatch=false;
                    for(PhanThanhToan part:pending.parts())for(ThongTinThanhToan prior:result.thanhToan())if(part.id().equals(prior.maThanhToan())){
                        found++;
                        if(prior.trangThai()!=TrangThaiThanhToan.THANH_CONG||prior.phuongThuc()!=part.method()
                                ||prior.soTien().compareTo(part.amount())!=0||!Objects.equals(prior.maGiaoDich(),part.reference())
                                ||(prior.tienKhachDua()==null?part.customerTender()!=null:part.customerTender()==null||prior.tienKhachDua().compareTo(part.customerTender())!=0))mismatch=true;
                    }
                    if(mismatch){view.lblTrangThai.setText("Mã lần thu tồn tại với dữ liệu khác/đã hủy; cần đối soát.");view.busy(false,false,true);return;}
                    if(found==pending.parts().size()){view.lblTrangThai.setText("Đã ghi nhận lần thu. Không thu lại.");complete();return;}
                    if(found>0){view.lblTrangThai.setText("Kết quả chưa đầy đủ, cần đối soát; khóa thu lại.");view.busy(false,false,true);return;}
                    retryAllowed=session.accountId().equals(loaded.owner())&&allowed()&&pending.amount().compareTo(result.hoaDon().conNo())<=0;
                    view.allowRetry(retryAllowed);
                    view.lblTrangThai.setText("Chưa ghi nhận lần thu "+pending.parts().getFirst().id()+". Chỉ xác nhận lại lần thu này; không tạo lần thu mới.");
                    view.busy(false,false,true);return;
                }else view.lblTrangThai.setText(pendingCancellation?"Hóa đơn đang chờ duyệt hủy: khóa ghi nhận thanh toán mới.":allowed()?"Có thể thu một phần hoặc toàn bộ bằng tiền mặt hoặc chuyển khoản.":"Hóa đơn không còn phải thu hoặc cần đối soát.");
                view.busy(false,allowed(),false);
                if(reloadPending)load();
            }
        }.execute();
    }
    private void collect() {
        if(busy||!session.isActive())return;
        final boolean retrying=pending!=null;
        if(retrying?!retryAllowed:!allowed()||!view.collectionContextAllowed())return;
        final YeuCauThanhToan request;
        try {
            if(retrying){request=pending;}else{
            BigDecimal cash=view.cashAmount(),bank=view.bankAmount();
            List<PhanThanhToan> parts=new ArrayList<>();RandomInvoiceIdGenerator ids=new RandomInvoiceIdGenerator();
            if(cash.signum()>0)parts.add(new PhanThanhToan(ids.next(null),cash,PhuongThucThanhToan.TIEN_MAT,null,BillingUi.moneyInput(view.txtKhachDua.getText())));
            if(bank.signum()>0)parts.add(new PhanThanhToan(ids.next(null),bank,PhuongThucThanhToan.CHUYEN_KHOAN,view.txtMaGiaoDich.getText().trim(),null));
            request=new YeuCauThanhToan(invoiceId,BillingUi.moneyInput(view.txtSoTien.getText()),parts,view.chkDaNhanChuyenKhoan.isSelected());PaymentService.validate(request);
            }
        }catch(RuntimeException failure){view.lblTrangThai.setText(BillingUi.error(failure));return;}
        if(!confirmation.test(request))return;
        pending=request;busy=true;retryAllowed=false;view.allowRetry(false);view.busy(true,false,true);
        new Task<Void>() {
            protected Void doInBackground()throws Exception{
                if(retrying){
                    if(journal.read(invoiceId)!=null&&!request.equals(journal.read(invoiceId)))throw new java.io.IOException("Nhật ký lần thu đã thay đổi; hãy kiểm tra lại.");
                }else journal.create(request);
                payments.collect(request);return null;
            }
            protected void succeeded(Void ignored){complete();}
            protected void failed(Throwable failure){
                if(failure instanceof PaymentService.CommittedPaymentException){succeeded(null);return;}
                if(failure instanceof IllegalArgumentException&&failure.getSuppressed().length==0){
                    // Validation rejection with successful rollback: no payment was committed.
                    try{journal.clear(request);pending=null;view.busy(false,allowed(),false);view.lblTrangThai.setText(BillingUi.error(failure));return;}
                    catch(java.io.IOException cleanup){ /* Keep recovery locked if the journal cannot be cleared. */ }
                }
                BillingUi.result(view,BillingUi.error(failure)+" Bấm Kiểm tra kết quả lần thu trước khi thử lại.","Chưa xác nhận kết quả thanh toán",javax.swing.JOptionPane.ERROR_MESSAGE);view.lblTrangThai.setText(BillingUi.error(failure)+" Bấm Kiểm tra kết quả lần thu trước khi thử lại.");view.busy(false,false,true);
            }
        }.execute();
    }
    public void refresh(){load();}
    private void complete(){
        final YeuCauThanhToan completed=pending;busy=true;view.busy(true,false,true);
        new Task<Void>(){
            protected Void doInBackground()throws Exception{
                if(completed!=null){
                    // A historical local-only journal may predate the shared reservation schema.
                    if(attempts.pending(invoiceId)!=null)attempts.acknowledge(completed);
                    journal.clear(completed);
                }
                return null;
            }
            protected void succeeded(Void ignored){
                retryAllowed=false;view.allowRetry(false);pending=null;view.clearAmounts();
                BillingUi.result(view,"Đã ghi nhận thanh toán hóa đơn "+invoiceId+" thành công.");saved.run();BillingChangeBus.forSession(session).publish();if(!view.finish())load();
            }
            protected void failed(Throwable failure){view.lblTrangThai.setText("Khoản thu đã lưu nhưng chưa hoàn tất đối soát. Không thu lại; bấm Kiểm tra kết quả lần thu.");view.busy(false,false,true);}
        }.execute();
    }
}
