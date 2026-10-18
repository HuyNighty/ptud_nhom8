package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.request.KitRestockLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import java.awt.BorderLayout;
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

/**
 * Bổ sung (cộng thêm) hoặc thay thế (thay hàng cận/hết hạn) cho hộp cấp cứu bằng hàng trong kho. Hệ thống lập một phiếu
 * xuất kho "Phục vụ phòng khám" và xuất ngay theo FEFO, chỉ lấy lô còn hạn sau ngưỡng của hộp.
 */
final class EmergencyKitRestockDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"#", "Mặt hàng", "ĐVT", "Tối thiểu", "Hiện có", "Hạn dùng hiện tại",
            "Đánh giá", "Kho dùng được", "Lấy từ kho"};
    private static final int COL_RESULT = 6;
    private static final int COL_TAKE = 8;

    private final transient EmergencyKitService service;
    private final transient Supplier<InventoryActor> session;
    private final transient EmergencyKitDetailResponse detail;
    private final KitLogType mode;
    private final TakeModel model = new TakeModel();
    private final JTable tblTake = new JTable(model);
    private final JTextField txtNote = new JTextField(40);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave;
    private boolean changed;

    static boolean open(Window owner, EmergencyKitService service, Supplier<InventoryActor> session,
                        EmergencyKitDetailResponse detail, KitLogType mode) {
        if (detail.matHang().isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Hộp " + detail.maHop() + " chưa có mặt hàng nào — bấm \"Sửa\" để "
                    + "khai báo thành phần hộp trước.", "Hộp cấp cứu", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        EmergencyKitRestockDialog dlg = new EmergencyKitRestockDialog(owner, service, session, detail, mode);
        dlg.setVisible(true);
        return dlg.changed;
    }

    private EmergencyKitRestockDialog(Window owner, EmergencyKitService service, Supplier<InventoryActor> session,
                                      EmergencyKitDetailResponse detail, KitLogType mode) {
        super(owner, (mode == KitLogType.BO_SUNG ? "Bổ sung hộp " : "Thay thế hàng trong hộp ") + detail.maHop(),
                ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.detail = detail;
        this.mode = mode;
        this.btnSave = createButton(mode == KitLogType.BO_SUNG ? "Xuất kho & bổ sung" : "Xuất kho & thay thế",
                ButtonKind.PRIMARY);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        model.load(detail.matHang());
        setContentPane(buildContent());
        setSize(1180, 600);
        setLocationRelativeTo(owner);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 12));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle() + " — " + detail.tenHop());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        String rule = mode == KitLogType.BO_SUNG
                ? "Số lấy từ kho được <b>cộng thêm</b> vào số hiện có."
                : "Hàng cũ (cận/hết hạn) được lấy ra; số hiện có <b>được thay bằng</b> số lấy từ kho.";
        JLabel lblInfo = new JLabel("<html>" + rule + " Hệ thống lập phiếu xuất kho \"Phục vụ phòng khám\" và xuất ngay "
                + "theo FEFO, chỉ lấy lô có hạn sau <b>" + InventoryFormat.date(LocalDate.now()
                .plusDays(detail.nguongHanDungNgay())) + "</b>. Thuốc kiểm soát đặc biệt phải xuất ở màn Xuất kho "
                + "(phê duyệt kép).</html>");
        lblInfo.setFont(font(Font.PLAIN, 14));
        lblInfo.setForeground(TEXT_MUTED);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(lblInfo, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        styleTable(tblTake, 38, new int[] {40, 300, 65, 85, 80, 135, 100, 135, 105}, Set.of(0, 3, 4, 7, 8));
        tblTake.setSurrendersFocusOnKeystroke(true);
        tblTake.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblTake.getColumnModel().getColumn(COL_RESULT).setCellRenderer(new EmergencyKitCheckDialog.ResultRenderer());
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(tableScroll(tblTake), BorderLayout.CENTER);
        JPanel pnlNote = new JPanel(new BorderLayout(10, 0));
        pnlNote.setOpaque(false);
        txtNote.setFont(font(Font.PLAIN, 14));
        pnlNote.add(fieldLabel("Ghi chú"), BorderLayout.WEST);
        pnlNote.add(txtNote, BorderLayout.CENTER);
        pnlCenter.add(pnlNote, BorderLayout.SOUTH);
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

    private void save() {
        if (tblTake.isEditing() && !tblTake.getCellEditor().stopCellEditing()) {
            return;
        }
        List<KitRestockLineRequest> lines = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < model.items.size(); i++) {
            int take = model.take.get(i);
            if (take > 0) {
                lines.add(new KitRestockLineRequest(model.items.get(i).maChiTiet(), take));
                total += take;
            }
        }
        if (lines.isEmpty()) {
            lblError.setText("Nhập số lượng cần lấy từ kho cho ít nhất một mặt hàng.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Xuất " + total + " đơn vị (" + lines.size()
                        + " mặt hàng) từ kho cho hộp " + detail.maHop() + "?\nTồn kho bị trừ ngay theo phiếu xuất.",
                getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        KitRestockRequest request = new KitRestockRequest(mode, lines, txtNote.getText());
        InventoryActor actor = session.get();
        String ma = detail.maHop();
        btnSave.setEnabled(false);
        lblError.setText(" ");
        new SwingWorker<EmergencyKitDetailResponse, Void>() {
            @Override
            protected EmergencyKitDetailResponse doInBackground() {
                return service.restock(actor, ma, request);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    EmergencyKitDetailResponse after = get();
                    changed = true;
                    JOptionPane.showMessageDialog(EmergencyKitRestockDialog.this, "Đã xuất kho theo phiếu "
                                    + after.nhatKy().get(0).maPhieuXuat() + ". Hộp " + ma + " hiện: "
                                    + after.trangThai().label() + ".", getTitle(), JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private final class TakeModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<EmergencyKitItemResponse> items = new ArrayList<>();
        private final List<Integer> take = new ArrayList<>();

        /** Gợi ý sẵn: bổ sung phần thiếu; thay thế đủ số tối thiểu cho mặt hàng cận/hết hạn — không vượt tồn dùng được. */
        void load(List<EmergencyKitItemResponse> data) {
            items.clear();
            take.clear();
            for (EmergencyKitItemResponse i : data) {
                items.add(i);
                int suggested = 0;
                if (!i.kiemSoatDacBiet()) {
                    boolean expiry = i.danhGia() == KitCheckResult.CAN_HAN || i.danhGia() == KitCheckResult.HET_HAN;
                    suggested = mode == KitLogType.BO_SUNG ? i.soLuongThieu()
                            : (expiry ? Math.max(i.soLuongToiThieu(), i.soLuongHienCo()) : 0);
                    suggested = Math.min(suggested, i.tonKhoDung());
                }
                take.add(suggested);
            }
            fireTableDataChanged();
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
            return column == COL_TAKE && !items.get(row).kiemSoatDacBiet();
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            String text = value == null ? "" : value.toString().trim();
            try {
                int number = text.isEmpty() ? 0 : Integer.parseInt(text.replace(".", "").replace(",", ""));
                if (number < 0) {
                    throw new NumberFormatException();
                }
                take.set(row, number);
                lblError.setText(number > items.get(row).tonKhoDung()
                        ? items.get(row).tenMatHang() + ": kho chỉ còn " + items.get(row).tonKhoDung()
                        + " dùng được — phiếu sẽ không lập được." : " ");
                fireTableRowsUpdated(row, row);
            } catch (NumberFormatException e) {
                lblError.setText(items.get(row).tenMatHang() + ": \"" + text + "\" không phải số lượng hợp lệ.");
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            EmergencyKitItemResponse i = items.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> i.maMatHang() + " — " + i.tenMatHang() + (i.kiemSoatDacBiet() ? " (KSĐB)" : "");
                case 2 -> i.donViTinh();
                case 3 -> i.soLuongToiThieu();
                case 4 -> i.soLuongHienCo();
                case 5 -> InventoryFormat.date(i.hanDung());
                case COL_RESULT -> i.danhGia();
                case 7 -> i.tonKhoDung();
                default -> i.kiemSoatDacBiet() ? "—" : String.valueOf(take.get(row));
            };
        }
    }
}
