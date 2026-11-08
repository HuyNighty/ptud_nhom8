package com.quanlynhakhoa.nghiepvu.kho.lohang.controller;

import com.quanlynhakhoa.nghiepvu.kho.lohang.model.LoHang;
import com.quanlynhakhoa.nghiepvu.kho.lohang.service.LoHangService;
import com.quanlynhakhoa.nghiepvu.kho.lohang.view.LoHangPanel;
import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import javax.swing.SwingWorker;

/** Controller điều phối truy vấn lô hàng; mọi JDBC chạy ngoài Swing EDT. */
public final class LoHangController implements ActionListener {
    private final LoHangPanel view;
    private final LoHangService service;
    private boolean dangTai;

    public LoHangController(LoHangPanel view, LoHangService service) {
        this.view = view;
        this.service = service;
        view.getBtnTim().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        taiDanhSach();
    }

    @Override
    public void actionPerformed(ActionEvent event) { taiDanhSach(); }

    private void taiDanhSach() {
        if (dangTai) return;
        dangTai = true;
        view.datDangTai(true);
        String keyword = view.getTuKhoa();
        String kind = view.getLoaiLoc();
        String expiry = view.getHanLoc();
        new SwingWorker<List<LoHang>, Void>() {
            @Override protected List<LoHang> doInBackground() throws Exception {
                return service.timKiem(keyword, kind, expiry);
            }
            @Override protected void done() {
                dangTai = false;
                view.datDangTai(false);
                try {
                    view.hienDanhSach(get());
                } catch (Exception error) {
                    view.hienLoi(thongBaoLoi(error));
                }
            }
        }.execute();
    }

    private String thongBaoLoi(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) root = root.getCause();
        return switch (root) {
            case AccessDeniedException accessDeniedException -> "Tài khoản không có quyền xem lô hàng.";
            case IllegalArgumentException illegalArgumentException -> root.getMessage();
            case SQLException throwables -> "Không thể đọc lô hàng. Kiểm tra kết nối và quyền SELECT M1.";
            default -> "Có lỗi khi tải lô hàng. Vui lòng thử lại.";
        };
    }
}
