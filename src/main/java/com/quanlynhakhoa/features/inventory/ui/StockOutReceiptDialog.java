package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutLineResponse;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
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
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/**
 * Hộp thoại tạo phiếu xuất mới hoặc sửa phiếu đang "Chờ xử lý".
 * Người dùng chọn mặt hàng + số lượng; hệ thống chia ra các lô theo FEFO (hết hạn trước — xuất trước),
 * bỏ qua lô đã hết hạn. Lưu phiếu không trừ tồn; tồn chỉ giảm khi bấm "Xuất kho".
 */
final class StockOutReceiptDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] LINE_COLUMNS =
            {"#", "Mặt hàng", "Số lô", "Hạn sử dụng", "Tồn lô", "SL xuất"};

    private final transient StockOutService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;
    private final transient StockOutDetailResponse existing;

    private final JSpinner spnDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<StockOutReason> cmbReason = new JComboBox<>(StockOutReason.values());
    private final JTextField txtReceiver = styleInput(new HintTextField("Bác sĩ / phòng / nhà cung cấp nhận hàng", 24));
    private final JTextField txtNote = styleInput(new JTextField(30));

    private final JComboBox<InventoryItemResponse> cmbItem = new JComboBox<>();
    private final JSpinner spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JButton btnAddLine = createButton("Thêm theo FEFO", ButtonKind.OUTLINE);
    private final JButton btnRemoveLine = createButton("Xóa dòng", ButtonKind.OUTLINE);

    private final LineTableModel lineModel = new LineTableModel();
    private final JTable tblLines = new JTable(lineModel);
    private final JLabel lblSummary = new JLabel();
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu phiếu", ButtonKind.PRIMARY);
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);

    private transient StockOutDetailResponse savedReceipt;
    /** true = chỉ cho chọn thuốc kiểm soát đặc biệt (mở từ màn Thuốc KSĐB). */
    private final boolean onlyControlled;

    StockOutReceiptDialog(Window owner, StockOutService service, InventoryItemService itemService,
                          Supplier<InventoryActor> session, StockOutDetailResponse existing) {
        this(owner, service, itemService, session, existing, false);
    }

    StockOutReceiptDialog(Window owner, StockOutService service, InventoryItemService itemService,
                          Supplier<InventoryActor> session, StockOutDetailResponse existing, boolean onlyControlled) {
        super(owner, existing == null ? (onlyControlled ? "Tạo phiếu xuất thuốc kiểm soát đặc biệt" : "Tạo phiếu xuất kho")
                : "Sửa phiếu xuất " + existing.maPhieu(), ModalityType.APPLICATION_MODAL);
        this.onlyControlled = onlyControlled;
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        this.existing = existing;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        wireEvents();
        if (existing != null) {
            spnDate.setValue(toDate(existing.ngayXuat()));
            cmbReason.setSelectedItem(existing.lyDo());
            txtReceiver.setText(existing.nguoiNhan());
            txtNote.setText(existing.ghiChu() == null ? "" : existing.ghiChu());
            existing.dong().forEach(lineModel::merge);
        }
        refreshSummary();
        loadItems();
        setSize(1000, 680);
        setLocationRelativeTo(owner);
    }

    Optional<StockOutDetailResponse> savedReceipt() {
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
        styleCombo(cmbReason);
        addField(pnlHeader, 0, 0, "Mã phiếu", readOnlyLabel(existing == null ? "Tự sinh khi lưu" : existing.maPhieu()));
        addField(pnlHeader, 0, 2, "Ngày xuất *", spnDate);
        addField(pnlHeader, 1, 0, "Lý do xuất *", cmbReason);
        addField(pnlHeader, 1, 2, "Người nhận *", txtReceiver);
        addField(pnlHeader, 2, 0, "Ghi chú", txtNote);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlHeader, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        Card pnlLineEntry = new Card(new GridBagLayout(), 12, 10);
        styleCombo(cmbItem);
        cmbItem.setRenderer(new ItemRenderer());
        spnQuantity.setFont(font(Font.PLAIN, 14));
        int inputHeight = txtNote.getPreferredSize().height;
        spnQuantity.setPreferredSize(new Dimension(100, inputHeight));
        spnQuantity.setMinimumSize(spnQuantity.getPreferredSize());
        spnQuantity.setToolTipText("Số lượng cần xuất");
        cmbItem.setPreferredSize(new Dimension(420, inputHeight));
        cmbItem.setMinimumSize(new Dimension(220, inputHeight));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        Object[][] entry = {{cmbItem, 1.0}, {fieldLabel("SL"), 0.0}, {spnQuantity, 0.0}, {btnAddLine, 0.0}};
        for (int i = 0; i < entry.length; i++) {
            gc.gridx = i;
            gc.weightx = (Double) entry[i][1];
            pnlLineEntry.add((Component) entry[i][0], gc);
        }

        tblLines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(tblLines, 40, new int[] {44, 330, 110, 130, 100, 110}, Set.of(0, 4, 5));
        JScrollPane scrLines = tableScroll(tblLines);

        JPanel pnlLinesFooter = new JPanel(new BorderLayout());
        pnlLinesFooter.setOpaque(false);
        JPanel pnlLineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLineButtons.setOpaque(false);
        pnlLineButtons.add(btnRemoveLine);
        lblSummary.setFont(font(Font.BOLD, 15));
        lblSummary.setForeground(TEXT);
        pnlLinesFooter.add(pnlLineButtons, BorderLayout.WEST);
        pnlLinesFooter.add(lblSummary, BorderLayout.EAST);

        JPanel pnlLines = new JPanel(new BorderLayout(0, 10));
        pnlLines.setOpaque(false);
        JLabel lblLinesTitle = new JLabel("DÒNG HÀNG — chọn mặt hàng, nhập số lượng rồi bấm \"Thêm theo FEFO\": "
                + "hệ thống lấy lô hết hạn sớm nhất trước, bỏ qua lô đã hết hạn");
        lblLinesTitle.setFont(font(Font.PLAIN, 13));
        lblLinesTitle.setForeground(TEXT_MUTED);
        JPanel pnlLinesTop = new JPanel(new BorderLayout(0, 6));
        pnlLinesTop.setOpaque(false);
        pnlLinesTop.add(lblLinesTitle, BorderLayout.NORTH);
        pnlLinesTop.add(pnlLineEntry, BorderLayout.CENTER);
        pnlLines.add(pnlLinesTop, BorderLayout.NORTH);
        pnlLines.add(scrLines, BorderLayout.CENTER);
        pnlLines.add(pnlLinesFooter, BorderLayout.SOUTH);
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
        btnAddLine.addActionListener(e -> addLines());
        btnRemoveLine.addActionListener(e -> {
            int row = tblLines.getSelectedRow();
            if (row >= 0) {
                lineModel.remove(row);
                refreshSummary();
            }
        });
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
    }

    // ----------------------------------------------------------------- dữ liệu

    private void loadItems() {
        Set<Role> roles = session.get().roles();
        setBusy(true);
        new SwingWorker<List<InventoryItemResponse>, Void>() {
            @Override
            protected List<InventoryItemResponse> doInBackground() {
                return itemService.search(roles, "", null);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    cmbItem.removeAllItems();
                    for (InventoryItemResponse item : get()) {
                        if (item.tonKho() > 0 && (!onlyControlled || item.kiemSoatDacBiet())) {
                            cmbItem.addItem(item);
                        }
                    }
                    if (cmbItem.getItemCount() == 0) {
                        lblError.setText(onlyControlled ? "Không có thuốc kiểm soát đặc biệt nào còn tồn khả dụng."
                                : "Kho chưa có mặt hàng nào còn tồn khả dụng — nhập kho trước khi xuất.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    /** Hỏi service chia lô theo FEFO, trừ phần đã chọn trên phiếu này, rồi gộp vào bảng. */
    private void addLines() {
        lblError.setText(" ");
        if (!commitSpinners(lblError, spnQuantity)) {
            return;
        }
        InventoryItemResponse item = (InventoryItemResponse) cmbItem.getSelectedItem();
        if (item == null) {
            lblError.setText("Chưa chọn mặt hàng.");
            return;
        }
        int quantity = ((Number) spnQuantity.getValue()).intValue();
        Map<Integer, Integer> chosen = lineModel.quantitiesByLot();
        Set<Role> roles = session.get().roles();
        setBusy(true);
        new SwingWorker<List<StockOutLineResponse>, Void>() {
            @Override
            protected List<StockOutLineResponse> doInBackground() {
                return service.suggestFefo(roles, item.loai(), item.ma(), quantity, chosen);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get().forEach(lineModel::merge);
                    refreshSummary();
                    spnQuantity.setValue(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void refreshSummary() {
        long special = lineModel.rows.stream().filter(StockOutLineResponse::kiemSoatDacBiet).count();
        lblSummary.setText(lineModel.rows.size() + " dòng · " + lineModel.totalQuantity() + " đơn vị"
                + (special > 0 ? " · có thuốc KSĐB — người khác người lập mới được bấm Xuất kho" : ""));
        lblSummary.setForeground(special > 0 ? ORANGE : TEXT);
    }

    private void save() {
        if (!commitSpinners(lblError, spnDate)) {
            return;
        }
        List<StockOutLineRequest> lines = new ArrayList<>();
        for (StockOutLineResponse l : lineModel.rows) {
            lines.add(new StockOutLineRequest(l.maLo(), l.soLuong()));
        }
        StockOutRequest request = new StockOutRequest(toLocalDate((Date) spnDate.getValue()),
                (StockOutReason) cmbReason.getSelectedItem(), txtReceiver.getText(), txtNote.getText(), lines);
        InventoryActor actor = session.get();
        setBusy(true);
        lblError.setText(" ");
        new SwingWorker<StockOutDetailResponse, Void>() {
            @Override
            protected StockOutDetailResponse doInBackground() {
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
        btnAddLine.setEnabled(!busy);
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ------------------------------------------------------------------- bảng

    private static final class LineTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<StockOutLineResponse> rows = new ArrayList<>();

        /** Cùng lô thì cộng dồn số lượng, khác lô thì thêm dòng mới. */
        void merge(StockOutLineResponse line) {
            for (int i = 0; i < rows.size(); i++) {
                StockOutLineResponse r = rows.get(i);
                if (r.maLo().equals(line.maLo())) {
                    rows.set(i, new StockOutLineResponse(r.maLo(), r.loai(), r.maMatHang(), r.tenMatHang(),
                            r.donViTinh(), r.soLo(), r.hanSuDung(), r.soLuong() + line.soLuong(), line.tonHienTai(),
                            r.kiemSoatDacBiet(), null, null));
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
            rows.add(line);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void remove(int row) {
            rows.remove(row);
            fireTableDataChanged();
        }

        Map<Integer, Integer> quantitiesByLot() {
            Map<Integer, Integer> result = new HashMap<>();
            rows.forEach(r -> result.merge(r.maLo(), r.soLuong(), Integer::sum));
            return result;
        }

        int totalQuantity() {
            return rows.stream().mapToInt(StockOutLineResponse::soLuong).sum();
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
        public Object getValueAt(int row, int column) {
            StockOutLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang() + " — " + l.tenMatHang() + (l.kiemSoatDacBiet() ? "  (KSĐB)" : "");
                case 2 -> l.soLo();
                case 3 -> InventoryFormat.date(l.hanSuDung());
                case 4 -> l.tonHienTai() + " " + l.donViTinh();
                default -> l.soLuong() + " " + l.donViTinh();
            };
        }
    }

    private static final class ItemRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focused) {
            JLabel lblItem = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
            lblItem.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lblItem.setFont(font(Font.PLAIN, 14));
            lblItem.setBackground(selected && index >= 0 ? SELECTED_FILL : SURFACE);
            lblItem.setForeground(TEXT);
            if (value instanceof InventoryItemResponse item) {
                lblItem.setText(item.ma() + " — " + item.ten() + "  ·  còn " + item.tonKho() + " " + item.donViTinh()
                        + (item.kiemSoatDacBiet() ? "  (KSĐB)" : ""));
            } else if (value == null) {
                lblItem.setText("Chọn mặt hàng còn tồn");
            }
            return lblItem;
        }
    }
}
