package com.quanlynhakhoa.nghiepvu.kho.supplyrequest.view;

import com.quanlynhakhoa.nghiepvu.kho.supplyrequest.service.SupplyRequestService;
import com.quanlynhakhoa.model.kho.PhanBoLo;
import com.quanlynhakhoa.model.kho.DongYeuCauCungUng;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

final class SupplyRequestController {
    private final SupplyRequestPanel view;
    private final SupplyRequestService service;

    SupplyRequestController(SupplyRequestPanel view, SupplyRequestService service) {
        this.view = view;
        this.service = service;
        if (view.isManagerMode()) {
            view.refreshManager.addActionListener(e -> loadManager());
            view.approve.addActionListener(e -> approve());
            view.reject.addActionListener(e -> reject());
            view.issue.addActionListener(e -> issue());
            view.managerTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) loadSelectedDetails();
            });
            loadManager();
        } else {
            view.addLine.addActionListener(e -> addLine());
            view.removeLine.addActionListener(e -> removeLine());
            view.create.addActionListener(e -> create());
            view.refreshMine.addActionListener(e -> loadMine());
            view.tabs().addChangeListener(e -> {
                if (view.tabs().getSelectedIndex() == 1) loadMine();
            });
            loadMine();
        }
    }

    private void addLine() {
        try {
            String type = (String) view.itemType.getSelectedItem(), code = view.itemCode.getText().trim().toUpperCase();
            int qty = Integer.parseInt(view.quantity.getText().trim());
            if (code.isBlank() || qty <= 0) throw new IllegalArgumentException("Nhập mã mặt hàng và số lượng lớn hơn 0.");
            view.draftModel.addRow(new Object[]{type, code, qty});
            view.itemCode.setText("");
            view.quantity.setText("1");
        } catch (RuntimeException error) {
            view.showError(error);
        }
    }

    private void removeLine() {
        int row = view.draftTable.getSelectedRow();
        if (row >= 0) view.draftModel.removeRow(view.draftTable.convertRowIndexToModel(row));
    }

    private void create() {
        List<DongYeuCauCungUng> lines = new ArrayList<>();
        for (int i = 0; i < view.draftModel.getRowCount(); i++) {
            lines.add(new DongYeuCauCungUng(String.valueOf(view.draftModel.getValueAt(i, 0)), String.valueOf(view.draftModel.getValueAt(i, 1)), Integer.parseInt(String.valueOf(view.draftModel.getValueAt(i, 2)))));
        }
        String id = view.requestId.getText(), reason = view.reason.getText();
        async(() -> {
            service.taoYeuCau(id, reason, lines);
            return null;
        }, ignored -> {
            view.draftModel.setRowCount(0);
            view.requestId.setText("");
            view.reason.setText("");
            loadMine();
            JOptionPane.showMessageDialog(view, "Đã gửi yêu cầu cấp phát.");
        });
    }

    private void loadMine() {
        async(service::danhSachCuaToi, rows -> fill(view.mineModel, rows));
    }

    private void loadManager() {
        async(service::danhSachChoKho, rows -> fill(view.managerModel, rows));
    }

    private void loadSelectedDetails() {
        String id = view.selectedManagerId();
        if (id == null) {
            view.detailModel.setRowCount(0);
            return;
        }
        async(() -> service.chiTiet(id), rows -> fill(view.detailModel, rows));
    }

    private void approve() {
        String id = view.selectedManagerId();
        if (id == null) {
            view.showError(new IllegalArgumentException("Chọn yêu cầu cần duyệt."));
            return;
        }
        async(() -> {
            service.duyet(id);
            return null;
        }, x -> {
            loadManager();
            JOptionPane.showMessageDialog(view, "Đã duyệt yêu cầu.");
        });
    }

    private void reject() {
        String id = view.selectedManagerId();
        if (id == null) {
            view.showError(new IllegalArgumentException("Chọn yêu cầu cần từ chối."));
            return;
        }
        String reason = JOptionPane.showInputDialog(view, "Lý do từ chối:", "Từ chối yêu cầu", JOptionPane.QUESTION_MESSAGE);
        if (reason == null) return;
        async(() -> {
            service.tuChoi(id, reason);
            return null;
        }, x -> {
            loadManager();
            JOptionPane.showMessageDialog(view, "Đã từ chối yêu cầu.");
        });
    }

    private void issue() {
        try {
            String id = view.selectedManagerId();
            if (id == null) throw new IllegalArgumentException("Chọn yêu cầu cần cấp phát.");
            List<PhanBoLo> allocations = new ArrayList<>();
            for (int row = 0; row < view.detailModel.getRowCount(); row++) {
                int line = Integer.parseInt(String.valueOf(view.detailModel.getValueAt(row, 0)));
                Object lotValue = view.detailModel.getValueAt(row, 5);
                String lot = lotValue == null ? "" : lotValue.toString().trim();
                allocations.add(new PhanBoLo(line, lot));
            }
            String voucher = view.voucherId.getText(), dept = view.department.getText(), receiver = view.receiver.getText(), reason = view.issueReason.getText();
            async(() -> {
                service.capPhat(id, voucher, dept, receiver, reason, allocations);
                return null;
            }, x -> {
                loadManager();
                view.detailModel.setRowCount(0);
                JOptionPane.showMessageDialog(view, "Đã ghi phiếu xuất và cập nhật tồn kho.");
            });
        } catch (RuntimeException error) {
            view.showError(error);
        }
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
                } catch (Exception e) {
                    Throwable r = e;
                    while (r.getCause() != null) r = r.getCause();
                    view.showError(r);
                }
            }
        }.execute();
    }
}
