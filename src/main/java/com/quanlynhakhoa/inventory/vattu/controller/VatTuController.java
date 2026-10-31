package com.quanlynhakhoa.inventory.vattu.controller;

import com.quanlynhakhoa.inventory.vattu.model.VatTu;
import com.quanlynhakhoa.inventory.vattu.service.VatTuService;
import com.quanlynhakhoa.inventory.vattu.view.VatTuDialog;
import com.quanlynhakhoa.inventory.vattu.view.VatTuPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.Window;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Controller nối sự kiện View với Service; tác vụ JDBC chạy nền. */
public final class VatTuController implements ActionListener {
    private final VatTuPanel view;
    private final VatTuService service;
    private VatTuDialog dialog;
    private boolean dangTai;

    public VatTuController(VatTuPanel view, VatTuService service) {
        this.view = view;
        this.service = service;
        view.getBtnTim().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnThem().addActionListener(this);
        view.getBtnSua().addActionListener(this);
        view.getTblVatTu().getSelectionModel().addListSelectionListener(event -> {
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
            VatTu selected = view.layVatTuDangChon();
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
        new SwingWorker<List<VatTu>, Void>() {
            @Override protected List<VatTu> doInBackground() throws Exception {
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

    private void moForm(VatTu vatTuCanSua) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        VatTuDialog form = new VatTuDialog(owner, vatTuCanSua);
        dialog = form;
        form.getBtnLuu().addActionListener(this);
        form.getBtnHuy().addActionListener(this);
        form.setVisible(true);
        dialog = null;
    }

    private void luu() {
        final VatTuDialog form = dialog;
        final VatTu vatTu;
        try {
            vatTu = form.layVatTuTuForm();
        } catch (RuntimeException error) {
            form.hienLoi(thongBaoLoi(error));
            return;
        }
        final boolean dangSua = form.getTitle().startsWith("Cập nhật");
        form.datDangLuu(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (dangSua) service.capNhat(vatTu);
                else service.them(vatTu);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    form.dispose();
                    if (dialog == form) dialog = null;
                    view.setTrangThai(dangSua ? "Đã cập nhật vật tư " + vatTu.getMaVatTu() + "."
                            : "Đã thêm vật tư " + vatTu.getMaVatTu() + ".", new java.awt.Color(40, 120, 65));
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
        while (root.getCause() != null) root = root.getCause();
        if (root instanceof AccessDeniedException) {
            return "Tài khoản không có quyền xem hoặc quản lý vật tư.";
        }
        if (root instanceof IllegalArgumentException) return root.getMessage();
        if (root instanceof SQLException) {
            return "Không thể đọc/lưu vật tư. Kiểm tra kết nối và quyền M1 trên database development.";
        }
        return "Có lỗi khi xử lý vật tư. Vui lòng thử lại.";
    }
}
