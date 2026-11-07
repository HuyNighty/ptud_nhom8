package com.quanlynhakhoa.nghiepvu.kho.workflow.view;

import com.quanlynhakhoa.nghiepvu.kho.workflow.service.InventoryWorkflowService;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

final class InventoryWorkflowController {
    private final InventoryWorkflowPanel view;
    private final InventoryWorkflowService service;

    InventoryWorkflowController(InventoryWorkflowPanel view, InventoryWorkflowService service) {
        this.view = view;
        this.service = service;
        view.receipt().save().addActionListener(event -> saveReceipt());
        view.receipt().refresh().addActionListener(event -> loadReceipt());
        view.issue().save().addActionListener(event -> saveIssue());
        view.issue().refresh().addActionListener(event -> loadIssue());
        view.stocktake().save().addActionListener(event -> saveStocktake());
        view.stocktake().refresh().addActionListener(event -> loadStocktake());
        view.stocktake().approve().addActionListener(event -> approveStocktake());
        view.stocktake().reject().addActionListener(event -> rejectStocktake());
        view.tabs().addChangeListener(event -> loadSelected());
        loadSelected();
    }

    private void loadSelected() {
        switch (view.tabs().getSelectedIndex()) {
            case 0 -> loadReceipt();
            case 1 -> loadIssue();
            case 2 -> loadStocktake();
            default -> { }
        }
    }

    private void loadReceipt() {
        async(service::danhSachNhap, rows -> fill(view.receipt().data(), rows));
    }

    private void loadIssue() {
        async(service::danhSachXuat, rows -> fill(view.issue().data(), rows));
    }

    private void loadStocktake() {
        async(service::danhSachKiemKe, rows -> fill(view.stocktake().data(), rows));
    }

    private void saveReceipt() {
        try {
            var f = view.receipt();
            int qty = Integer.parseInt(f.soLuong().getText().trim());
            long price = Long.parseLong(f.donGia().getText().trim());
            LocalDate made = dateOrNull(f.ngaySX().getText());
            LocalDate expiry = dateOrNull(f.hanSD().getText());
            String id = f.maPhieu().getText(), lot = f.maLo().getText(), batch = f.soLo().getText();
            String supplier = f.maNcc().getText(), type = (String) f.loai().getSelectedItem(), item = f.maHang().getText(), note = f.ghiChu().getText();
            async(() -> {
                service.taoPhieuNhap(id, lot, batch, supplier, type, item, made, expiry, qty, price, note);
                return null;
            }, x -> { JOptionPane.showMessageDialog(view, "Đã tạo phiếu nhập và cập nhật tồn kho."); loadReceipt(); });
        } catch (RuntimeException error) {
            showError(error);
        }
    }

    private void saveIssue() {
        try {
            var f = view.issue();
            int qty = Integer.parseInt(f.soLuong().getText().trim());
            String id = f.maPhieu().getText(), lot = f.maLo().getText(), dept = f.boPhan().getText(), receiver = f.nguoiNhan().getText(), reason = f.lyDo().getText();
            async(() -> {
                service.taoPhieuXuat(id, lot, dept, receiver, reason, qty);
                return null;
            }, x -> { JOptionPane.showMessageDialog(view, "Đã tạo phiếu xuất và cập nhật tồn kho."); loadIssue(); });
        } catch (RuntimeException error) {
            showError(error);
        }
    }

    private void saveStocktake() {
        try {
            var f = view.stocktake();
            int actual = Integer.parseInt(f.actual().getText().trim());
            String id = f.maPhieu().getText(), lot = f.maLo().getText(), reason = f.reason().getText();
            async(() -> {
                service.guiKiemKe(id, lot, actual, reason);
                return null;
            }, x -> { JOptionPane.showMessageDialog(view, "Đã gửi yêu cầu kiểm kê."); loadStocktake(); });
        } catch (RuntimeException error) {
            showError(error);
        }
    }

    private void approveStocktake() {
        String id = view.stocktake().selectedId();
        if (id == null) { showError(new IllegalArgumentException("Chọn phiếu kiểm kê cần duyệt.")); return; }
        int choice = JOptionPane.showConfirmDialog(view, "Duyệt phiếu " + id + "? Tồn kho sẽ được điều chỉnh nếu dữ liệu chưa thay đổi.",
                "Xác nhận điều chỉnh kho", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) return;
        async(() -> { service.duyetKiemKe(id); return null; }, x -> { JOptionPane.showMessageDialog(view, "Đã duyệt kiểm kê và cập nhật tồn kho."); loadStocktake(); });
    }

    private void rejectStocktake() {
        String id = view.stocktake().selectedId();
        if (id == null) { showError(new IllegalArgumentException("Chọn phiếu kiểm kê cần từ chối.")); return; }
        String reason = JOptionPane.showInputDialog(view, "Lý do từ chối phiếu " + id + ":");
        if (reason == null) return;
        async(() -> { service.tuChoiKiemKe(id, reason); return null; }, x -> { JOptionPane.showMessageDialog(view, "Đã từ chối phiếu kiểm kê."); loadStocktake(); });
    }
    private static LocalDate dateOrNull(String text) {
        String s = text == null ? "" : text.trim();
        return s.isEmpty() ? null : LocalDate.parse(s);
    }

    private static void fill(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) model.addRow(row);
    }

    private <T> void async(Callable<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (Exception error) {
                    Throwable root = error;
                    while (root.getCause() != null) root = root.getCause();
                    showError(root);
                }
            }
        }.execute();
    }

    private void showError(Throwable error) {
        JOptionPane.showMessageDialog(view, error.getMessage() == null ? "Không thể xử lý yêu cầu." : error.getMessage(), "Nghiệp vụ kho", JOptionPane.ERROR_MESSAGE);
    }
}