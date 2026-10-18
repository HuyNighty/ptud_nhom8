package com.quanlynhakhoa.features.examination.ui;

import com.quanlynhakhoa.features.examination.dto.request.DichVuCreateRequest;
import com.quanlynhakhoa.features.examination.dto.request.DichVuUpdateRequest;
import com.quanlynhakhoa.features.examination.dto.response.DichVuResponse;
import com.quanlynhakhoa.features.examination.service.DichVuService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Lazy;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@org.springframework.stereotype.Component
@Lazy
public final class ServicePanel extends JPanel {

    private final DichVuService dichVuService;
    private final JTextField keywordField = new JTextField(22);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"Tất cả trạng thái", "Đang áp dụng", "Ngừng áp dụng"}
    );
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã dịch vụ", "Tên dịch vụ", "Đơn giá", "Cận lâm sàng", "Trạng thái"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Danh mục dịch vụ khám");
    private List<DichVuResponse> visibleServices = List.of();

    public ServicePanel(DichVuService dichVuService) {
        this.dichVuService = dichVuService;
        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("DANH MỤC DỊCH VỤ KHÁM");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(8));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Tìm kiếm:"));
        searchRow.add(keywordField);
        searchRow.add(new JLabel("Trạng thái:"));
        searchRow.add(statusFilter);
        JButton refreshButton = new JButton("Làm mới");
        refreshButton.addActionListener(event -> refreshAsync());
        searchRow.add(refreshButton);
        top.add(searchRow);
        add(top, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Thêm dịch vụ");
        JButton editButton = new JButton("Sửa dịch vụ");
        JButton statusButton = new JButton("Đổi trạng thái");
        addButton.addActionListener(event -> showEditorDialog(null));
        editButton.addActionListener(event -> {
            DichVuResponse selected = selectedService();
            if (selected != null) {
                showEditorDialog(selected);
            }
        });
        statusButton.addActionListener(event -> toggleSelectedStatus());
        actions.add(addButton);
        actions.add(editButton);
        actions.add(statusButton);
        bottom.add(actions, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        statusFilter.addActionListener(event -> refreshAsync());
    }

    private void refreshAsync() {
        String keyword = keywordField.getText();
        Boolean active = switch (statusFilter.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
        runAsync(
                () -> dichVuService.timKiem(keyword, active),
                items -> {
                    visibleServices = items;
                    tableModel.setRowCount(0);
                    for (DichVuResponse item : items) {
                        tableModel.addRow(new Object[]{
                                item.maDichVu(),
                                item.tenDichVu(),
                                item.donGia().toPlainString(),
                                item.laCanLamSang() ? "Có" : "Không",
                                item.dangApDung() ? "Đang áp dụng" : "Ngừng áp dụng"
                        });
                    }
                    statusLabel.setText("Số dịch vụ: " + items.size());
                }
        );
    }

    private void showEditorDialog(DichVuResponse current) {
        boolean creating = current == null;
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(
                owner,
                creating ? "Thêm dịch vụ" : "Cập nhật dịch vụ",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField codeField = new JTextField(creating ? "" : current.maDichVu(), 20);
        codeField.setEnabled(creating);
        JTextField nameField = new JTextField(creating ? "" : current.tenDichVu(), 24);
        JTextField descriptionField = new JTextField(
                creating || current.moTa() == null ? "" : current.moTa(), 24
        );
        JTextField priceField = new JTextField(
                creating ? "0.00" : current.donGia().toPlainString(), 24
        );
        JCheckBox clinicalLabCheck = new JCheckBox(
                "Là dịch vụ cận lâm sàng", !creating && current.laCanLamSang()
        );
        addFormRow(form, "Mã dịch vụ:", codeField);
        addFormRow(form, "Tên dịch vụ:", nameField);
        addFormRow(form, "Mô tả:", descriptionField);
        addFormRow(form, "Đơn giá:", priceField);
        form.add(clinicalLabCheck);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        buttons.add(saveButton);
        buttons.add(cancelButton);
        cancelButton.addActionListener(event -> dialog.dispose());
        saveButton.addActionListener(event -> {
            final BigDecimal price;
            try {
                price = new BigDecimal(priceField.getText().strip());
            } catch (NumberFormatException ex) {
                showError(new IllegalArgumentException("Đơn giá không hợp lệ."));
                return;
            }

            saveButton.setEnabled(false);
            if (creating) {
                DichVuCreateRequest request = new DichVuCreateRequest(
                        codeField.getText(), nameField.getText(), descriptionField.getText(),
                        price, clinicalLabCheck.isSelected()
                );
                runAsync(() -> dichVuService.taoMoi(request), saved -> {
                    dialog.dispose();
                    showMessage("Đã thêm dịch vụ " + saved.maDichVu() + ".");
                    refreshAsync();
                }, error -> {
                    saveButton.setEnabled(true);
                    showError(error);
                });
            } else {
                DichVuUpdateRequest request = new DichVuUpdateRequest(
                        nameField.getText(), descriptionField.getText(), price,
                        clinicalLabCheck.isSelected()
                );
                runAsync(() -> dichVuService.capNhat(current.maDichVu(), request), saved -> {
                    dialog.dispose();
                    showMessage("Đã cập nhật dịch vụ " + saved.maDichVu() + ".");
                    refreshAsync();
                }, error -> {
                    saveButton.setEnabled(true);
                    showError(error);
                });
            }
        });

        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(460, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void toggleSelectedStatus() {
        DichVuResponse selected = selectedService();
        if (selected == null) {
            showMessage("Vui lòng chọn một dịch vụ.");
            return;
        }
        boolean newStatus = !selected.dangApDung();
        String action = newStatus ? "kích hoạt lại" : "ngừng áp dụng";
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có muốn " + action + " dịch vụ '" + selected.tenDichVu() + "'?",
                "Xác nhận thay đổi trạng thái",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        runAsync(() -> dichVuService.doiTrangThai(selected.maDichVu(), newStatus), saved -> {
            showMessage("Đã cập nhật trạng thái dịch vụ " + saved.maDichVu() + ".");
            refreshAsync();
        });
    }

    private DichVuResponse selectedService() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showMessage("Vui lòng chọn một dịch vụ.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= visibleServices.size()) {
            return null;
        }
        return visibleServices.get(modelRow);
    }

    private static void addFormRow(JPanel parent, String label, JComponent field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel caption = new JLabel(label);
        caption.setPreferredSize(new Dimension(105, 26));
        row.add(caption);
        row.add(field);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(row);
        parent.add(Box.createVerticalStrut(4));
    }

    private <T> void runAsync(Callable<T> work, Consumer<T> onSuccess) {
        runAsync(work, onSuccess, this::showError);
    }

    private <T> void runAsync(
            Callable<T> work,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        SecurityContext capturedContext = SecurityContextHolder.createEmptyContext();
        capturedContext.setAuthentication(
                SecurityContextHolder.getContext().getAuthentication()
        );

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                SecurityContext previousContext = SecurityContextHolder.getContext();
                SecurityContextHolder.setContext(capturedContext);
                try {
                    return work.call();
                } finally {
                    SecurityContextHolder.setContext(previousContext);
                }
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(ex);
                } catch (ExecutionException ex) {
                    onFailure.accept(ex.getCause() == null ? ex : ex.getCause());
                }
            }
        }.execute();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = "Không thể hoàn thành thao tác.";
        }
        JOptionPane.showMessageDialog(this, message, "Có lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
