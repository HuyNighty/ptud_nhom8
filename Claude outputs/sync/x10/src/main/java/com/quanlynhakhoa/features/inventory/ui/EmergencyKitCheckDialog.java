package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.request.KitCheckLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Kiểm tra hộp cấp cứu (UC34): nhập số thực tế và hạn dùng gần nhất của từng mặt hàng; mục đạt khi đủ số tối thiểu
 * và hạn còn hơn ngưỡng của hộp. Kết quả được lưu vào nhật ký, không làm thay đổi tồn kho.
 */
final class EmergencyKitCheckDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"#", "Mặt hàng", "ĐVT", "Tối thiểu", "Đang ghi nhận", "Số thực tế",
            "HSD gần nhất", "Đánh giá"};
    private static final int COL_ACTUAL = 5;
    private static final int COL_EXPIRY = 6;
    private static final int COL_RESULT = 7;

    private final transient EmergencyKitService service;
    private final transient Supplier<InventoryActor> session;
    private final transient EmergencyKitDetailResponse detail;
    private final CheckModel model = new CheckModel();
    private final JTable tblCheck = new JTable(model);
    private final JTextField txtNote = new JTextField(40);
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu kết quả kiểm tra", ButtonKind.PRIMARY);
    private boolean changed;

    static boolean open(Window owner, EmergencyKitService service, Supplier<InventoryActor> session,
                        EmergencyKitDetailResponse detail) {
        if (detail.matHang().isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Hộp " + detail.maHop() + " chưa có mặt hàng nào — bấm \"Sửa\" để "
                    + "khai báo thành phần hộp trước.", "Kiểm tra hộp cấp cứu", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        EmergencyKitCheckDialog dlg = new EmergencyKitCheckDialog(owner, service, session, detail);
        dlg.setVisible(true);
        return dlg.changed;
    }

    private EmergencyKitCheckDialog(Window owner, EmergencyKitService service, Supplier<InventoryActor> session,
                                    EmergencyKitDetailResponse detail) {
        super(owner, "Kiểm tra hộp " + detail.maHop(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.detail = detail;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        model.load(detail.matHang());
        setContentPane(buildContent());
        refreshSummary();
        setSize(1160, 620);
        setLocationRelativeTo(owner);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 12));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle() + " — " + detail.tenHop());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        JLabel lblInfo = new JLabel("<html>" + EmergencyKitPanel.escape(detail.viTri()) + " · Đạt khi số thực tế ≥ "
                + "tối thiểu và hạn dùng sau <b>" + InventoryFormat.date(LocalDate.now()
                .plusDays(detail.nguongHanDungNgay())) + "</b> (ngưỡng " + detail.nguongHanDungNgay() + " ngày). "
                + "Nhập ngày dạng dd/MM/yyyy; vật tư không có hạn dùng để trống.</html>");
        lblInfo.setFont(font(Font.PLAIN, 14));
        lblInfo.setForeground(TEXT_MUTED);
        JButton btnFill = createButton("Điền theo số đang ghi nhận", ButtonKind.OUTLINE);
        btnFill.addActionListener(e -> {
            model.fillFromRecorded();
            refreshSummary();
        });
        JPanel pnlTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlTools.setOpaque(false);
        pnlTools.add(btnFill);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(lblInfo, BorderLayout.CENTER);
        pnlTop.add(pnlTools, BorderLayout.SOUTH);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        styleTable(tblCheck, 38, new int[] {40, 300, 70, 85, 185, 105, 130, 105}, Set.of(0, 3, 4, 5));
        tblCheck.setSurrendersFocusOnKeystroke(true);
        tblCheck.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblCheck.getColumnModel().getColumn(COL_RESULT).setCellRenderer(new ResultRenderer());
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(tableScroll(tblCheck), BorderLayout.CENTER);
        JPanel pnlNote = new JPanel(new BorderLayout(10, 0));
        pnlNote.setOpaque(false);
        txtNote.setFont(font(Font.PLAIN, 14));
        pnlNote.add(fieldLabel("Ghi chú"), BorderLayout.WEST);
        pnlNote.add(txtNote, BorderLayout.CENTER);
        pnlCenter.add(pnlNote, BorderLayout.SOUTH);
        pnlRoot.add(pnlCenter, BorderLayout.CENTER);

        lblSummary.setFont(font(Font.BOLD, 15));
        lblSummary.setForeground(TEXT);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);
        JPanel pnlMessages = new JPanel(new BorderLayout(0, 4));
        pnlMessages.setOpaque(false);
        pnlMessages.add(lblSummary, BorderLayout.NORTH);
        pnlMessages.add(lblError, BorderLayout.SOUTH);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(pnlMessages, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private void refreshSummary() {
        int entered = 0;
        int passed = 0;
        for (int i = 0; i < model.items.size(); i++) {
            KitCheckResult r = model.result(i);
            if (r != null) {
                entered++;
                if (r == KitCheckResult.DAT) {
                    passed++;
                }
            }
        }
        lblSummary.setText("Đã nhập " + entered + "/" + model.items.size() + " mặt hàng · Đạt " + passed + "/"
                + model.items.size());
    }

    private void save() {
        if (tblCheck.isEditing() && !tblCheck.getCellEditor().stopCellEditing()) {
            return;
        }
        List<KitCheckLineRequest> lines = new ArrayList<>();
        for (int i = 0; i < model.items.size(); i++) {
            lines.add(new KitCheckLineRequest(model.items.get(i).maChiTiet(), model.actual.get(i), model.expiry.get(i)));
        }
        KitCheckRequest request = new KitCheckRequest(lines, txtNote.getText());
        InventoryActor actor = session.get();
        String ma = detail.maHop();
        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu…");
        lblError.setText(" ");
        new SwingWorker<EmergencyKitDetailResponse, Void>() {
            @Override
            protected EmergencyKitDetailResponse doInBackground() {
                return service.check(actor, ma, request);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu kết quả kiểm tra");
                try {
                    EmergencyKitDetailResponse after = get();
                    changed = true;
                    boolean pass = Boolean.TRUE.equals(after.nhatKy().get(0).dat());
                    JOptionPane.showMessageDialog(EmergencyKitCheckDialog.this, pass
                                    ? "Hộp " + ma + " ĐẠT. Lần kiểm tra tiếp theo trước "
                                    + InventoryFormat.date(after.hanKiemTraTiep()) + "."
                                    : "Hộp " + ma + " KHÔNG ĐẠT (" + after.trangThai().label() + "). "
                                    + "Dùng \"Bổ sung\" hoặc \"Thay thế\" để lấy hàng từ kho.",
                            "Kết quả kiểm tra", pass ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private final class CheckModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<EmergencyKitItemResponse> items = new ArrayList<>();
        private final List<Integer> actual = new ArrayList<>();
        private final List<LocalDate> expiry = new ArrayList<>();

        void load(List<EmergencyKitItemResponse> data) {
            items.clear();
            actual.clear();
            expiry.clear();
            for (EmergencyKitItemResponse i : data) {
                items.add(i);
                actual.add(null);
                expiry.add(null);
            }
            fireTableDataChanged();
        }

        void fillFromRecorded() {
            for (int i = 0; i < items.size(); i++) {
                actual.set(i, items.get(i).soLuongHienCo());
                expiry.set(i, items.get(i).hanDung());
            }
            fireTableDataChanged();
        }

        KitCheckResult result(int row) {
            Integer qty = actual.get(row);
            if (qty == null) {
                return null;
            }
            EmergencyKitItemResponse i = items.get(row);
            LocalDate today = LocalDate.now();
            LocalDate han = expiry.get(row);
            if (qty > 0 && han != null) {
                if (han.isBefore(today)) {
                    return KitCheckResult.HET_HAN;
                }
                if (!han.isAfter(today.plusDays(detail.nguongHanDungNgay()))) {
                    return KitCheckResult.CAN_HAN;
                }
            }
            return qty < i.soLuongToiThieu() ? KitCheckResult.THIEU : KitCheckResult.DAT;
        }

        @Override
        public int getRowCount() {
            return items.size();
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
            return column == COL_ACTUAL || column == COL_EXPIRY;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            String text = value == null ? "" : value.toString().trim();
            String name = items.get(row).tenMatHang();
            if (column == COL_ACTUAL) {
                if (text.isEmpty()) {
                    actual.set(row, null);
                } else {
                    try {
                        int number = Integer.parseInt(text.replace(".", "").replace(",", ""));
                        if (number < 0) {
                            throw new NumberFormatException();
                        }
                        actual.set(row, number);
                        lblError.setText(" ");
                    } catch (NumberFormatException e) {
                        lblError.setText(name + ": \"" + text + "\" không phải số lượng hợp lệ.");
                        return;
                    }
                }
            } else if (column == COL_EXPIRY) {
                if (text.isEmpty()) {
                    expiry.set(row, null);
                } else {
                    try {
                        expiry.set(row, InventoryFormat.parseDate(text));
                        lblError.setText(" ");
                    } catch (IllegalArgumentException e) {
                        lblError.setText(name + ": hạn dùng \"" + text + "\" không đúng dạng dd/MM/yyyy.");
                        return;
                    }
                }
            }
            fireTableRowsUpdated(row, row);
            refreshSummary();
        }

        @Override
        public Object getValueAt(int row, int column) {
            EmergencyKitItemResponse i = items.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> i.maMatHang() + " — " + i.tenMatHang();
                case 2 -> i.donViTinh();
                case 3 -> i.soLuongToiThieu();
                case 4 -> i.soLuongHienCo() + (i.hanDung() == null ? "" : " · HSD " + InventoryFormat.date(i.hanDung()));
                case COL_ACTUAL -> actual.get(row) == null ? "" : String.valueOf(actual.get(row));
                case COL_EXPIRY -> expiry.get(row) == null ? "" : InventoryFormat.date(expiry.get(row));
                default -> result(row);
            };
        }
    }

    /** Đánh giá: Đạt xanh, Thiếu cam, Cận/Hết hạn đỏ, chưa nhập xám. */
    static class ResultRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            lbl.setFont(font(Font.BOLD, 14));
            if (value instanceof KitCheckResult r) {
                lbl.setText(r.label());
                lbl.setForeground(color(r));
            } else {
                lbl.setText("Chưa nhập");
                lbl.setForeground(TEXT_MUTED);
                lbl.setFont(font(Font.PLAIN, 14));
            }
            return lbl;
        }

        static Color color(KitCheckResult r) {
            return switch (r) {
                case DAT -> GREEN;
                case THIEU -> ORANGE;
                case CAN_HAN, HET_HAN -> RED;
            };
        }
    }
}
