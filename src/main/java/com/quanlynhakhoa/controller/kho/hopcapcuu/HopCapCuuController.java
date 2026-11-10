package com.quanlynhakhoa.controller.kho.hopcapcuu;

import com.quanlynhakhoa.view.kho.hopcapcuu.HopCapCuuPanel;
import com.quanlynhakhoa.service.kho.hopcapcuu.HopCapCuuService;
import com.quanlynhakhoa.model.kho.DongKiemTraCapCuu;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public final class HopCapCuuController {
    private final HopCapCuuPanel view;
    private final HopCapCuuService service;

    public HopCapCuuController(HopCapCuuPanel view, HopCapCuuService service) {
        this.view = view;
        this.service = service;
        view.refreshCatalog().addActionListener(e -> loadCatalog());
        view.refreshHistory().addActionListener(e -> loadHistory());
        view.save().addActionListener(e -> save());
        view.history().getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadSelectedDetail(); });
        view.tabs().addChangeListener(e -> {
            if (view.tabs().getSelectedIndex() == 0) loadCatalog();
            else loadHistory();
        });
        loadCatalog();
    }

    private void loadCatalog() {
        async(service::danhMuc, rows -> {
            view.catalogModel().setRowCount(0);
            for (Object[] row : rows) {
                Object[] display = {row[0], row[1], row[2], row[3], row[4], row[5], "", "", row[8]};
                view.catalogModel().addRow(display);
            }
        });
    }

    private void loadHistory() {
        async(service::lichSu, rows -> fill(view.historyModel(), rows));
    }

    private void loadSelectedDetail() {
        int row = view.history().getSelectedRow();
        if (row < 0) { view.detailModel().setRowCount(0); return; }
        String id = String.valueOf(view.history().getValueAt(view.history().convertRowIndexToModel(row), 0));
        async(() -> service.chiTiet(id), rows -> fill(view.detailModel(), rows));
    }

    private void save() {
        try {
            if (view.catalogModel().getRowCount() == 0) throw new IllegalArgumentException("Danh mục kiểm tra đang trống.");
            List<DongKiemTraCapCuu> lines = new ArrayList<>();
            for (int i = 0; i < view.catalogModel().getRowCount(); i++) {
                String code = String.valueOf(view.catalogModel().getValueAt(i, 0));
                Object qtyValue = view.catalogModel().getValueAt(i, 6);
                if (qtyValue == null || qtyValue.toString().isBlank()) throw new IllegalArgumentException("Nhập số lượng thực tế cho mọi mục trước khi lưu.");
                int qty = Integer.parseInt(qtyValue.toString().trim());
                Object dateValue = view.catalogModel().getValueAt(i, 7);
                String text = dateValue == null ? "" : dateValue.toString().trim();
                LocalDate expiry = text.isEmpty() ? null : LocalDate.parse(text);
                lines.add(new DongKiemTraCapCuu(code, qty, expiry));
            }
            String id = view.checkId().getText(), note = view.note().getText();
            async(() -> {
                service.ghiKiemTra(id, note, lines);
                return null;
            }, x -> {
                JOptionPane.showMessageDialog(view, "Đã lưu lần kiểm tra; kết quả DAT/CAN_BO_SUNG được tính từ định mức và hạn dùng.");
                view.checkId().setText("");
                loadCatalog();
                loadHistory();
            });
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(view, error.getMessage(), "Hộp cấp cứu", JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(view, r.getMessage() == null ? "Không thể xử lý." : r.getMessage(), "Hộp cấp cứu", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
