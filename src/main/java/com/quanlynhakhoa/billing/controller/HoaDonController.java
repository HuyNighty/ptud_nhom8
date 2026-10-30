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
    public HoaDonController(HoaDonPanel view, DesktopSession session, HoaDonService service) {
        this.view = view; this.session = session; this.service = service;
        view.onSearch(new ActionListener() { public void actionPerformed(ActionEvent event) { load(); } });
        view.onSelect(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) { if (!event.getValueIsAdjusting() && !busy) detail(); }
        });
    }
    public void load() {
        if (busy) return;
        if (!session.isActive()) { view.expired(); return; }
        final String keyword = view.keyword(); final TrangThaiHoaDon filter = view.statusFilter();
        final LocalDate from, to;
        try { from = date(view.fromText()); to = date(view.toText()); }
        catch (IllegalArgumentException failure) { view.status(failure.getMessage()); return; }
        busy = true; view.setBusy(true); view.clearDetail(); view.status("Đang đọc hóa đơn...");
        new SessionWorker<List<HoaDon>>(session) {
            protected List<HoaDon> doInBackground() throws Exception { return service.find(keyword, from, to, filter); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<HoaDon> rows) {
                // Keep selection listeners idle while replacing the model.
                busy = true; view.showRows(rows); busy = false;
                view.status(rows.isEmpty() ? "Không có hóa đơn phù hợp." : rows.size() + " hóa đơn. Chọn một dòng để xem.");
            }
            protected void failed(Throwable failure) { view.status("Tải thất bại; danh sách trước đó chưa được cập nhật. " + BillingUi.error(failure)); }
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
            }
            protected void failed(Throwable failure) { view.status(BillingUi.error(failure)); }
        }.execute();
    }
    private static LocalDate date(String text) {
        if (text == null || text.isBlank()) return null;
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException failure) { throw new IllegalArgumentException("Ngày phải có dạng yyyy-MM-dd và tồn tại; để trống nếu không lọc."); }
    }
}
