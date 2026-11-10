package com.quanlynhakhoa.nghiepvu.hoadon.controller;







import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.InvoiceService;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.*;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.event.*;

public final class InvoiceController {
    private final InvoicePanel view;
    private final DesktopSession session;
    private final InvoiceService service;
    private boolean busy;
    private int page;
    private String createdId;
    private final Runnable invalidated;
    private boolean reloadPending;
    private String selectionAfterLoad;
    private boolean afterPayment;
    public void paymentSaved(){afterPayment=true;load();}
    public void focusInvoice(String id){
        boolean wasBusy=busy;busy=true;view.filterInvoice(id);busy=wasBusy;selectionAfterLoad=id;page=0;load();
    }
    public InvoiceController(InvoicePanel view, DesktopSession session, InvoiceService service) {
        this.view = view; this.session = session; this.service = service;
        view.onSearch(new ActionListener() { public void actionPerformed(ActionEvent event) { if(busy)return;page=0;load(); } });
        view.onReload(new ActionListener() { public void actionPerformed(ActionEvent event) { if(busy)return; busy=true; view.filterInvoice(""); busy=false; selectionAfterLoad=null; page=0;load(); } });
        view.btnTrangTruoc.addActionListener(e->{if(!busy&&page>0){page--;load();}});
        view.btnTrangSau.addActionListener(e->{if(!busy){page++;load();}});
        view.onPay(new ActionListener(){public void actionPerformed(ActionEvent event){if(!busy)view.paySelected();}});
        view.onRequestCancellation(new ActionListener(){public void actionPerformed(ActionEvent event){if(!busy)view.requestCancellation();}});
        view.onCreate(new ActionListener() { public void actionPerformed(ActionEvent event) {
            if (busy || !session.isActive() || !service.creationConfigured()) return;
            InvoiceCreationDialog winDialog=new InvoiceCreationDialog(javax.swing.SwingUtilities.getWindowAncestor(view));
            new InvoiceCreationController(winDialog,session,service,new InvoiceCreationController.Saved() {
                public void accept(String id) {
                    busy=true;view.filterInvoice(id);busy=false;createdId=id;page=0;load();
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
        final String keyword = view.keyword(); final InvoiceStatus filter = view.statusFilter();
        final LocalDate from, to;
        try { from = date(view.fromText()); to = date(view.toText()); }
        catch (IllegalArgumentException failure) { view.status(failure.getMessage()); return; }
        busy = true; view.setBusy(true); view.clearDetail(); view.status("Đang đọc hóa đơn...");
        final boolean followingPayment=afterPayment;
        new SessionWorker<List<Invoice>>(session) {
            protected List<Invoice> doInBackground() throws Exception {
                List<Invoice> found=service.findPage(keyword, from, to, filter,page);
                return view.outstandingOnly()?com.quanlynhakhoa.nghiepvu.hoadon.service.DebtService.outstanding(found):found;
            }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<Invoice> rows) {
                // Keep selection listeners idle while replacing the model.
                boolean next=rows.size()>100;
                busy = true; view.showRows(next?rows.subList(0,100):rows); busy = false;view.pageState(page,next);
                view.status((followingPayment?"Đã thu tiền thành công. ":"")+(rows.isEmpty() ? keyword.isBlank()&&from==null&&to==null&&filter==null?"Chưa có hóa đơn.":"Không tìm thấy hóa đơn phù hợp." : ""));
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
        new SessionWorker<InvoiceDetails>(session) {
            protected InvoiceDetails doInBackground() throws Exception { return service.detail(id); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(InvoiceDetails value) {
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
        return BillingUi.parseDate(text);
    }
}
