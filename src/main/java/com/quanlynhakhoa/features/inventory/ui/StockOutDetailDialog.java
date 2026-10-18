package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutLineResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Xem chi tiết phiếu xuất; các nút thao tác hiện theo trạng thái phiếu. */
final class StockOutDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS =
            {"#", "Mã", "Mặt hàng", "Số lô", "HSD", "Tồn lô", "SL", "ĐVT", "Giá vốn", "Thành tiền"};

    private final transient StockOutService service;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openEditor;
    private transient StockOutDetailResponse detail;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JLabel lblError = new JLabel(" ");

    /** @param openEditor mở hộp thoại sửa phiếu (do màn danh sách cung cấp) */
    StockOutDetailDialog(Window owner, StockOutService service, Supplier<InventoryActor> session,
                        StockOutDetailResponse detail, Consumer<String> openEditor) {
        super(owner, "Phiếu xuất " + detail.maPhieu(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.openEditor = Objects.requireNonNull(openEditor);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(pnlBody);
        render(detail);
        setSize(1080, 620);
        setLocationRelativeTo(owner);
    }

    /** Có thay đổi dữ liệu (xuất kho / hủy / sửa) → màn danh sách cần tải lại. */
    boolean changed() {
        return changed;
    }

    private void render(StockOutDetailResponse d) {
        this.detail = d;
        pnlBody.removeAll();

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Phiếu xuất " + d.maPhieu());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        lblStatus.apply(d.trangThai().label(), InventoryFormat.statusDot(d.trangThai()),
                InventoryFormat.statusFill(d.trangThai()), InventoryFormat.statusText(d.trangThai()));
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Ngày xuất", InventoryFormat.date(d.ngayXuat()));
        info(pnlInfo, 0, 2, "Lý do xuất", d.lyDo().label());
        info(pnlInfo, 1, 0, "Người nhận", d.nguoiNhan());
        info(pnlInfo, 1, 2, "Người lập", d.nguoiLap() + " · " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.trangThai() == VoucherStatus.DA_HOAN_THANH) {
            info(pnlInfo, 2, 0, "Người xuất kho", d.nguoiXuatKho() + " · " + InventoryFormat.dateTime(d.thoiDiemXuatKho()));
        } else if (d.trangThai() == VoucherStatus.DA_HUY) {
            info(pnlInfo, 2, 0, "Người hủy", d.nguoiHuy() + " · " + InventoryFormat.dateTime(d.thoiDiemHuy()));
            info(pnlInfo, 2, 2, "Lý do hủy", d.lyDoHuy());
        }
        info(pnlInfo, 3, 0, "Ghi chú", d.ghiChu() == null ? "—" : d.ghiChu());

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(pnlTitle, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblLines = new JTable(new LineModel(d.dong()));
        tblLines.setFocusable(false);
        tblLines.setRowSelectionAllowed(false);
        styleTable(tblLines, 40, new int[] {44, 84, 250, 84, 112, 80, 60, 64, 110, 124}, Set.of(0, 5, 6, 8, 9));
        JScrollPane scrLines = tableScroll(tblLines);
        pnlBody.add(scrLines, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout(0, 10));
        pnlFooter.setOpaque(false);
        JLabel lblTotal = new JLabel(d.trangThai() == VoucherStatus.DA_HOAN_THANH
                ? "Tổng giá trị xuất (giá vốn): " + InventoryFormat.money(d.tongTien())
                : "Giá vốn được chốt theo giá nhập của từng lô khi bấm \"Xuất kho\"");
        lblTotal.setFont(font(Font.BOLD, 16));
        lblTotal.setForeground(TEXT);
        lblTotal.setHorizontalAlignment(JLabel.RIGHT);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        JButton btnClose = createButton("Đóng", ButtonKind.OUTLINE);
        btnClose.addActionListener(e -> dispose());
        JButton btnPrint = createButton("In phiếu", ButtonKind.OUTLINE);
        btnPrint.addActionListener(e -> StockOutPrinter.print(this, detail));
        pnlButtons.add(btnClose);
        if (d.trangThai() != VoucherStatus.DA_HUY) {
            pnlButtons.add(btnPrint);
        }
        if (d.trangThai() == VoucherStatus.CHO_XU_LY) {
            JButton btnCancelReceipt = createButton("Hủy phiếu", ButtonKind.OUTLINE);
            btnCancelReceipt.setForeground(RED);
            btnCancelReceipt.addActionListener(e -> cancelReceipt());
            JButton btnEdit = createButton("Sửa phiếu", ButtonKind.OUTLINE);
            btnEdit.addActionListener(e -> {
                changed = true;
                dispose();
                openEditor.accept(detail.maPhieu());
            });
            JButton btnReceive = createButton("Xuất kho", ButtonKind.PRIMARY);
            btnReceive.addActionListener(e -> issue());
            pnlButtons.add(btnCancelReceipt);
            pnlButtons.add(btnEdit);
            pnlButtons.add(btnReceive);
        } else if (d.trangThai() == VoucherStatus.DA_HOAN_THANH) {
            JButton btnNote = createButton("Sửa ghi chú", ButtonKind.OUTLINE);
            btnNote.addActionListener(e -> editNote());
            pnlButtons.add(btnNote);
        }
        JPanel pnlActions = new JPanel(new BorderLayout(12, 0));
        pnlActions.setOpaque(false);
        pnlActions.add(lblError, BorderLayout.CENTER);
        pnlActions.add(pnlButtons, BorderLayout.EAST);
        pnlFooter.add(lblTotal, BorderLayout.NORTH);
        pnlFooter.add(pnlActions, BorderLayout.SOUTH);
        pnlBody.add(pnlFooter, BorderLayout.SOUTH);

        pnlBody.revalidate();
        pnlBody.repaint();
    }

    private static void info(JPanel target, int row, int column, String label, String value) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = column;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, column == 0 ? 0 : 24, 3, 10);
        target.add(fieldLabel(label), gc);
        gc.gridx = column + 1;
        gc.insets = new Insets(3, 0, 3, 0);
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(font(Font.PLAIN, 14));
        lblValue.setForeground(TEXT);
        target.add(lblValue, gc);
    }

    private void issue() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Xác nhận xuất kho phiếu " + detail.maPhieu() + "?\nSố lượng sẽ bị trừ khỏi tồn kho của từng lô.",
                "Xuất kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            InventoryActor actor = session.get();
            run(() -> service.complete(actor, detail.maPhieu()));
        }
    }

    private void cancelReceipt() {
        String reason = JOptionPane.showInputDialog(this, "Lý do hủy phiếu " + detail.maPhieu() + " (bắt buộc):",
                "Hủy phiếu xuất", JOptionPane.WARNING_MESSAGE);
        if (reason != null) {
            InventoryActor actor = session.get();
            run(() -> service.cancel(actor, detail.maPhieu(), reason));
        }
    }

    private void editNote() {
        Object input = JOptionPane.showInputDialog(this, "Ghi chú (tồn kho không thay đổi):", "Sửa ghi chú",
                JOptionPane.PLAIN_MESSAGE, null, null, detail.ghiChu() == null ? "" : detail.ghiChu());
        if (input != null) {
            InventoryActor actor = session.get();
            run(() -> service.updateNote(actor, detail.maPhieu(), input.toString()));
        }
    }

    private void run(Supplier<StockOutDetailResponse> call) {
        lblError.setText(" ");
        new SwingWorker<StockOutDetailResponse, Void>() {
            @Override
            protected StockOutDetailResponse doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                try {
                    StockOutDetailResponse updated = get();
                    changed = true;
                    render(updated);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private static final class LineModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<StockOutLineResponse> rows;

        LineModel(List<StockOutLineResponse> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Object getValueAt(int row, int column) {
            StockOutLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang();
                case 2 -> l.tenMatHang();
                case 3 -> l.soLo();
                case 4 -> InventoryFormat.date(l.hanSuDung());
                case 5 -> l.tonHienTai();
                case 6 -> l.soLuong();
                case 7 -> l.donViTinh();
                case 8 -> l.donGia() == null ? "—" : InventoryFormat.money(l.donGia());
                default -> l.thanhTien() == null ? "—" : InventoryFormat.money(l.thanhTien());
            };
        }
    }
}
