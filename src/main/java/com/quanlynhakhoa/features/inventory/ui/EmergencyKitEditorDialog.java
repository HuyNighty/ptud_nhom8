package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Tạo / sửa hộp cấp cứu: thông tin hộp, tham số kiểm tra và danh sách mặt hàng với số lượng tối thiểu. */
final class EmergencyKitEditorDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"#", "Mặt hàng", "ĐVT", "Số tối thiểu", "Hiện có", ""};
    private static final int COL_MIN = 3;
    private static final int COL_REMOVE = 5;

    private final transient EmergencyKitService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;
    private final String maHop;
    private transient EmergencyKitDetailResponse saved;

    private final JTextField txtName = new JTextField(28);
    private final JComboBox<EmergencyKitType> cmbType = new JComboBox<>(EmergencyKitType.values());
    private final JComboBox<String> cmbLocation = new JComboBox<>();
    private final JSpinner spnCycle = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
    private final JSpinner spnThreshold = new JSpinner(new SpinnerNumberModel(30, 0, 365, 1));
    private final JTextField txtNote = new JTextField(28);
    private final JComboBox<Object> cmbItem = new JComboBox<>();
    private final JSpinner spnMin = new JSpinner(new SpinnerNumberModel(1, 1, 100_000, 1));
    private final JButton btnAdd = createButton("Thêm vào hộp", ButtonKind.OUTLINE);
    private final CompositionModel model = new CompositionModel();
    private final JTable tblItems = new JTable(model);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu hộp", ButtonKind.PRIMARY);

    private record Line(InventoryItemType loai, String ma, String ten, String donVi, int toiThieu, Integer hienCo) {
    }

    private record Choice(InventoryItemResponse item) {
        @Override
        public String toString() {
            String ten = item.ten() + (item.hamLuong() == null || item.hamLuong().isBlank() ? "" : " " + item.hamLuong());
            return item.ma() + " — " + ten + " (" + item.donViTinh() + ")";
        }
    }

    EmergencyKitEditorDialog(Window owner, EmergencyKitService service, InventoryItemService itemService,
                             Supplier<InventoryActor> session, String maHop, List<String> knownLocations) {
        super(owner, maHop == null ? "Tạo hộp cấp cứu" : "Sửa hộp " + maHop, ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        this.maHop = maHop;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        cmbLocation.setEditable(true);
        knownLocations.forEach(cmbLocation::addItem);
        cmbLocation.setSelectedItem("");
        setContentPane(buildContent());
        setSize(980, 700);
        setLocationRelativeTo(owner);
        load();
    }

    Optional<EmergencyKitDetailResponse> saved() {
        return Optional.ofNullable(saved);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 14));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);

        styleCombo(cmbType);
        for (JComponent c : new JComponent[] {txtName, cmbType, cmbLocation, spnCycle, spnThreshold, txtNote}) {
            c.setFont(font(Font.PLAIN, 14));
        }
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        field(pnlForm, 0, 0, "Tên hộp *", txtName);
        field(pnlForm, 0, 2, "Loại hộp *", cmbType);
        field(pnlForm, 1, 0, "Vị trí *", cmbLocation);
        field(pnlForm, 1, 2, "Ghi chú", txtNote);
        field(pnlForm, 2, 0, "Chu kỳ kiểm tra (ngày)", spnCycle);
        field(pnlForm, 2, 2, "Ngưỡng cận hạn (ngày)", spnThreshold);
        JLabel lblHint = new JLabel("Mặt hàng đạt khi số hiện có ≥ số tối thiểu và hạn dùng còn hơn ngưỡng cận hạn "
                + "(mặc định 30 ngày); kiểm tra lại sau mỗi chu kỳ (mặc định 7 ngày).");
        lblHint.setFont(font(Font.PLAIN, 13));
        lblHint.setForeground(TEXT_MUTED);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlForm, BorderLayout.CENTER);
        pnlTop.add(lblHint, BorderLayout.SOUTH);

        styleCombo(cmbItem);
        cmbItem.setFont(font(Font.PLAIN, 14));
        cmbItem.setPreferredSize(new Dimension(520, 36));
        cmbItem.addItem("Đang tải danh mục…");
        spnMin.setPreferredSize(new Dimension(90, 36));
        JPanel pnlAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlAdd.setOpaque(false);
        pnlAdd.add(fieldLabel("Mặt hàng"));
        pnlAdd.add(cmbItem);
        pnlAdd.add(fieldLabel("Tối thiểu"));
        pnlAdd.add(spnMin);
        pnlAdd.add(btnAdd);
        btnAdd.addActionListener(e -> addItem());

        styleTable(tblItems, 36, new int[] {40, 440, 80, 120, 100, 70}, Set.of(0, 3, 4));
        tblItems.setSurrendersFocusOnKeystroke(true);
        tblItems.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblItems.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblItems.rowAtPoint(e.getPoint());
                if (row >= 0 && tblItems.columnAtPoint(e.getPoint()) == COL_REMOVE) {
                    model.remove(row);
                }
            }
        });
        tblItems.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hand = tblItems.rowAtPoint(e.getPoint()) >= 0
                        && tblItems.columnAtPoint(e.getPoint()) == COL_REMOVE;
                tblItems.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        JLabel lblItems = new JLabel("MẶT HÀNG TRONG HỘP (sửa trực tiếp số tối thiểu trong bảng)");
        lblItems.setFont(font(Font.PLAIN, 13));
        lblItems.setForeground(TEXT_MUTED);
        JPanel pnlItemsHeader = new JPanel(new BorderLayout(0, 8));
        pnlItemsHeader.setOpaque(false);
        pnlItemsHeader.add(pnlAdd, BorderLayout.NORTH);
        pnlItemsHeader.add(lblItems, BorderLayout.SOUTH);
        JPanel pnlItems = new JPanel(new BorderLayout(0, 8));
        pnlItems.setOpaque(false);
        pnlItems.add(pnlItemsHeader, BorderLayout.NORTH);
        pnlItems.add(tableScroll(tblItems), BorderLayout.CENTER);

        JPanel pnlCenter = new JPanel(new BorderLayout(0, 16));
        pnlCenter.setOpaque(false);
        pnlCenter.add(pnlTop, BorderLayout.NORTH);
        pnlCenter.add(pnlItems, BorderLayout.CENTER);
        pnlRoot.add(pnlCenter, BorderLayout.CENTER);

        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
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

    private static void field(JPanel target, int row, int column, String label, JComponent input) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = column;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(4, column == 0 ? 0 : 24, 4, 10);
        target.add(fieldLabel(label), gc);
        gc.gridx = column + 1;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        input.setPreferredSize(new Dimension(260, 34));
        target.add(input, gc);
    }

    private void load() {
        Set<Role> roles = session.get().roles();
        btnSave.setEnabled(false);
        btnAdd.setEnabled(false);
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                List<InventoryItemResponse> catalog = itemService.search(roles, "", null);
                EmergencyKitDetailResponse detail = maHop == null ? null : service.getDetail(roles, maHop);
                return new Object[] {catalog, detail};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    cmbItem.removeAllItems();
                    for (InventoryItemResponse item : (List<InventoryItemResponse>) result[0]) {
                        cmbItem.addItem(new Choice(item));
                    }
                    if (result[1] instanceof EmergencyKitDetailResponse d) {
                        fill(d);
                    }
                    btnSave.setEnabled(true);
                    btnAdd.setEnabled(cmbItem.getItemCount() > 0);
                    if (cmbItem.getItemCount() == 0) {
                        lblError.setText("Danh mục chưa có thuốc / vật tư — thêm ở mục Vật tư & thuốc trước.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void fill(EmergencyKitDetailResponse d) {
        txtName.setText(d.tenHop());
        cmbType.setSelectedItem(d.loai());
        cmbLocation.setSelectedItem(d.viTri());
        spnCycle.setValue(d.chuKyKiemTraNgay());
        spnThreshold.setValue(d.nguongHanDungNgay());
        txtNote.setText(d.ghiChu() == null ? "" : d.ghiChu());
        List<Line> lines = new ArrayList<>();
        for (EmergencyKitItemResponse i : d.matHang()) {
            lines.add(new Line(i.loai(), i.maMatHang(), i.tenMatHang(), i.donViTinh(), i.soLuongToiThieu(),
                    i.soLuongHienCo()));
        }
        model.setLines(lines);
    }

    private void addItem() {
        if (!(cmbItem.getSelectedItem() instanceof Choice choice)) {
            return;
        }
        InventoryItemResponse item = choice.item();
        if (model.contains(item.loai(), item.ma())) {
            lblError.setText(item.ma() + " đã có trong hộp — sửa số tối thiểu trực tiếp trong bảng.");
            return;
        }
        try {
            spnMin.commitEdit();
        } catch (java.text.ParseException e) {
            lblError.setText("Số tối thiểu không hợp lệ.");
            return;
        }
        String ten = item.ten() + (item.hamLuong() == null || item.hamLuong().isBlank() ? "" : " " + item.hamLuong());
        model.add(new Line(item.loai(), item.ma(), ten, item.donViTinh(), (Integer) spnMin.getValue(), null));
        lblError.setText(" ");
    }

    private void save() {
        if (tblItems.isEditing() && !tblItems.getCellEditor().stopCellEditing()) {
            return;
        }
        if (!commitSpinners(lblError, spnCycle, spnThreshold)) {
            return;
        }
        Object location = cmbLocation.getEditor().getItem();
        List<EmergencyKitItemRequest> items = new ArrayList<>();
        for (Line l : model.lines) {
            items.add(new EmergencyKitItemRequest(l.loai(), l.ma(), l.toiThieu()));
        }
        EmergencyKitRequest request = new EmergencyKitRequest(txtName.getText(),
                (EmergencyKitType) cmbType.getSelectedItem(), location == null ? "" : location.toString(),
                (Integer) spnCycle.getValue(), (Integer) spnThreshold.getValue(), txtNote.getText(), items);
        InventoryActor actor = session.get();
        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu…");
        lblError.setText(" ");
        new SwingWorker<EmergencyKitDetailResponse, Void>() {
            @Override
            protected EmergencyKitDetailResponse doInBackground() {
                return maHop == null ? service.create(actor, request) : service.update(actor, maHop, request);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu hộp");
                try {
                    saved = get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private final class CompositionModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<Line> lines = new ArrayList<>();

        void setLines(List<Line> data) {
            lines.clear();
            lines.addAll(data);
            fireTableDataChanged();
        }

        boolean contains(InventoryItemType loai, String ma) {
            return lines.stream().anyMatch(l -> l.loai() == loai && l.ma().equals(ma));
        }

        void add(Line line) {
            lines.add(line);
            fireTableRowsInserted(lines.size() - 1, lines.size() - 1);
        }

        void remove(int row) {
            lines.remove(row);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_MIN;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != COL_MIN) {
                return;
            }
            String text = value == null ? "" : value.toString().trim();
            try {
                int number = Integer.parseInt(text.replace(".", "").replace(",", ""));
                if (number <= 0) {
                    throw new NumberFormatException();
                }
                Line l = lines.get(row);
                lines.set(row, new Line(l.loai(), l.ma(), l.ten(), l.donVi(), number, l.hienCo()));
                lblError.setText(" ");
                fireTableRowsUpdated(row, row);
            } catch (NumberFormatException e) {
                lblError.setText("Số tối thiểu phải là số nguyên dương.");
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            Line l = lines.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.ma() + " — " + l.ten();
                case 2 -> l.donVi();
                case COL_MIN -> l.toiThieu();
                case 4 -> l.hienCo() == null ? "Mới" : String.valueOf(l.hienCo());
                default -> "Xóa";
            };
        }
    }
}
