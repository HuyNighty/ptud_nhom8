package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.HoaDonService;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.event.*;

public final class HoaDonController {
    private final HoaDonPanel view;
    private final DesktopSession session;
    private final HoaDonService service;
    private boolean busy;
    private String createdId;
    private final Runnable invalidated;
    private boolean reloadPending;
    private String selectionAfterLoad;
    private boolean afterPayment;
    public void paymentSaved(){afterPayment=true;load();}
    public void focusInvoice(String id){
        boolean wasBusy=busy;busy=true;view.filterInvoice(id);busy=wasBusy;selectionAfterLoad=id;load();
    }
    public HoaDonController(HoaDonPanel view, DesktopSession session, HoaDonService service) {
        this.view = view; this.session = session; this.service = service;
        view.onSearch(new ActionListener() { public void actionPerformed(ActionEvent event) { load(); } });
        view.onReload(new ActionListener() { public void actionPerformed(ActionEvent event) { load(); } });
        view.onPay(new ActionListener(){public void actionPerformed(ActionEvent event){if(!busy)view.paySelected();}});
        view.onRequestCancellation(new ActionListener(){public void actionPerformed(ActionEvent event){if(!busy)view.requestCancellation();}});
        view.onCreate(new ActionListener() { public void actionPerformed(ActionEvent event) {
            if (busy || !session.isActive() || !service.creationConfigured()) return;
            TaoHoaDonDialog winDialog=new TaoHoaDonDialog(javax.swing.SwingUtilities.getWindowAncestor(view));
            new TaoHoaDonController(winDialog,session,service,new TaoHoaDonController.Saved() {
                public void accept(String id) {
                    busy=true;view.filterInvoice(id);busy=false;createdId=id;load();
                }
            });
            winDialog.setVisible(true);
        } });
        view.onSelect(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) { if (!event.getValueIsAdjusting() && !busy) detail(); }
        });
        invalidated=new Runnable(){public void run(){load();}};BillingChangeBus.forSession(session).subscribe(invalidated);
    }
    public void load() {
        if (busy) { reloadPending=true;return; }
        if (!session.isActive()) { view.expired(); return; }
        final String keyword = view.keyword(); final TrangThaiHoaDon filter = view.statusFilter();
        final LocalDate from, to;
        try { from = date(view.fromText()); to = date(view.toText()); }
        catch (IllegalArgumentException failure) { view.status(failure.getMessage()); return; }
        busy = true; view.setBusy(true); view.clearDetail(); view.status("Đang đọc hóa đơn...");
        final boolean followingPayment=afterPayment;
        new SessionWorker<List<HoaDon>>(session) {
            protected List<HoaDon> doInBackground() throws Exception {
                List<HoaDon> found=service.find(keyword, from, to, filter);
                return view.outstandingOnly()?com.quanlynhakhoa.billing.service.DebtService.outstanding(found):found;
            }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<HoaDon> rows) {
                // Keep selection listeners idle while replacing the model.
                busy = true; view.showRows(rows); busy = false;
                view.status((followingPayment?"Đã thu tiền thành công. ":"")+(rows.isEmpty() ? "Không có hóa đơn phù hợp." : rows.size() + " hóa đơn. Chọn một dòng để xem."));
                if(followingPayment&&!reloadPending)afterPayment=false;
                if (createdId != null) { String id=createdId;createdId=null;view.selectInvoice(id); }
                if(selectionAfterLoad!=null&&keyword.trim().equals(selectionAfterLoad)){String id=selectionAfterLoad;selectionAfterLoad=null;view.selectInvoice(id);}
                if(reloadPending){reloadPending=false;load();}
            }
            protected void failed(Throwable failure) { view.status((afterPayment?"Đã thu tiền thành công, nhưng tải lại thất bại. ":createdId == null ? "Tải thất bại; danh sách trước đó chưa được cập nhật. " : "Đã lưu hóa đơn " + createdId + ", nhưng tải lại thất bại. ") + BillingUi.error(failure)); }
        }.execute();
    }
    private void detail() {
        if (!session.isActive()) { view.expired(); return; }
        final String id = view.selectedId();
        view.clearDetail();
        if (id == null) return;
        busy = true; view.setBusy(true); view.status("Đang đọc chi tiết và lịch sử thu...");
        new SessionWorker<ChiTietBilling>(session) {
            protected ChiTietBilling doInBackground() throws Exception { return service.detail(id); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(ChiTietBilling value) {
                // Ignore programmatic selection changes while a request is running.
                if (!id.equals(view.selectedId())) { detail(); return; }
                view.showDetail(value);
                view.status(id + ": " + value.chiTiet().size() + " dòng, " + value.thanhToan().size() + " lần thu."
                        + (value.chiTiet().isEmpty() ? " Hóa đơn chưa có chi tiết." : ""));
                if(reloadPending){reloadPending=false;load();}
            }
            protected void failed(Throwable failure) { view.status(BillingUi.error(failure));if(reloadPending){reloadPending=false;load();} }
        }.execute();
    }
    private static LocalDate date(String text) {
        if (text == null || text.isBlank()) return null;
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException failure) { throw new IllegalArgumentException("Ngày phải có dạng yyyy-MM-dd và tồn tại; để trống nếu không lọc."); }
    }
}
