package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.SupplierResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.StockInService;
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
import javax.swing.JOptionPane;
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
 * Hộp thoại tạo phiếu nhập mới hoặc sửa phiếu đang "Chờ xử lý".
 * Lưu phiếu không cộng tồn; tồn chỉ tăng khi bấm "Nhập kho".
 */
final class StockInReceiptDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] LINE_COLUMNS =
            {"#", "Mặt hàng", "Số lô", "Hạn sử dụng", "SL", "Đơn giá", "Thành tiền"};

    private final transient StockInService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;
    private final transient StockInDetailResponse existing;

    private final JSpinner spnDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<SupplierResponse> cmbSupplier = new JComboBox<>();
    private final JButton btnAddSupplier = createButton("+ Nhà cung cấp", ButtonKind.OUTLINE);
    private final JTextField txtNote = styleInput(new JTextField(30));

    private final JComboBox<InventoryItemResponse> cmbItem = new JComboBox<>();
    private final JTextField txtLot = styleInput(new HintTextField("Số lô", 10));
    private final JTextField txtExpiry = styleInput(new HintTextField("dd/MM/yyyy", 9));
    private final JSpinner spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JTextField txtPrice = styleInput(new HintTextField("Đơn giá (đ)", 9));
    private final JButton btnAddLine = createButton("Thêm dòng", ButtonKind.OUTLINE);
    private final JButton btnRemoveLine = createButton("Xóa dòng", ButtonKind.OUTLINE);

    private final LineTableModel lineModel = new LineTableModel();
    private final JTable tblLines = new JTable(lineModel);
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu phiếu", ButtonKind.PRIMARY);
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);

    private transient StockInDetailResponse savedReceipt;

    /** Một dòng đang soạn trên form. */
    private record DraftLine(InventoryItemType loai, String ma, String ten, String donVi, String soLo,
                             LocalDate hanSuDung, int soLuong, BigDecimal donGia) {
        BigDecimal thanhTien() {
            return donGia.multiply(BigDecimal.valueOf(soLuong));
        }
    }

    StockInReceiptDialog(Window owner, StockInService service, InventoryItemService itemService,
                         Supplier<InventoryActor> session, StockInDetailResponse existing) {
        super(owner, existing == null ? "Tạo phiếu nhập kho" : "Sửa phiếu nhập " + existing.maPhieu(),
                ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        this.existing = existing;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        wireEvents();
        if (existing != null) {
            spnDate.setValue(toDate(existing.ngayNhap()));
            txtNote.setText(existing.ghiChu() == null ? "" : existing.ghiChu());
            for (StockInLineResponse l : existing.dong()) {
                lineModel.add(new DraftLine(l.loai(), l.maMatHang(), l.tenMatHang(), l.donViTinh(), l.soLo(),
                        l.hanSuDung(), l.soLuong(), l.donGia()));
            }
        }
        refreshTotal();
        loadChoices();
        setSize(1060, 700);
        setLocationRelativeTo(owner);
    }

    Optional<StockInDetailResponse> savedReceipt() {
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
        styleCombo(cmbSupplier);
        JPanel pnlSupplier = new JPanel(new BorderLayout(8, 0));
        pnlSupplier.setOpaque(false);
        pnlSupplier.add(cmbSupplier, BorderLayout.CENTER);
        pnlSupplier.add(btnAddSupplier, BorderLayout.EAST);
        addField(pnlHeader, 0, 0, "Mã phiếu", readOnlyLabel(existing == null ? "Tự sinh khi lưu" : existing.maPhieu()));
        addField(pnlHeader, 0, 2, "Ngày nhập *", spnDate);
        addField(pnlHeader, 1, 0, "Nhà cung cấp *", pnlSupplier);
        addField(pnlHeader, 1, 2, "Ghi chú", txtNote);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlHeader, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        // Dòng nhập nhanh + bảng dòng hàng
        Card pnlLineEntry = new Card(new GridBagLayout(), 12, 10);
        styleCombo(cmbItem);
        cmbItem.setRenderer(new ItemRenderer());
        spnQuantity.setFont(font(Font.PLAIN, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 0, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        Object[][] entry = {{cmbItem, 1.0}, {txtLot, 0.0}, {txtExpiry, 0.0}, {spnQuantity, 0.0}, {txtPrice, 0.0},
                {btnAddLine, 0.0}};
        for (int i = 0; i < entry.length; i++) {
            gc.gridx = i;
            gc.weightx = (Double) entry[i][1];
            pnlLineEntry.add((Component) entry[i][0], gc);
        }
        int inputHeight = txtLot.getPreferredSize().height;
        txtLot.setPreferredSize(new Dimension(120, inputHeight));
        txtLot.setMinimumSize(txtLot.getPreferredSize());
        txtExpiry.setPreferredSize(new Dimension(120, inputHeight));
        txtExpiry.setMinimumSize(txtExpiry.getPreferredSize());
        txtPrice.setPreferredSize(new Dimension(130, inputHeight));
        txtPrice.setMinimumSize(txtPrice.getPreferredSize());
        spnQuantity.setPreferredSize(new Dimension(90, inputHeight));
        spnQuantity.setMinimumSize(spnQuantity.getPreferredSize());
        cmbItem.setPreferredSize(new Dimension(260, inputHeight));
        cmbItem.setMinimumSize(new Dimension(180, inputHeight));
        txtLot.setToolTipText("Số lô in trên bao bì");
        spnQuantity.setToolTipText("Số lượng");

        tblLines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(tblLines, 40, new int[] {44, 280, 100, 135, 90, 120, 140}, Set.of(0, 4, 5, 6));
        JScrollPane scrLines = tableScroll(tblLines);

        JPanel pnlLinesFooter = new JPanel(new BorderLayout());
        pnlLinesFooter.setOpaque(false);
        JPanel pnlLineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLineButtons.setOpaque(false);
        pnlLineButtons.add(btnRemoveLine);
        lblTotal.setFont(font(Font.BOLD, 16));
        lblTotal.setForeground(TEXT);
        pnlLinesFooter.add(pnlLineButtons, BorderLayout.WEST);
        pnlLinesFooter.add(lblTotal, BorderLayout.EAST);

        JPanel pnlLines = new JPanel(new BorderLayout(0, 10));
        pnlLines.setOpaque(false);
        JLabel lblLinesTitle = new JLabel("DÒNG HÀNG — chọn mặt hàng, nhập số lô, hạn dùng (bắt buộc với thuốc), "
                + "số lượng, đơn giá rồi bấm \"Thêm dòng\"");
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
        btnAddLine.addActionListener(e -> addLine());
        btnRemoveLine.addActionListener(e -> {
            int row = tblLines.getSelectedRow();
            if (row >= 0) {
                lineModel.remove(row);
                refreshTotal();
            }
        });
        btnAddSupplier.addActionListener(e -> createSupplier());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        cmbItem.addActionListener(e -> {
            InventoryItemResponse item = (InventoryItemResponse) cmbItem.getSelectedItem();
            ((HintTextField) txtExpiry).setToolTipText(item != null && item.loai() == InventoryItemType.THUOC
                    ? "Bắt buộc với thuốc" : "Không bắt buộc với vật tư");
        });
    }

    // ----------------------------------------------------------------- dữ liệu

    private void loadChoices() {
        Set<Role> roles = session.get().roles();
        setBusy(true);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                return new Object[] {service.listSuppliers(roles), itemService.search(roles, "", null)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                setBusy(false);
                try {
                    Object[] result = get();
                    fillSuppliers((List<SupplierResponse>) result[0],
                            existing == null ? null : existing.maNhaCungCap());
                    cmbItem.removeAllItems();
                    for (InventoryItemResponse item : (List<InventoryItemResponse>) result[1]) {
                        cmbItem.addItem(item);
                    }
                    if (cmbItem.getItemCount() == 0) {
                        lblError.setText("Danh mục chưa có mặt hàng — thêm ở mục Vật tư & thuốc trước.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void fillSuppliers(List<SupplierResponse> suppliers, String selectCode) {
        cmbSupplier.removeAllItems();
        for (SupplierResponse s : suppliers) {
            if (s.dangHopTac() || s.ma().equals(selectCode)) {
                cmbSupplier.addItem(s);
                if (s.ma().equals(selectCode)) {
                    cmbSupplier.setSelectedItem(s);
                }
            }
        }
        if (cmbSupplier.getItemCount() == 0) {
            lblError.setText("Chưa có nhà cung cấp — bấm \"+ Nhà cung cấp\" để thêm.");
        }
    }

    private void addLine() {
        lblError.setText(" ");
        if (!commitSpinners(lblError, spnQuantity)) {
            return;
        }
        InventoryItemResponse item = (InventoryItemResponse) cmbItem.getSelectedItem();
        if (item == null) {
            lblError.setText("Chưa chọn mặt hàng.");
            return;
        }
        try {
            String lot = txtLot.getText().trim().toUpperCase(java.util.Locale.ROOT);
            if (lot.isEmpty()) {
                throw new IllegalArgumentException("Chưa nhập số lô.");
            }
            LocalDate expiry = InventoryFormat.parseDate(txtExpiry.getText());
            if (item.loai() == InventoryItemType.THUOC && expiry == null) {
                throw new IllegalArgumentException("Thuốc bắt buộc có hạn sử dụng.");
            }
            BigDecimal price = InventoryFormat.parseMoney(txtPrice.getText());
            int quantity = ((Number) spnQuantity.getValue()).intValue();
            lineModel.add(new DraftLine(item.loai(), item.ma(), item.ten(), item.donViTinh(), lot, expiry,
                    quantity, price));
            refreshTotal();
            txtLot.setText("");
            txtExpiry.setText("");
            txtPrice.setText("");
            spnQuantity.setValue(1);
            txtLot.requestFocusInWindow();
        } catch (IllegalArgumentException ex) {
            lblError.setText(ex.getMessage());
        }
    }

    private void refreshTotal() {
        lblTotal.setText("Tổng tiền: " + InventoryFormat.money(lineModel.total()));
    }

    private void createSupplier() {
        JTextField txtName = styleInput(new JTextField(24));
        JTextField txtPhone = styleInput(new JTextField(14));
        JTextField txtAddress = styleInput(new JTextField(24));
        JTextField txtEmail = styleInput(new JTextField(24));
        JPanel pnlForm = new JPanel(new GridBagLayout());
        addField(pnlForm, 0, 0, "Tên *", txtName);
        addField(pnlForm, 1, 0, "Số điện thoại *", txtPhone);
        addField(pnlForm, 2, 0, "Địa chỉ", txtAddress);
        addField(pnlForm, 3, 0, "Email", txtEmail);
        int answer = JOptionPane.showConfirmDialog(this, pnlForm, "Thêm nhà cung cấp",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        SupplierRequest request = new SupplierRequest(txtName.getText(), txtPhone.getText(),
                txtAddress.getText(), txtEmail.getText());
        Set<Role> roles = session.get().roles();
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                SupplierResponse created = service.createSupplier(roles, request);
                return new Object[] {created, service.listSuppliers(roles)};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    fillSuppliers((List<SupplierResponse>) result[1], ((SupplierResponse) result[0]).ma());
                    lblError.setText(" ");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void save() {
        if (!commitSpinners(lblError, spnDate)) {
            return;
        }
        SupplierResponse supplier = (SupplierResponse) cmbSupplier.getSelectedItem();
        List<StockInLineRequest> lines = new ArrayList<>();
        for (DraftLine d : lineModel.rows) {
            lines.add(new StockInLineRequest(d.loai(), d.ma(), d.soLo(), d.hanSuDung(), d.soLuong(), d.donGia()));
        }
        StockInRequest request = new StockInRequest(toLocalDate((Date) spnDate.getValue()),
                supplier == null ? null : supplier.ma(), txtNote.getText(), lines);
        InventoryActor actor = session.get();
        setBusy(true);
        lblError.setText(" ");
        new SwingWorker<StockInDetailResponse, Void>() {
            @Override
            protected StockInDetailResponse doInBackground() {
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
        btnAddSupplier.setEnabled(!busy);
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
        private final transient List<DraftLine> rows = new ArrayList<>();

        void add(DraftLine line) {
            rows.add(line);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        void remove(int row) {
            rows.remove(row);
            fireTableDataChanged();
        }

        BigDecimal total() {
            return rows.stream().map(DraftLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
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
            DraftLine d = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> d.ma() + " — " + d.ten();
                case 2 -> d.soLo();
                case 3 -> InventoryFormat.date(d.hanSuDung());
                case 4 -> d.soLuong() + " " + d.donVi();
                case 5 -> InventoryFormat.money(d.donGia());
                default -> InventoryFormat.money(d.thanhTien());
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
                lblItem.setText(item.ma() + " — " + item.ten() + " (" + item.donViTinh() + ")");
            } else if (value == null) {
                lblItem.setText("Chọn mặt hàng");
            }
            return lblItem;
        }
    }
}
