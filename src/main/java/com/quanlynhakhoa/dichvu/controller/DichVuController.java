package com.quanlynhakhoa.dichvu.controller;

import com.quanlynhakhoa.billing.controller.SessionWorker;
import com.quanlynhakhoa.billing.ui.BillingUi;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.event.*;
import java.util.List;
import javax.swing.event.*;

public final class DichVuController {
    private final DichVuPanel view;
    private final DesktopSession session;
    private final DichVuService service;
    private boolean busy;
    private boolean reloadPending;
    public DichVuController(DichVuPanel view, DesktopSession session, DichVuService service) {
        this.view = view; this.session = session; this.service = service;
        view.onSearch(new ActionListener() { public void actionPerformed(ActionEvent event) { load(false); } });
        view.onReload(new ActionListener() { public void actionPerformed(ActionEvent event) { load(false); } });
        view.onNew(new ActionListener() { public void actionPerformed(ActionEvent event) {
            if (!busy) { view.clearForm(); view.focusNewCode(); view.status("Nhập dịch vụ mới rồi bấm Lưu."); }
        } });
        view.onClear(new ActionListener() { public void actionPerformed(ActionEvent event) {
            if (!busy) { view.clearForm(); view.status("Đã xóa trắng form."); }
        } });
        view.onSave(new ActionListener() { public void actionPerformed(ActionEvent event) { save(null); } });
        view.onDeactivate(new ActionListener() { public void actionPerformed(ActionEvent event) { save(false); } });
        view.onActivate(new ActionListener() { public void actionPerformed(ActionEvent event) { save(true); } });
        view.onSelect(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) { if (!busy && !event.getValueIsAdjusting()) view.selectRow(); }
        });
    }
    public void load(final boolean afterSave) {
        if (busy) {reloadPending=true;return;}
        if (!session.isActive()) { view.expired(); return; }
        final String keyword = view.keyword(); final Boolean active = view.activeFilter();
        busy = true; view.setBusy(true);
        view.status(afterSave ? "Đã lưu thành công. Đang tải lại..." : "Đang đọc database...");
        new SessionWorker<List<DichVu>>(session) {
            protected List<DichVu> doInBackground() throws Exception { return service.find(keyword, active); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<DichVu> values) {
                view.showRows(values);
                view.status((afterSave ? "Đã lưu và tải lại. " : "") + (values.isEmpty() ? keyword.isBlank()&&active==null?"Chưa có dịch vụ. Bấm Thêm mới để bắt đầu.":"Không tìm thấy dịch vụ phù hợp." : values.size() + " dịch vụ."));
                if(reloadPending){reloadPending=false;load(false);}
            }
            protected void failed(Throwable failure) {
                view.status((afterSave ? "Đã lưu thành công, nhưng tải lại thất bại. " : "Tải thất bại. ") + BillingUi.error(failure));
            }
        }.execute();
    }
    private void save(final Boolean activeOnly) {
        if (busy) return;
        if (!session.isActive()) { view.expired(); return; }
        final boolean creating = view.creating();
        final DichVu value;
        try {
            if (activeOnly != null) {
                if (view.selected() == null) return;
                value = view.selected();
            } else value = DichVuService.validate(view.input());
        } catch (RuntimeException failure) { view.status(BillingUi.error(failure)); return; }
        busy = true; view.setBusy(true); view.status("Đang lưu dịch vụ và nhật ký...");
        new SessionWorker<Void>(session) {
            protected Void doInBackground() throws Exception {
                if (activeOnly == null) {
                    if(view.editorMode()==DichVuPanel.EditorMode.SERVICES)service.saveInformation(value,creating);
                    else if(view.editorMode()==DichVuPanel.EditorMode.PRICES)service.savePrice(value);
                    else service.save(value, creating);
                }
                else service.changeActive(value.maDichVu(), activeOnly);
                return null;
            }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(Void ignored) { saved(); }
            private void saved() {
                view.showForm(new DichVu(value.maDichVu(), value.tenDichVu(), value.donGia(), value.moTa(),
                        value.laCanLamSang(), activeOnly == null ? value.dangApDung() : activeOnly));
                load(true);
                com.quanlynhakhoa.billing.controller.BillingChangeBus.forSession(session).publish();
            }
            protected void failed(Throwable failure) {
                if (failure instanceof DichVuService.CommittedWriteException) { saved(); return; }
                view.status(BillingUi.error(failure) + " Nếu lỗi kết nối khi ghi: tải lại xác minh trước khi lưu lại; không tự thử lại.");
            }
        }.execute();
    }
}
