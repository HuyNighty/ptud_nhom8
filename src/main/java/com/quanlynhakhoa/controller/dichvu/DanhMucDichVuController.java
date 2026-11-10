package com.quanlynhakhoa.controller.dichvu;

import com.quanlynhakhoa.enums.CheDoSuaDanhMucDichVu;
import com.quanlynhakhoa.controller.hoadon.SessionWorker;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.entity.DichVuNhaKhoa;
import com.quanlynhakhoa.service.dichvu.DanhMucDichVuService;
import com.quanlynhakhoa.view.dichvu.DanhMucDichVuPanel;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.event.*;
import java.util.List;
import javax.swing.event.*;

public final class DanhMucDichVuController {
    private final DanhMucDichVuPanel view;
    private final DesktopSession session;
    private final DanhMucDichVuService service;
    private boolean busy;
    private boolean reloadPending;
    public DanhMucDichVuController(DanhMucDichVuPanel view, DesktopSession session, DanhMucDichVuService service) {
        this.view = view; this.session = session; this.service = service;
        view.onSearch(new ActionListener() { public void actionPerformed(ActionEvent event) { load(false); } });
        view.onReload(new ActionListener() { public void actionPerformed(ActionEvent event) { if(busy)return; busy=true; view.clearFilters(); busy=false; reloadPending=false; load(false); } });
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
        final String selectedId = view.selected() == null ? null : view.selected().maDichVu();
        busy = true;
        if (afterSave) {
            // A successful save must remain visible even when the old search excludes it.
            view.clearFilters();
            reloadPending = false;
        }
        final String keyword = view.keyword(); final Boolean active = view.activeFilter();
        view.setBusy(true);
        view.status(afterSave ? "Đã lưu thành công. Đang tải lại..." : "Đang đọc database...");
        new SessionWorker<List<DichVuNhaKhoa>>(session) {
            protected List<DichVuNhaKhoa> doInBackground() throws Exception { return service.find(keyword, active); }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(List<DichVuNhaKhoa> values) {
                view.showRows(values);
                view.selectService(selectedId);
                view.status((afterSave ? "Đã lưu và tải lại. Dịch vụ " + selectedId + ". Đã bỏ bộ lọc. " : "") + (values.isEmpty() ? keyword.isBlank()&&active==null?"Chưa có dịch vụ. Bấm Thêm mới để bắt đầu.":"Không tìm thấy dịch vụ phù hợp." : values.size() + " dịch vụ."));
                if(reloadPending){reloadPending=false;load(false);}
            }
            protected void failed(Throwable failure) {
                view.status((afterSave ? "Đã lưu thành công, nhưng tải lại thất bại. " : "Tải thất bại. ") + GiaoDienHoaDon.error(failure));
            }
        }.execute();
    }
    private void showSaveFailure(Throwable failure) {
        String detail = GiaoDienHoaDon.error(failure);
        if (failure instanceof java.sql.SQLException) {
            view.status("Chưa xác nhận lưu thành công. Dữ liệu nhập vẫn được giữ nguyên. "
                    + "Kiểm tra kết nối/quyền và đối chiếu dữ liệu đã lưu trước khi bấm Lưu lại. "
                    + "Không tự động gửi lại yêu cầu lưu.");
        } else {
            view.status("Chưa lưu. Dữ liệu nhập vẫn được giữ nguyên. " + detail
                    + " Sửa thông tin rồi bấm Lưu lại.");
        }
    }
    private void save(final Boolean activeOnly) {
        if (busy) return;
        if (!session.isActive()) { view.expired(); return; }
        final boolean creating = view.creating();
        final DichVuNhaKhoa value;
        try {
            if (activeOnly != null) {
                if (view.selected() == null) return;
                value = view.selected();
            } else value = DanhMucDichVuService.validate(view.input());
        } catch (RuntimeException failure) { showSaveFailure(failure); return; }
        busy = true; view.setBusy(true); view.status("Đang lưu dịch vụ và nhật ký...");
        new SessionWorker<Void>(session) {
            protected Void doInBackground() throws Exception {
                if (activeOnly == null) {
                    if(view.editorMode()==CheDoSuaDanhMucDichVu.SERVICES)service.saveInformation(value,creating);
                    else if(view.editorMode()==CheDoSuaDanhMucDichVu.PRICES)service.savePrice(value);
                    else service.save(value, creating);
                }
                else service.changeActive(value.maDichVu(), activeOnly);
                return null;
            }
            protected void finished() { busy = false; view.setBusy(false); }
            protected void expired() { view.expired(); }
            protected void succeeded(Void ignored) { saved(); }
            private void saved() {
                view.showForm(new DichVuNhaKhoa(value.maDichVu(), value.tenDichVu(), value.donGia(), value.moTa(),
                        value.laCanLamSang(), activeOnly == null ? value.dangApDung() : activeOnly));
                load(true);
                com.quanlynhakhoa.controller.hoadon.ThongBaoThayDoiHoaDon.forSession(session).publish();
            }
            protected void failed(Throwable failure) {
                if (failure instanceof DanhMucDichVuService.CommittedWriteException) { saved(); return; }
                reloadPending = false;
                showSaveFailure(failure);
            }
        }.execute();
    }
}
