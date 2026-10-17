package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeLineResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Xem chi tiết đợt kiểm kê: sổ sách, thực tế, chênh lệch từng lô; in biên bản, cập nhật hoặc hủy đợt. */
final class StocktakeDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS =
            {"#", "Mã", "Mặt hàng", "Số lô", "HSD", "Sổ sách", "Thực tế", "Chênh lệch", "Lý do"};

    private final transient StocktakeService service;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openCount;
    private transient StocktakeDetailResponse detail;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JLabel lblError = new JLabel(" ");

    /** @param openCount mở hộp thoại "Cập nhật" số đếm (do màn danh sách cung cấp) */
    StocktakeDetailDialog(Window owner, StocktakeService service, Supplier<InventoryActor> session,
                          StocktakeDetailResponse detail, Consumer<String> openCount) {
        super(owner, "Đợt kiểm kê " + detail.maKiemKe(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.openCount = Objects.requireNonNull(openCount);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(pnlBody);
        render(detail);
        setSize(1120, 640);
        setLocationRelativeTo(owner);
    }

    boolean changed() {
        return changed;
    }

    private void render(StocktakeDetailResponse d) {
        this.detail = d;
        pnlBody.removeAll();

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Đợt kiểm kê " + d.maKiemKe());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        lblStatus.apply(StocktakePanel.statusLabel(d.trangThai()), InventoryFormat.statusDot(d.trangThai()),
                InventoryFormat.statusFill(d.trangThai()), InventoryFormat.statusText(d.trangThai()));
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Ngày kiểm kê", InventoryFormat.date(d.ngayKiemKe()));
        info(pnlInfo, 0, 2, "Phạm vi", d.phamVi().label());
        info(pnlInfo, 1, 0, "Người lập", d.nguoiLap() + " · " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.trangThai() == VoucherStatus.DA_HOAN_THANH) {
            info(pnlInfo, 1, 2, "Người duyệt", d.nguoiDuyet() + " · " + InventoryFormat.dateTime(d.thoiDiemDuyet()));
        } else if (d.trangThai() == VoucherStatus.DA_HUY) {
            info(pnlInfo, 1, 2, "Người hủy", d.nguoiHuy() + " · " + InventoryFormat.dateTime(d.thoiDiemHuy()));
            info(pnlInfo, 2, 2, "Lý do hủy", d.lyDoHuy());
        }
        info(pnlInfo, 2, 0, "Ghi chú", d.ghiChu() == null ? "—" : d.ghiChu());

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(pnlTitle, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblLines = new JTable(new LineModel(d.dong()));
        tblLines.setFocusable(false);
        tblLines.setRowSelectionAllowed(false);
        styleTable(tblLines, 40, new int[] {40, 70, 240, 85, 110, 90, 80, 115, 220}, Set.of(0, 5, 6, 7));
        pnlBody.add(tableScroll(tblLines), BorderLayout.CENTER);

        JLabel lblTotal = new JLabel(d.tongMatHang() + " mặt hàng · " + d.dong().size() + " lô · đã đếm " + d.daDem()
                + " · " + (d.trangThai() == VoucherStatus.DA_HUY ? "đã hủy, tồn không đổi"
                : StocktakePanel.differenceText(d.thieu(), d.thua())));
        lblTotal.setFont(font(Font.BOLD, 16));
        lblTotal.setForeground(TEXT);
        lblTotal.setHorizontalAlignment(JLabel.RIGHT);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        JButton btnClose = createButton("Đóng", ButtonKind.OUTLINE);
        btnClose.addActionListener(e -> dispose());
        pnlButtons.add(btnClose);
        if (d.trangThai() != VoucherStatus.DA_HUY) {
            JButton btnPrint = createButton("In biên bản", ButtonKind.OUTLINE);
            btnPrint.addActionListener(e -> StocktakePrinter.print(this, detail));
            pnlButtons.add(btnPrint);
        }
        if (d.trangThai() == VoucherStatus.CHO_XU_LY) {
            JButton btnCancel = createButton("Hủy đợt", ButtonKind.OUTLINE);
            btnCancel.setForeground(RED);
            btnCancel.addActionListener(e -> cancelStocktake());
            JButton btnCount = createButton("Cập nhật số đếm", ButtonKind.PRIMARY);
            btnCount.addActionListener(e -> {
                changed = true;
                dispose();
                openCount.accept(detail.maKiemKe());
            });
            pnlButtons.add(btnCancel);
            pnlButtons.add(btnCount);
        }
        JPanel pnlActions = new JPanel(new BorderLayout(12, 0));
        pnlActions.setOpaque(false);
        pnlActions.add(lblError, BorderLayout.CENTER);
        pnlActions.add(pnlButtons, BorderLayout.EAST);
        JPanel pnlFooter = new JPanel(new BorderLayout(0, 10));
        pnlFooter.setOpaque(false);
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

    private void cancelStocktake() {
        String reason = JOptionPane.showInputDialog(this, "Lý do hủy đợt " + detail.maKiemKe()
                + " (bắt buộc; tồn kho không thay đổi):", "Hủy đợt kiểm kê", JOptionPane.WARNING_MESSAGE);
        if (reason == null) {
            return;
        }
        InventoryActor actor = session.get();
        String ma = detail.maKiemKe();
        lblError.setText(" ");
        new SwingWorker<StocktakeDetailResponse, Void>() {
            @Override
            protected StocktakeDetailResponse doInBackground() {
                return service.cancel(actor, ma, reason);
            }

            @Override
            protected void done() {
                try {
                    StocktakeDetailResponse updated = get();
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
        private final transient List<StocktakeLineResponse> rows;

        LineModel(List<StocktakeLineResponse> rows) {
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
            StocktakeLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang();
                case 2 -> l.tenMatHang();
                case 3 -> l.soLo();
                case 4 -> InventoryFormat.date(l.hanSuDung());
                case 5 -> l.soSach();
                case 6 -> l.thucTe() == null ? "—" : l.thucTe();
                case 7 -> l.chenhLech() == null ? "—" : l.chenhLech() > 0 ? "+" + l.chenhLech() : l.chenhLech();
                default -> l.ghiChu() == null ? "" : l.ghiChu();
            };
        }
    }
}
