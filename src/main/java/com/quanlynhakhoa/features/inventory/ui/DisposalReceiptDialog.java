package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalRequest;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLotOption;
import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.service.DisposalService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.List;
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
 * Lập phiếu hủy mới hoặc sửa phiếu đang chờ duyệt. Chọn lô (lô hết hạn hiện trước, có nhãn "HẾT HẠN"),
 * số lượng ≤ tồn lô. Lưu phiếu chưa trừ tồn — tồn chỉ giảm khi người khác người lập bấm "Duyệt".
 */
final class DisposalReceiptDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] LINE_COLUMNS =
            {"#", "Mặt hàng", "Số lô", "Hạn sử dụng", "Tồn lô", "SL hủy", "Đơn giá", "Thành tiền"};

    private final transient DisposalService service;
    private final transient Supplier<InventoryActor> session;
    private final transient DisposalDetailResponse existing;

    private final JSpinner spnDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<DisposalReason> cmbReason = new JComboBox<>(DisposalReason.values());
    private final JTextField txtDescription = styleInput(new HintTextField("Vd. Hỏng hóc do đổ vỡ khi vận chuyển", 26));
    private final JComboBox<DisposalLotOption> cmbLot = new JComboBox<>();
    private final SpinnerNumberModel quantityModel = new SpinnerNumberModel(1, 1, 1_000_000, 1);
    private final JSpinner spnQuantity = new JSpinner(quantityModel);
    private final JButton btnAddLine = createButton("Thêm dòng", ButtonKind.OUTLINE);
    private final JButton btnAddExpired = createButton("Thêm tất cả lô hết hạn", ButtonKind.OUTLINE);
    private final JButton btnRemoveLine = createButton("Xóa dòng", ButtonKind.OUTLINE);
    private final LineModel lineModel = new LineModel();
    private final JTable tblLines = new JTable(lineModel);
    private final JLabel lblTotal = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu phiếu", ButtonKind.PRIMARY);
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);

    private transient List<DisposalLotOption> options = List.of();
    private transient DisposalDetailResponse saved;

    /** Một dòng đang soạn. */
    private record Draft(Integer maLo, String mat, String donVi, String soLo, LocalDate hsd, int ton, boolean hetHan,
                         int soLuong, BigDecimal donGia) {
        BigDecimal thanhTien() {
            return donGia.multiply(BigDecimal.valueOf(soLuong));
        }
    }

    DisposalReceiptDialog(Window owner, DisposalService service, Supplier<InventoryActor> session,
                          DisposalDetailResponse existing) {
        super(owner, existing == null ? "Tạo phiếu hủy / hao hụt" : "Sửa phiếu hủy " + existing.maPhieu(),
                ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.existing = existing;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        wireEvents();
        if (existing != null) {
            spnDate.setValue(toDate(existing.ngayHuy()));
            cmbReason.setSelectedItem(existing.lyDo());
            txtDescription.setText(existing.moTa() == null ? "" : existing.moTa());
            for (DisposalLineResponse l : existing.dong()) {
                lineModel.add(new Draft(l.maLo(), l.maMatHang() + " — " + l.tenMatHang(), l.donViTinh(), l.soLo(),
                        l.hanSuDung(), l.tonHienTai(), l.hanSuDung() != null && l.hanSuDung().isBefore(LocalDate.now()),
                        l.soLuong(), l.donGia()));
            }
        }
        refreshTotal();
        loadLots();
        setSize(1080, 700);
        setLocationRelativeTo(owner);
    }

    Optional<DisposalDetailResponse> saved() {
        return Optional.ofNullable(saved);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 14));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);

        spnDate.setEditor(new JSpinner.DateEditor(spnDate, "dd/MM/yyyy"));
        spnDate.setFont(font(Font.PLAIN, 14));
        spnDate.setValue(toDate(LocalDate.now()));
        styleCombo(cmbReason);
        JPanel pnlHeader = new JPanel(new GridBagLayout());
        pnlHeader.setOpaque(false);
        addField(pnlHeader, 0, 0, "Mã phiếu", readOnly(existing == null ? "Tự sinh khi lưu" : existing.maPhieu()));
        addField(pnlHeader, 0, 2, "Ngày hủy *", spnDate);
        addField(pnlHeader, 1, 0, "Lý do *", cmbReason);
        addField(pnlHeader, 1, 2, "Mô tả cụ thể", txtDescription);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlHeader, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        Card pnlEntry = new Card(new GridBagLayout(), 12, 10);
        styleCombo(cmbLot);
        cmbLot.setRenderer(new LotRenderer());
        int h = txtDescription.getPreferredSize().height;
        cmbLot.setPreferredSize(new Dimension(460, h));
        cmbLot.setMinimumSize(new Dimension(240, h));
        spnQuantity.setFont(font(Font.PLAIN, 14));
        spnQuantity.setPreferredSize(new Dimension(100, h));
        spnQuantity.setMinimumSize(spnQuantity.getPreferredSize());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        Object[][] entry = {{cmbLot, 1.0}, {fieldLabel("SL"), 0.0}, {spnQuantity, 0.0}, {btnAddLine, 0.0},
                {btnAddExpired, 0.0}};
        for (int i = 0; i < entry.length; i++) {
            gc.gridx = i;
            gc.weightx = (Double) entry[i][1];
            pnlEntry.add((Component) entry[i][0], gc);
        }

        tblLines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(tblLines, 40, new int[] {40, 240, 90, 200, 85, 90, 110, 130}, Set.of(0, 4, 5, 6, 7));
        JScrollPane scrLines = tableScroll(tblLines);
        lblTotal.setFont(font(Font.BOLD, 16));
        lblTotal.setForeground(TEXT);
        JPanel pnlLinesFooter = new JPanel(new BorderLayout());
        pnlLinesFooter.setOpaque(false);
        JPanel pnlRemove = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlRemove.setOpaque(false);
        pnlRemove.add(btnRemoveLine);
        pnlLinesFooter.add(pnlRemove, BorderLayout.WEST);
        pnlLinesFooter.add(lblTotal, BorderLayout.EAST);
        JLabel lblHint = new JLabel("LÔ CẦN HỦY — lô đã hết hạn được xếp đầu danh sách; giá trị tính theo giá nhập "
                + "của từng lô");
        lblHint.setFont(font(Font.PLAIN, 13));
        lblHint.setForeground(TEXT_MUTED);
        JPanel pnlLinesTop = new JPanel(new BorderLayout(0, 6));
        pnlLinesTop.setOpaque(false);
        pnlLinesTop.add(lblHint, BorderLayout.NORTH);
        pnlLinesTop.add(pnlEntry, BorderLayout.CENTER);
        JPanel pnlLines = new JPanel(new BorderLayout(0, 10));
        pnlLines.setOpaque(false);
        pnlLines.add(pnlLinesTop, BorderLayout.NORTH);
        pnlLines.add(scrLines, BorderLayout.CENTER);
        pnlLines.add(pnlLinesFooter, BorderLayout.SOUTH);
        pnlRoot.add(pnlLines, BorderLayout.CENTER);

        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblError, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private static JLabel readOnly(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font(Font.BOLD, 14));
        lbl.setForeground(TEXT);
        return lbl;
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
        cmbLot.addActionListener(e -> {
            DisposalLotOption lot = (DisposalLotOption) cmbLot.getSelectedItem();
            if (lot != null) {
                quantityModel.setMaximum(Math.max(1, lot.tonKho()));
                quantityModel.setValue(lot.tonKho());
            }
        });
        btnAddLine.addActionListener(e -> addSelected());
        btnAddExpired.addActionListener(e -> addAllExpired());
        btnRemoveLine.addActionListener(e -> {
            int row = tblLines.getSelectedRow();
            if (row >= 0) {
                lineModel.remove(row);
                refreshTotal();
            }
        });
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
    }

    private void loadLots() {
        Set<Role> roles = session.get().roles();
        setBusy(true);
        new SwingWorker<List<DisposalLotOption>, Void>() {
            @Override
            protected List<DisposalLotOption> doInBackground() {
                return service.availableLots(roles);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    options = get();
                    cmbLot.removeAllItems();
                    options.forEach(cmbLot::addItem);
                    long expired = options.stream().filter(DisposalLotOption::hetHan).count();
                    btnAddExpired.setText("Thêm tất cả lô hết hạn (" + expired + ")");
                    btnAddExpired.setEnabled(expired > 0);
                    if (options.isEmpty()) {
                        lblError.setText("Kho không còn lô nào có tồn.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private static Draft draftOf(DisposalLotOption o, int quantity) {
        return new Draft(o.maLo(), o.maMatHang() + " — " + o.tenMatHang(), o.donViTinh(), o.soLo(), o.hanSuDung(),
                o.tonKho(), o.hetHan(), quantity, o.donGia());
    }

    private void addSelected() {
        lblError.setText(" ");
        if (!commitSpinners(lblError, spnQuantity)) {
            return;
        }
        DisposalLotOption lot = (DisposalLotOption) cmbLot.getSelectedItem();
        if (lot == null) {
            lblError.setText("Chưa chọn lô.");
            return;
        }
        if (lineModel.contains(lot.maLo())) {
            lblError.setText("Lô " + lot.soLo() + " đã có trong phiếu — xóa dòng cũ nếu muốn đổi số lượng.");
            return;
        }
        int quantity = ((Number) spnQuantity.getValue()).intValue();
        if (quantity > lot.tonKho()) {
            lblError.setText("Lô " + lot.soLo() + " chỉ còn " + lot.tonKho() + ".");
            return;
        }
        lineModel.add(draftOf(lot, quantity));
        refreshTotal();
    }

    private void addAllExpired() {
        int added = 0;
        for (DisposalLotOption o : options) {
            if (o.hetHan() && !lineModel.contains(o.maLo())) {
                lineModel.add(draftOf(o, o.tonKho()));
                added++;
            }
        }
        if (added > 0 && cmbReason.getSelectedItem() != DisposalReason.QUA_HAN && lineModel.rows.size() == added) {
            cmbReason.setSelectedItem(DisposalReason.QUA_HAN);
        }
        lblError.setForeground(added > 0 ? GREEN : RED);
        lblError.setText(added > 0 ? "Đã thêm " + added + " lô hết hạn với toàn bộ số tồn." : "Không có lô hết hạn mới.");
        refreshTotal();
    }

    private void refreshTotal() {
        lblTotal.setText(lineModel.rows.size() + " lô · Giá trị hủy: " + InventoryFormat.money(lineModel.total()));
    }

    private void save() {
        if (!commitSpinners(lblError, spnDate)) {
            return;
        }
        List<DisposalLineRequest> lines = new ArrayList<>();
        lineModel.rows.forEach(d -> lines.add(new DisposalLineRequest(d.maLo(), d.soLuong())));
        DisposalRequest request = new DisposalRequest(toLocalDate((Date) spnDate.getValue()),
                (DisposalReason) cmbReason.getSelectedItem(), txtDescription.getText(), lines);
        InventoryActor actor = session.get();
        setBusy(true);
        lblError.setForeground(RED);
        lblError.setText(" ");
        new SwingWorker<DisposalDetailResponse, Void>() {
            @Override
            protected DisposalDetailResponse doInBackground() {
                return existing == null ? service.create(actor, request)
                        : service.update(actor, existing.maPhieu(), request);
            }

            @Override
            protected void done() {
                try {
                    saved = get();
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

    private static final class LineModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<Draft> rows = new ArrayList<>();

        void add(Draft d) {
            rows.add(d);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void remove(int row) {
            rows.remove(row);
            fireTableDataChanged();
        }

        boolean contains(Integer maLo) {
            return rows.stream().anyMatch(r -> r.maLo().equals(maLo));
        }

        BigDecimal total() {
            return rows.stream().map(Draft::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
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
            Draft d = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> d.mat();
                case 2 -> d.soLo();
                case 3 -> InventoryFormat.date(d.hsd()) + (d.hetHan() ? "  (hết hạn)" : "");
                case 4 -> d.ton() + " " + d.donVi();
                case 5 -> d.soLuong() + " " + d.donVi();
                case 6 -> InventoryFormat.money(d.donGia());
                default -> InventoryFormat.money(d.thanhTien());
            };
        }
    }

    private static final class LotRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focused) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
            lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lbl.setFont(font(Font.PLAIN, 14));
            lbl.setBackground(selected && index >= 0 ? SELECTED_FILL : SURFACE);
            lbl.setForeground(TEXT);
            if (value instanceof DisposalLotOption o) {
                lbl.setText("<html><nobr>" + (o.hetHan() ? "<b style='color:#D32F2F'>HẾT HẠN</b> · " : "")
                        + o.maMatHang() + " — " + o.tenMatHang() + " · lô " + o.soLo() + " · HSD "
                        + InventoryFormat.date(o.hanSuDung()) + " · tồn " + o.tonKho() + " " + o.donViTinh() + "</nobr></html>");
            } else if (value == null) {
                lbl.setText("Chọn lô cần hủy");
            }
            return lbl;
        }
    }
}
