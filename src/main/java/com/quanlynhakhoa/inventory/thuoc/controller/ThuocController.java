package com.quanlynhakhoa.inventory.thuoc.controller;

import com.quanlynhakhoa.inventory.thuoc.model.Thuoc;
import com.quanlynhakhoa.inventory.thuoc.service.ThuocService;
import com.quanlynhakhoa.inventory.thuoc.view.ThuocDialog;
import com.quanlynhakhoa.inventory.thuoc.view.ThuocPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Controller chỉ điều phối sự kiện View sang Service và trả kết quả về View. */
public final class ThuocController implements ActionListener {
    private final ThuocPanel view;
    private final ThuocService service;
    private ThuocDialog dialog;
    private boolean dangTai;

    public ThuocController(ThuocPanel view, ThuocService service) {
        this.view = view;
        this.service = service;
        view.getBtnTim().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnThem().addActionListener(this);
        view.getBtnSua().addActionListener(this);
        view.getTblThuoc().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                view.datQuyen(service.coQuyen(Permission.INVENTORY_MANAGE));
            }
        });
        view.datQuyen(service.coQuyen(Permission.INVENTORY_MANAGE));
        taiDanhSach();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == view.getBtnTim() || source == view.getTxtTimKiem()
                || source == view.getBtnTaiLai()) {
            taiDanhSach();
        } else if (source == view.getBtnThem()) {
            moForm(null);
        } else if (source == view.getBtnSua()) {
            Thuoc selected = view.layThuocDangChon();
            if (selected != null) moForm(selected);
        } else if (dialog != null && source == dialog.getBtnLuu()) {
            luu();
        } else if (dialog != null && source == dialog.getBtnHuy()) {
            dialog.dispose();
        }
    }

    private void taiDanhSach() {
        if (dangTai) return;
        dangTai = true;
        view.datDangTai(true);
        String keyword = view.getTuKhoa();
        new SwingWorker<List<Thuoc>, Void>() {
            @Override protected List<Thuoc> doInBackground() throws Exception {
                return service.timKiem(keyword);
            }

            @Override protected void done() {
                dangTai = false;
                view.datDangTai(false);
                try {
                    view.hienDanhSach(get());
                    view.datQuyen(service.coQuyen(Permission.INVENTORY_MANAGE));
                } catch (Exception error) {
                    view.hienLoi(thongBaoLoi(error));
                }
            }
        }.execute();
    }

    private void moForm(Thuoc thuocCanSua) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        ThuocDialog form = new ThuocDialog(owner, thuocCanSua);
        dialog = form;
        form.getBtnLuu().addActionListener(this);
        form.getBtnHuy().addActionListener(this);
        form.setVisible(true);
        dialog = null;
    }

    private void luu() {
        final ThuocDialog form = dialog;
        final Thuoc thuoc;
        try {
            thuoc = form.layThuocTuForm();
        } catch (RuntimeException error) {
            form.hienLoi(thongBaoLoi(error));
            return;
        }
        final boolean dangSua = form.getTitle().startsWith("Cập nhật");
        form.datDangLuu(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (dangSua) service.capNhat(thuoc);
                else service.them(thuoc);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    form.dispose();
                    if (dialog == form) dialog = null;
                    view.setTrangThai(dangSua ? "Đã cập nhật thuốc " + thuoc.getMaThuoc() + "."
                            : "Đã thêm thuốc " + thuoc.getMaThuoc() + ".", new java.awt.Color(40, 120, 65));
                    taiDanhSach();
                } catch (Exception error) {
                    form.datDangLuu(false);
                    form.hienLoi(thongBaoLoi(error));
                }
            }
        }.execute();
    }

    private String thongBaoLoi(Throwable error) {
        Throwable root = error;
        while ((root instanceof ExecutionException || root.getCause() != null)
                && root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof AccessDeniedException) {
            return "Tài khoản không có quyền xem hoặc quản lý thuốc.";
        }
        if (root instanceof IllegalArgumentException) return root.getMessage();
        if (root instanceof SQLException) {
            return "Không thể đọc/lưu thuốc. Kiểm tra kết nối và quyền M1 trên database development.";
        }
        return "Có lỗi khi xử lý thuốc. Vui lòng thử lại.";
    }
}
