package com.quanlynhakhoa.nghiepvu.dichvu.controller;

import com.quanlynhakhoa.nghiepvu.hoadon.controller.SessionWorker;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.BillingUi;
import com.quanlynhakhoa.nghiepvu.dichvu.model.DentalService;
import com.quanlynhakhoa.nghiepvu.dichvu.service.ServiceCatalogService;
import com.quanlynhakhoa.nghiepvu.dichvu.ui.ServiceCatalogPanel;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.event.*;
import java.util.List;
import javax.swing.event.*;

public final class ServiceCatalogController {
    private final ServiceCatalogPanel view;
    private final DesktopSession session;
    private final ServiceCatalogService service;
    private boolean busy;
    private boolean reloadPending;
    public ServiceCatalogController(ServiceCatalogPanel view, DesktopSession session, ServiceCatalogService service) {
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
        new SessionWorker<List<DentalService>>(session) {
            protected List<DentalService> doInBackground() throws Exception { return service.find(keyword, active); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<DentalService> values) {
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
        final DentalService value;
        try {
            if (activeOnly != null) {
                if (view.selected() == null) return;
                value = view.selected();
            } else value = ServiceCatalogService.validate(view.input());
        } catch (RuntimeException failure) { view.status(BillingUi.error(failure)); return; }
        busy = true; view.setBusy(true); view.status("Đang lưu dịch vụ và nhật ký...");
        new SessionWorker<Void>(session) {
            protected Void doInBackground() throws Exception {
                if (activeOnly == null) {
                    if(view.editorMode()==ServiceCatalogPanel.EditorMode.SERVICES)service.saveInformation(value,creating);
                    else if(view.editorMode()==ServiceCatalogPanel.EditorMode.PRICES)service.savePrice(value);
                    else service.save(value, creating);
                }
                else service.changeActive(value.maDichVu(), activeOnly);
                return null;
            }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(Void ignored) { saved(); }
            private void saved() {
                view.showForm(new DentalService(value.maDichVu(), value.tenDichVu(), value.donGia(), value.moTa(),
                        value.laCanLamSang(), activeOnly == null ? value.dangApDung() : activeOnly));
                load(true);
                com.quanlynhakhoa.nghiepvu.hoadon.controller.BillingChangeBus.forSession(session).publish();
            }
            protected void failed(Throwable failure) {
                if (failure instanceof ServiceCatalogService.CommittedWriteException) { saved(); return; }
                view.status(BillingUi.error(failure) + " Nếu lỗi kết nối khi ghi: tải lại xác minh trước khi lưu lại; không tự thử lại.");
            }
        }.execute();
    }
}
