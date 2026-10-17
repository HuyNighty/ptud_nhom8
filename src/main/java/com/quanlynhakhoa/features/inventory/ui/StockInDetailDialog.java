package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInLineResponse;
import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockInService;
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

/** Xem chi tiết phiếu nhập; các nút thao tác hiện theo trạng thái phiếu. */
final class StockInDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS =
            {"#", "Mã", "Mặt hàng", "Số lô", "Hạn sử dụng", "SL", "ĐVT", "Đơn giá", "Thành tiền"};

    private final transient StockInService service;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openEditor;
    private transient StockInDetailResponse detail;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JLabel lblError = new JLabel(" ");

    /** @param openEditor mở hộp thoại sửa phiếu (do màn danh sách cung cấp) */
    StockInDetailDialog(Window owner, StockInService service, Supplier<InventoryActor> session,
                        StockInDetailResponse detail, Consumer<String> openEditor) {
        super(owner, "Phiếu nhập " + detail.maPhieu(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.openEditor = Objects.requireNonNull(openEditor);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(pnlBody);
        render(detail);
        setSize(980, 620);
        setLocationRelativeTo(owner);
    }

    /** Có thay đổi dữ liệu (nhập kho / hủy / sửa) → màn danh sách cần tải lại. */
    boolean changed() {
        return changed;
    }

    private void render(StockInDetailResponse d) {
        this.detail = d;
        pnlBody.removeAll();

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Phiếu nhập " + d.maPhieu());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        lblStatus.apply(d.trangThai().label(), InventoryFormat.statusDot(d.trangThai()),
                InventoryFormat.statusFill(d.trangThai()), InventoryFormat.statusText(d.trangThai()));
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Ngày nhập", InventoryFormat.date(d.ngayNhap()));
        info(pnlInfo, 0, 2, "Nhà cung cấp", d.tenNhaCungCap());
        info(pnlInfo, 1, 0, "Người lập", d.nguoiLap() + " · " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.trangThai() == StockInStatus.DA_HOAN_THANH) {
            info(pnlInfo, 1, 2, "Người nhập kho", d.nguoiNhapKho() + " · " + InventoryFormat.dateTime(d.thoiDiemNhapKho()));
        } else if (d.trangThai() == StockInStatus.DA_HUY) {
            info(pnlInfo, 1, 2, "Người hủy", d.nguoiHuy() + " · " + InventoryFormat.dateTime(d.thoiDiemHuy()));
            info(pnlInfo, 2, 0, "Lý do hủy", d.lyDoHuy());
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
        styleTable(tblLines, 40, new int[] {44, 80, 200, 80, 135, 55, 65, 115, 135}, Set.of(0, 5, 7, 8));
        JScrollPane scrLines = tableScroll(tblLines);
        pnlBody.add(scrLines, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout(0, 10));
        pnlFooter.setOpaque(false);
        JLabel lblTotal = new JLabel("Tổng tiền: " + InventoryFormat.money(d.tongTien()));
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
        btnPrint.addActionListener(e -> StockInPrinter.print(this, detail));
        pnlButtons.add(btnClose);
        if (d.trangThai() != StockInStatus.DA_HUY) {
            pnlButtons.add(btnPrint);
        }
        if (d.trangThai() == StockInStatus.CHO_XU_LY) {
            JButton btnCancelReceipt = createButton("Hủy phiếu", ButtonKind.OUTLINE);
            btnCancelReceipt.setForeground(RED);
            btnCancelReceipt.addActionListener(e -> cancelReceipt());
            JButton btnEdit = createButton("Sửa phiếu", ButtonKind.OUTLINE);
            btnEdit.addActionListener(e -> {
                changed = true;
                dispose();
                openEditor.accept(detail.maPhieu());
            });
            JButton btnReceive = createButton("Nhập kho", ButtonKind.PRIMARY);
            btnReceive.addActionListener(e -> receive());
            pnlButtons.add(btnCancelReceipt);
            pnlButtons.add(btnEdit);
            pnlButtons.add(btnReceive);
        } else if (d.trangThai() == StockInStatus.DA_HOAN_THANH) {
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

    private void receive() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Xác nhận nhập kho phiếu " + detail.maPhieu() + "?\nSố lượng sẽ được cộng vào tồn kho theo từng lô.",
                "Nhập kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            InventoryActor actor = session.get();
            run(() -> service.complete(actor, detail.maPhieu()));
        }
    }

    private void cancelReceipt() {
        String reason = JOptionPane.showInputDialog(this, "Lý do hủy phiếu " + detail.maPhieu() + " (bắt buộc):",
                "Hủy phiếu nhập", JOptionPane.WARNING_MESSAGE);
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

    private void run(Supplier<StockInDetailResponse> call) {
        lblError.setText(" ");
        new SwingWorker<StockInDetailResponse, Void>() {
            @Override
            protected StockInDetailResponse doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                try {
                    StockInDetailResponse updated = get();
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
        private final transient List<StockInLineResponse> rows;

        LineModel(List<StockInLineResponse> rows) {
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
            StockInLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang();
                case 2 -> l.tenMatHang();
                case 3 -> l.soLo();
                case 4 -> InventoryFormat.date(l.hanSuDung());
                case 5 -> l.soLuong();
                case 6 -> l.donViTinh();
                case 7 -> InventoryFormat.money(l.donGia());
                default -> InventoryFormat.money(l.thanhTien());
            };
        }
    }
}
