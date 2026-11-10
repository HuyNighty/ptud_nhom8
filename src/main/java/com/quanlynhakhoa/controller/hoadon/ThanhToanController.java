package com.quanlynhakhoa.controller.hoadon;

import com.quanlynhakhoa.view.hoadon.ThanhToanForm;
import com.quanlynhakhoa.view.hoadon.ThanhToanDialog;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.service.hoadon.ThanhToanService;
import com.quanlynhakhoa.service.hoadon.TaoMaHoaDonNgauNhien;
import com.quanlynhakhoa.service.hoadon.LuuThanhToanDangCho;
import com.quanlynhakhoa.service.hoadon.LanThanhToanService;
import com.quanlynhakhoa.service.hoadon.HoaDonService;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.entity.ThanhToan;
import com.quanlynhakhoa.model.hoadon.ThanhToanDangCho;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.model.hoadon.DuLieuThanhToanDaTai;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.security.DesktopSession;
import java.math.BigDecimal;
import java.awt.event.*;
import java.util.*;
import javax.swing.JOptionPane;

public final class ThanhToanController {
    private final ThanhToanForm view;private final DesktopSession session;private final HoaDonService hoaDon;private final ThanhToanService thanhToan;
    private final String maHoaDon;private final Runnable saved;private DuLieuChiTietHoaDon chiTiet;private boolean busy;private YeuCauThanhToan dangCho;
    private final LuuThanhToanDangCho nhatKyThanhToan;
    private final LanThanhToanService lanThanhToan;
    private final java.util.function.Predicate<YeuCauThanhToan> confirmation;
    private boolean duocThuLai;
    private boolean dangChoHuy;
    private boolean reloadPending;
    public ThanhToanController(ThanhToanDialog view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this(view.form(),session,connections,invoiceId,saved);
    }
    public ThanhToanController(ThanhToanForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved) {
        this(view,session,connections,invoiceId,saved,LuuThanhToanDangCho.local());
    }
    public ThanhToanController(ThanhToanForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved,LuuThanhToanDangCho journal) {
        this(view,session,connections,invoiceId,saved,journal,new java.util.function.Predicate<YeuCauThanhToan>(){
            public boolean test(YeuCauThanhToan request){return JOptionPane.showConfirmDialog(view,
                    "Xác nhận đã nhận "+GiaoDienHoaDon.vnd(request.amount())+" cho hóa đơn "+request.invoiceId()+"?",
                    "Ghi nhận thu tiền",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
        });
    }
    public ThanhToanController(ThanhToanForm view,DesktopSession session,ConnectionProvider connections,String invoiceId,Runnable saved,
            LuuThanhToanDangCho journal,java.util.function.Predicate<YeuCauThanhToan> confirmation) {
        this.nhatKyThanhToan=journal;this.confirmation=confirmation;lanThanhToan=new LanThanhToanService(session,connections);
        this.view=view;this.session=session;this.maHoaDon=invoiceId;this.saved=saved;hoaDon=new HoaDonService(session,connections);thanhToan=new ThanhToanService(session,connections);
        view.btnThuTien.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){collect();}});
        view.btnThuLai.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){collect();}});
        view.btnKiemTra.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){load();}});

        load();
    }
    private boolean allowed(){return !dangChoHuy&&chiTiet!=null&&chiTiet.hoaDon().trangThai()!=TrangThaiHoaDon.DA_HUY&&chiTiet.hoaDon().conNo().signum()>0&&chiTiet.hoaDon().canhBao().isEmpty();}
    private abstract class Task<T> extends SessionWorker<T> {
        Task(){super(ThanhToanController.this.session);}
        protected void finished(){busy=false;view.busy(false,allowed(),dangCho!=null);}
        protected void expired(){view.closeView();}
        protected void failed(Throwable failure){view.lblTrangThai.setText(failure instanceof java.io.IOException?"Không đọc được lần thu chờ đối soát. Chưa thể thu mới; liên hệ quản trị viên.":GiaoDienHoaDon.error(failure));}
    }

    private void load() {
        if(busy){reloadPending=true;return;}reloadPending=false;busy=true;duocThuLai=false;view.allowRetry(false);chiTiet=null;view.loading();view.busy(true,false,dangCho!=null);
        new Task<DuLieuThanhToanDaTai>() {
            protected DuLieuThanhToanDaTai doInBackground()throws Exception{
                YeuCauThanhToan local=nhatKyThanhToan.read(maHoaDon);
                ThanhToanDangCho shared=lanThanhToan.pending(maHoaDon);
                if(local!=null&&shared!=null&&!local.equals(shared.request()))throw new java.io.IOException("Lần thu trên máy khác không khớp nhật ký cục bộ; cần quản lý đối soát.");
                YeuCauThanhToan restored=shared==null?local:shared.request();
                DuLieuChiTietHoaDon result=hoaDon.detail(maHoaDon);
                return new DuLieuThanhToanDaTai(result,restored,thanhToan.pendingCancellation(maHoaDon),shared==null?session.accountId():shared.owner());
            }
            protected void succeeded(DuLieuThanhToanDaTai loaded){
                DuLieuChiTietHoaDon result=loaded.invoice();dangCho=loaded.pending();
                chiTiet=result;dangChoHuy=loaded.cancellation();view.showInvoice(result);
                if(dangCho!=null){
                    view.restoreRequest(dangCho);
                    int found=0;boolean mismatch=false;
                    for(PhanThanhToan part:dangCho.parts())for(ThanhToan prior:result.thanhToan())if(part.id().equals(prior.maThanhToan())){
                        found++;
                        if(prior.trangThai()!=TrangThaiThanhToan.THANH_CONG||prior.phuongThuc()!=part.method()
                                ||prior.soTien().compareTo(part.amount())!=0||!Objects.equals(prior.maGiaoDich(),part.reference())
                                ||(prior.tienKhachDua()==null?part.customerTender()!=null:part.customerTender()==null||prior.tienKhachDua().compareTo(part.customerTender())!=0))mismatch=true;
                    }
                    if(mismatch){view.lblTrangThai.setText("Mã lần thu tồn tại với dữ liệu khác/đã hủy; cần đối soát.");view.busy(false,false,true);return;}
                    if(found==dangCho.parts().size()){view.lblTrangThai.setText("Đã ghi nhận lần thu. Không thu lại.");complete();return;}
                    if(found>0){view.lblTrangThai.setText("Kết quả chưa đầy đủ, cần đối soát; khóa thu lại.");view.busy(false,false,true);return;}
                    duocThuLai=session.accountId().equals(loaded.owner())&&allowed()&&dangCho.amount().compareTo(result.hoaDon().conNo())<=0;
                    view.allowRetry(duocThuLai);
                    view.lblTrangThai.setText("Chưa ghi nhận lần thu "+dangCho.parts().getFirst().id()+". Chỉ xác nhận lại lần thu này; không tạo lần thu mới.");
                    view.busy(false,false,true);return;
                }else view.lblTrangThai.setText(dangChoHuy?"Hóa đơn đang chờ duyệt hủy: khóa ghi nhận thanh toán mới.":allowed()?"Có thể thu một phần hoặc toàn bộ bằng tiền mặt hoặc chuyển khoản.":"Hóa đơn không còn phải thu hoặc cần đối soát.");
                view.busy(false,allowed(),false);
                if(reloadPending)load();
            }
        }.execute();
    }
    private void collect() {
        if(busy||!session.isActive())return;
        final boolean retrying=dangCho!=null;
        if(retrying?!duocThuLai:!allowed()||!view.collectionContextAllowed())return;
        final YeuCauThanhToan request;
        try {
            if(retrying){request=dangCho;}else{
            BigDecimal cash=view.cashAmount(),bank=view.bankAmount();
            List<PhanThanhToan> parts=new ArrayList<>();TaoMaHoaDonNgauNhien ids=new TaoMaHoaDonNgauNhien();
            if(cash.signum()>0)parts.add(new PhanThanhToan(ids.next(null),cash,PhuongThucThanhToan.TIEN_MAT,null,GiaoDienHoaDon.moneyInput(view.txtKhachDua.getText())));
            if(bank.signum()>0)parts.add(new PhanThanhToan(ids.next(null),bank,PhuongThucThanhToan.CHUYEN_KHOAN,view.txtMaGiaoDich.getText().trim(),null));
            request=new YeuCauThanhToan(maHoaDon,GiaoDienHoaDon.moneyInput(view.txtSoTien.getText()),parts,view.chkDaNhanChuyenKhoan.isSelected());ThanhToanService.validate(request);
            }
        }catch(RuntimeException failure){view.lblTrangThai.setText(GiaoDienHoaDon.error(failure));return;}
        if(!confirmation.test(request))return;
        dangCho=request;busy=true;duocThuLai=false;view.allowRetry(false);view.busy(true,false,true);
        new Task<Void>() {
            protected Void doInBackground()throws Exception{
                if(retrying){
                    if(nhatKyThanhToan.read(maHoaDon)!=null&&!request.equals(nhatKyThanhToan.read(maHoaDon)))throw new java.io.IOException("Nhật ký lần thu đã thay đổi; hãy kiểm tra lại.");
                }else nhatKyThanhToan.create(request);
                thanhToan.collect(request);return null;
            }
            protected void succeeded(Void ignored){complete();}
            protected void failed(Throwable failure){
                if(failure instanceof ThanhToanService.CommittedPaymentException){succeeded(null);return;}
                if(failure instanceof IllegalArgumentException&&failure.getSuppressed().length==0){
                    // Validation rejection with successful rollback: no payment was committed.
                    try{nhatKyThanhToan.clear(request);dangCho=null;view.busy(false,allowed(),false);view.lblTrangThai.setText(GiaoDienHoaDon.error(failure));return;}
                    catch(java.io.IOException cleanup){ /* Keep recovery locked if the journal cannot be cleared. */ }
                }
                GiaoDienHoaDon.result(view,GiaoDienHoaDon.error(failure)+" Bấm Kiểm tra kết quả lần thu trước khi thử lại.","Chưa xác nhận kết quả thanh toán",javax.swing.JOptionPane.ERROR_MESSAGE);view.lblTrangThai.setText(GiaoDienHoaDon.error(failure)+" Bấm Kiểm tra kết quả lần thu trước khi thử lại.");view.busy(false,false,true);
            }
        }.execute();
    }
    public void refresh(){load();}
    private void complete(){
        final YeuCauThanhToan completed=dangCho;busy=true;view.busy(true,false,true);
        new Task<Void>(){
            protected Void doInBackground()throws Exception{
                if(completed!=null){
                    // A historical local-only journal may predate the shared reservation schema.
                    if(lanThanhToan.pending(maHoaDon)!=null)lanThanhToan.acknowledge(completed);
                    nhatKyThanhToan.clear(completed);
                }
                return null;
            }
            protected void succeeded(Void ignored){
                duocThuLai=false;view.allowRetry(false);dangCho=null;view.clearAmounts();
                GiaoDienHoaDon.result(view,"Đã ghi nhận thanh toán hóa đơn "+maHoaDon+" thành công.");saved.run();ThongBaoThayDoiHoaDon.forSession(session).publish();if(!view.finish())load();
            }
            protected void failed(Throwable failure){view.lblTrangThai.setText("Khoản thu đã lưu nhưng chưa hoàn tất đối soát. Không thu lại; bấm Kiểm tra kết quả lần thu.");view.busy(false,false,true);}
        }.execute();
    }
}
