package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnRequest;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnSourceResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnableLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnLineResponse;
import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockReturnService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/**
 * Hộp thoại lập phiếu hoàn trả mới hoặc sửa phiếu đang chờ kiểm duyệt.
 * Chọn phiếu xuất gốc → bảng liệt kê các lô đã xuất và số còn được trả; nhập "SL trả" cho lô cần trả.
 * Lưu phiếu không đổi tồn; tồn chỉ thay đổi khi phiếu được "Duyệt".
 */
final class StockReturnReceiptDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] LINE_COLUMNS =
            {"#", "Mặt hàng", "Số lô", "Hạn sử dụng", "Đã xuất", "Đã/đang trả", "Còn được trả", "SL trả"};
    private static final int COL_QUANTITY = 7;

    private final transient StockReturnService service;
    private final transient Supplier<InventoryActor> session;
    private final transient StockReturnDetailResponse existing;

    private final JSpinner spnDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<ReturnSourceResponse> cmbSource = new JComboBox<>();
    private final JComboBox<ReturnReason> cmbReason = new JComboBox<>(ReturnReason.values());
    private final JTextField txtOrigin = styleInput(new HintTextField("Phòng khám / bác sĩ / nhà cung cấp", 22));
    private final JTextField txtReturner = styleInput(new HintTextField("Người mang hàng trả", 22));
    private final JTextField txtNote = styleInput(new JTextField(30));

    private final LineTableModel lineModel = new LineTableModel();
    private final JTable tblLines = new JTable(lineModel);
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblReasonHint = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu phiếu", ButtonKind.PRIMARY);
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);

    private transient StockReturnDetailResponse savedReceipt;
    /** Giá trị đã tự điền vào "Nguồn hoàn trả" — chỉ ghi đè khi người dùng chưa sửa. */
    private String autoOrigin = "";
    private boolean loadingSources;

    StockReturnReceiptDialog(Window owner, StockReturnService service, Supplier<InventoryActor> session,
                             StockReturnDetailResponse existing) {
        super(owner, existing == null ? "Tạo phiếu hoàn trả" : "Sửa phiếu hoàn trả " + existing.maPhieu(),
                ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.existing = existing;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        wireEvents();
        if (existing != null) {
            spnDate.setValue(toDate(existing.ngayHoanTra()));
            cmbReason.setSelectedItem(existing.lyDo());
            txtOrigin.setText(existing.nguonHoanTra());
            txtReturner.setText(existing.nguoiHoanTra());
            txtNote.setText(existing.ghiChu() == null ? "" : existing.ghiChu());
            cmbSource.setEnabled(false);
            loadLines(existing.maPhieuXuat());
        } else {
            loadSources();
        }
        refreshReasonHint();
        refreshSummary();
        setSize(1060, 700);
        setLocationRelativeTo(owner);
    }

    Optional<StockReturnDetailResponse> savedReceipt() {
        return Optional.ofNullable(savedReceipt);
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 14));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);

        JPanel pnlHeader = new JPanel(new GridBagLayout());
        pnlHeader.setOpaque(false);
        spnDate.setEditor(new JSpinner.DateEditor(spnDate, "dd/MM/yyyy"));
        spnDate.setFont(font(Font.PLAIN, 14));
        spnDate.setValue(toDate(LocalDate.now()));
        styleCombo(cmbSource);
        cmbSource.setRenderer(new SourceRenderer());
        styleCombo(cmbReason);
        addField(pnlHeader, 0, 0, "Mã phiếu", readOnlyLabel(existing == null ? "Tự sinh khi lưu" : existing.maPhieu()));
        addField(pnlHeader, 0, 2, "Ngày hoàn trả *", spnDate);
        if (existing == null) {
            addField(pnlHeader, 1, 0, "Phiếu xuất gốc *", cmbSource);
        } else {
            addField(pnlHeader, 1, 0, "Phiếu xuất gốc", readOnlyLabel(existing.maPhieuXuat()));
        }
        addField(pnlHeader, 1, 2, "Lý do *", cmbReason);
        addField(pnlHeader, 2, 0, "Nguồn hoàn trả *", txtOrigin);
        addField(pnlHeader, 2, 2, "Người hoàn trả *", txtReturner);
        addField(pnlHeader, 3, 0, "Ghi chú", txtNote);
        lblReasonHint.setFont(font(Font.PLAIN, 13));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 2;
        gc.gridy = 3;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(4, 18, 4, 0);
        pnlHeader.add(lblReasonHint, gc);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlHeader, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        styleTable(tblLines, 40, new int[] {40, 280, 90, 120, 85, 105, 115, 100}, Set.of(0, 4, 5, 6, 7));
        tblLines.setSurrendersFocusOnKeystroke(true);
        tblLines.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JScrollPane scrLines = tableScroll(tblLines);

        JLabel lblLinesTitle = new JLabel("CÁC LÔ TRÊN PHIẾU XUẤT GỐC — nhập số lượng trả vào cột \"SL trả\" "
                + "(để 0 với lô không trả)");
        lblLinesTitle.setFont(font(Font.PLAIN, 13));
        lblLinesTitle.setForeground(TEXT_MUTED);
        lblSummary.setFont(font(Font.BOLD, 15));
        lblSummary.setForeground(TEXT);
        lblSummary.setHorizontalAlignment(JLabel.RIGHT);
        JPanel pnlLines = new JPanel(new BorderLayout(0, 8));
        pnlLines.setOpaque(false);
        pnlLines.add(lblLinesTitle, BorderLayout.NORTH);
        pnlLines.add(scrLines, BorderLayout.CENTER);
        pnlLines.add(lblSummary, BorderLayout.SOUTH);
        pnlRoot.add(pnlLines, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);
        pnlFooter.add(lblError, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private static JLabel readOnlyLabel(String text) {
        JLabel lblValue = new JLabel(text);
        lblValue.setFont(font(Font.BOLD, 14));
        lblValue.setForeground(TEXT);
        return lblValue;
    }

    private static void addField(JPanel target, int row, int column, String label, JComponent field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = column;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(4, column == 0 ? 0 : 18, 4, 10);
        target.add(fieldLabel(label), gc);
        gc.gridx = column + 1;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        target.add(field, gc);
    }

    private void wireEvents() {
        cmbSource.addActionListener(e -> {
            if (loadingSources) {
                return;
            }
            ReturnSourceResponse source = (ReturnSourceResponse) cmbSource.getSelectedItem();
            if (source != null) {
                if (txtOrigin.getText().isBlank() || txtOrigin.getText().equals(autoOrigin)) {
                    autoOrigin = source.nguoiNhan();
                    txtOrigin.setText(autoOrigin);
                }
                loadLines(source.maPhieuXuat());
            }
        });
        cmbReason.addActionListener(e -> refreshReasonHint());
        lineModel.addTableModelListener(e -> refreshSummary());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
    }

    private void refreshReasonHint() {
        ReturnReason reason = (ReturnReason) cmbReason.getSelectedItem();
        boolean restock = reason == null || reason.restock();
        lblReasonHint.setForeground(restock ? TEXT_MUTED : ORANGE);
        lblReasonHint.setText(restock ? "Khi duyệt: cộng lại tồn của đúng lô đã xuất."
                : "Hàng lỗi: chỉ ghi nhận, không cộng lại vào tồn khả dụng.");
    }

    // ----------------------------------------------------------------- dữ liệu

    private void loadSources() {
        Set<Role> roles = session.get().roles();
        setBusy(true);
        new SwingWorker<List<ReturnSourceResponse>, Void>() {
            @Override
            protected List<ReturnSourceResponse> doInBackground() {
                return service.listSources(roles);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<ReturnSourceResponse> sources = get();
                    loadingSources = true;
                    cmbSource.removeAllItems();
                    sources.forEach(cmbSource::addItem);
                    cmbSource.setSelectedIndex(-1);
                    loadingSources = false;
                    if (sources.isEmpty()) {
                        lblError.setText("Chưa có phiếu xuất đã hoàn thành nào còn hàng để hoàn trả.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void loadLines(String maPhieuXuat) {
        Set<Role> roles = session.get().roles();
        String exclude = existing == null ? null : existing.maPhieu();
        setBusy(true);
        lblError.setText(" ");
        new SwingWorker<List<ReturnableLineResponse>, Void>() {
            @Override
            protected List<ReturnableLineResponse> doInBackground() {
                return service.returnableLines(roles, maPhieuXuat, exclude);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    Map<Integer, Integer> preset = new HashMap<>();
                    if (existing != null) {
                        for (StockReturnLineResponse l : existing.dong()) {
                            preset.put(l.maLo(), l.soLuong());
                        }
                    }
                    lineModel.setRows(get(), preset);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lineModel.setRows(List.of(), Map.of());
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void refreshSummary() {
        int units = 0;
        BigDecimal value = BigDecimal.ZERO;
        int lines = 0;
        for (int i = 0; i < lineModel.rows.size(); i++) {
            int q = lineModel.quantities.get(i);
            if (q > 0) {
                lines++;
                units += q;
                value = value.add(lineModel.rows.get(i).donGia().multiply(BigDecimal.valueOf(q)));
            }
        }
        lblSummary.setText(lines + " lô · " + units + " đơn vị · Giá trị hoàn trả: " + InventoryFormat.money(value));
    }

    private void save() {
        if (tblLines.isEditing() && !tblLines.getCellEditor().stopCellEditing()) {
            lblError.setText("Số lượng trả không hợp lệ — nhập số nguyên.");
            return;
        }
        if (!commitSpinners(lblError, spnDate)) {
            return;
        }
        ReturnSourceResponse source = (ReturnSourceResponse) cmbSource.getSelectedItem();
        String maPhieuXuat = existing != null ? existing.maPhieuXuat() : source == null ? null : source.maPhieuXuat();
        List<StockReturnLineRequest> lines = new ArrayList<>();
        for (int i = 0; i < lineModel.rows.size(); i++) {
            int q = lineModel.quantities.get(i);
            if (q > 0) {
                lines.add(new StockReturnLineRequest(lineModel.rows.get(i).maLo(), q));
            }
        }
        StockReturnRequest request = new StockReturnRequest(toLocalDate((Date) spnDate.getValue()), maPhieuXuat,
                (ReturnReason) cmbReason.getSelectedItem(), txtOrigin.getText(), txtReturner.getText(),
                txtNote.getText(), lines);
        InventoryActor actor = session.get();
        setBusy(true);
        lblError.setText(" ");
        new SwingWorker<StockReturnDetailResponse, Void>() {
            @Override
            protected StockReturnDetailResponse doInBackground() {
                return existing == null ? service.create(actor, request)
                        : service.update(actor, existing.maPhieu(), request);
            }

            @Override
            protected void done() {
                try {
                    savedReceipt = get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    setBusy(false);
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Đang xử lý…" : "Lưu phiếu");
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ------------------------------------------------------------------- bảng

    private final class LineTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<ReturnableLineResponse> rows = new ArrayList<>();
        private final List<Integer> quantities = new ArrayList<>();

        void setRows(List<ReturnableLineResponse> lines, Map<Integer, Integer> preset) {
            rows.clear();
            quantities.clear();
            for (ReturnableLineResponse l : lines) {
                rows.add(l);
                quantities.add(preset.getOrDefault(l.maLo(), 0));
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return LINE_COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return LINE_COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == COL_QUANTITY ? Integer.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_QUANTITY && rows.get(row).conDuocTra() > 0;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != COL_QUANTITY || !(value instanceof Integer q)) {
                return;
            }
            int max = rows.get(row).conDuocTra();
            int clamped = Math.max(0, Math.min(q, max));
            lblError.setText(q > max ? "Lô " + rows.get(row).soLo() + " chỉ còn được trả " + max + " — đã chỉnh về "
                    + max + "." : q < 0 ? "Số lượng trả không được âm." : " ");
            quantities.set(row, clamped);
            fireTableCellUpdated(row, column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            ReturnableLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang() + " — " + l.tenMatHang();
                case 2 -> l.soLo();
                case 3 -> InventoryFormat.date(l.hanSuDung());
                case 4 -> l.daXuat() + " " + l.donViTinh();
                case 5 -> l.daTra() + " " + l.donViTinh();
                case 6 -> l.conDuocTra() + " " + l.donViTinh();
                default -> quantities.get(row);
            };
        }
    }

    private static final class SourceRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focused) {
            JLabel lblItem = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
            lblItem.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lblItem.setFont(font(Font.PLAIN, 14));
            lblItem.setBackground(selected && index >= 0 ? SELECTED_FILL : SURFACE);
            lblItem.setForeground(TEXT);
            if (value instanceof ReturnSourceResponse s) {
                lblItem.setText(s.maPhieuXuat() + " · " + InventoryFormat.date(s.ngayXuat()) + " · " + s.nguoiNhan()
                        + " · " + s.lyDoXuat().shortLabel() + " · còn trả " + s.conDuocTra());
            } else if (value == null) {
                lblItem.setText("Chọn phiếu xuất đã hoàn thành");
            }
            return lblItem;
        }
    }
}
