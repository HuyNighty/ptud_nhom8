package com.quanlynhakhoa.inventory.nhacungcap.controller;

import com.quanlynhakhoa.inventory.nhacungcap.model.NhaCungCap;
import com.quanlynhakhoa.inventory.nhacungcap.service.NhaCungCapService;
import com.quanlynhakhoa.inventory.nhacungcap.view.NhaCungCapDialog;
import com.quanlynhakhoa.inventory.nhacungcap.view.NhaCungCapPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Controller điều phối UI và Service; không viết SQL hoặc validate nghiệp vụ. */
public final class NhaCungCapController implements ActionListener {
    private final NhaCungCapPanel view;
    private final NhaCungCapService service;
    private NhaCungCapDialog dialog;
    private boolean dangTai;

    public NhaCungCapController(NhaCungCapPanel view, NhaCungCapService service) {
        this.view = view;
        this.service = service;
        view.getBtnTim().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnThem().addActionListener(this);
        view.getBtnSua().addActionListener(this);
        view.getBtnDoiTrangThai().addActionListener(this);
        view.getTblNhaCungCap().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) view.datQuyen(service.coQuyen(Permission.INVENTORY_MANAGE));
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
            NhaCungCap selected = view.layNhaCungCapDangChon();
            if (selected != null) moForm(selected);
        } else if (source == view.getBtnDoiTrangThai()) {
            doiTrangThai();
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
        String status = view.getTrangThaiLoc();
        new SwingWorker<List<NhaCungCap>, Void>() {
            @Override protected List<NhaCungCap> doInBackground() throws Exception {
                return service.timKiem(keyword, status);
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

    private void moForm(NhaCungCap nccCanSua) {
        Window owner = SwingUtilities.getWindowAncestor(view);
        NhaCungCapDialog form = new NhaCungCapDialog(owner, nccCanSua);
        dialog = form;
        form.getBtnLuu().addActionListener(this);
        form.getBtnHuy().addActionListener(this);
        form.setVisible(true);
        dialog = null;
    }

    private void luu() {
        NhaCungCapDialog form = dialog;
        NhaCungCap ncc;
        try {
            ncc = form.layNhaCungCapTuForm();
        } catch (RuntimeException error) {
            form.hienLoi(thongBaoLoi(error));
            return;
        }
        boolean edit = form.getTitle().startsWith("Cập nhật");
        form.datDangLuu(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (edit) service.capNhatThongTin(ncc);
                else service.them(ncc);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    form.dispose();
                    if (dialog == form) dialog = null;
                    view.setTrangThai(edit ? "Đã cập nhật nhà cung cấp " + ncc.getMaNhaCungCap() + "."
                            : "Đã thêm nhà cung cấp " + ncc.getMaNhaCungCap() + ".", new Color(40, 120, 65));
                    taiDanhSach();
                } catch (Exception error) {
                    form.datDangLuu(false);
                    form.hienLoi(thongBaoLoi(error));
                }
            }
        }.execute();
    }

    private void doiTrangThai() {
        NhaCungCap selected = view.layNhaCungCapDangChon();
        if (selected == null) return;
        boolean stopping = NhaCungCap.DANG_HOP_TAC.equals(selected.getTrangThai());
        String next = stopping ? NhaCungCap.NGUNG_HOP_TAC : NhaCungCap.DANG_HOP_TAC;
        String action = stopping ? "ngừng hợp tác" : "khôi phục hợp tác";
        int choice = JOptionPane.showConfirmDialog(view,
                "Bạn có chắc muốn " + action + " với nhà cung cấp " + selected.getTenNhaCungCap()
                        + " (" + selected.getMaNhaCungCap() + ")? Dữ liệu và tham chiếu lịch sử sẽ được giữ nguyên.",
                "Xác nhận thay đổi trạng thái", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        view.datDangTai(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.capNhatTrangThai(selected.getMaNhaCungCap(), next);
                return null;
            }
            @Override protected void done() {
                view.datDangTai(false);
                try {
                    get();
                    view.setTrangThai("Đã " + action + " với " + selected.getTenNhaCungCap() + ".",
                            new Color(40, 120, 65));
                    taiDanhSach();
                } catch (Exception error) {
                    view.hienLoi(thongBaoLoi(error));
                }
            }
        }.execute();
    }

    private String thongBaoLoi(Throwable error) {
        Throwable root = error;
        while ((root instanceof ExecutionException || root.getCause() != null) && root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof AccessDeniedException) return "Tài khoản không có quyền xem hoặc quản lý nhà cung cấp.";
        if (root instanceof IllegalArgumentException) return root.getMessage();
        if (root instanceof SQLException) {
            return "Không thể đọc/lưu nhà cung cấp. Kiểm tra kết nối và quyền M1 trên database development.";
        }
        return "Có lỗi khi xử lý nhà cung cấp. Vui lòng thử lại.";
    }
}
