package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLineResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.DisposalService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
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

/** Xem chi tiết phiếu hủy; chờ duyệt: Hủy phiếu / Sửa phiếu / Duyệt (người khác người lập); đã duyệt: In. */
final class DisposalDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS =
            {"#", "Mã", "Mặt hàng", "Số lô", "HSD", "SL hủy", "ĐVT", "Đơn giá", "Thành tiền"};

    private final transient DisposalService service;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openEditor;
    private transient DisposalDetailResponse detail;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JLabel lblError = new JLabel(" ");

    DisposalDetailDialog(Window owner, DisposalService service, Supplier<InventoryActor> session,
                         DisposalDetailResponse detail, Consumer<String> openEditor) {
        super(owner, "Phiếu hủy " + detail.maPhieu(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.openEditor = Objects.requireNonNull(openEditor);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        setContentPane(pnlBody);
        render(detail);
        setSize(1060, 620);
        setLocationRelativeTo(owner);
    }

    boolean changed() {
        return changed;
    }

    private void render(DisposalDetailResponse d) {
        this.detail = d;
        pnlBody.removeAll();
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Phiếu hủy " + d.maPhieu());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        lblStatus.apply(DisposalPanel.statusLabel(d.trangThai()), InventoryFormat.statusDot(d.trangThai()),
                InventoryFormat.statusFill(d.trangThai()), InventoryFormat.statusText(d.trangThai()));
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Ngày hủy", InventoryFormat.date(d.ngayHuy()));
        info(pnlInfo, 0, 2, "Lý do", d.lyDo().label());
        info(pnlInfo, 1, 0, "Mô tả", d.moTa() == null ? "—" : d.moTa());
        info(pnlInfo, 1, 2, "Người yêu cầu", d.nguoiYeuCau() + " · " + InventoryFormat.dateTime(d.thoiDiemLap()));
        if (d.trangThai() == VoucherStatus.DA_HOAN_THANH) {
            info(pnlInfo, 2, 0, "Người duyệt", d.nguoiDuyet() + " · " + InventoryFormat.dateTime(d.thoiDiemDuyet()));
        } else if (d.trangThai() == VoucherStatus.DA_HUY) {
            info(pnlInfo, 2, 0, "Người hủy phiếu", d.nguoiHuy() + " · " + InventoryFormat.dateTime(d.thoiDiemHuy()));
            info(pnlInfo, 2, 2, "Lý do hủy phiếu", d.lyDoHuy());
        }
        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(pnlTitle, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblLines = new JTable(new LineModel(d.dong()));
        tblLines.setFocusable(false);
        tblLines.setRowSelectionAllowed(false);
        styleTable(tblLines, 40, new int[] {40, 75, 260, 90, 110, 80, 70, 115, 130}, Set.of(0, 5, 7, 8));
        pnlBody.add(tableScroll(tblLines), BorderLayout.CENTER);

        JLabel lblTotal = new JLabel("Giá trị hủy (giá nhập của lô): " + InventoryFormat.money(d.giaTri()));
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
            JButton btnPrint = createButton("In phiếu", ButtonKind.OUTLINE);
            btnPrint.addActionListener(e -> DisposalPrinter.print(this, detail));
            pnlButtons.add(btnPrint);
        }
        if (d.trangThai() == VoucherStatus.CHO_XU_LY) {
            JButton btnCancel = createButton("Hủy phiếu", ButtonKind.OUTLINE);
            btnCancel.setForeground(RED);
            btnCancel.addActionListener(e -> cancelDisposal());
            JButton btnEdit = createButton("Sửa phiếu", ButtonKind.OUTLINE);
            btnEdit.addActionListener(e -> {
                changed = true;
                dispose();
                openEditor.accept(detail.maPhieu());
            });
            pnlButtons.add(btnCancel);
            pnlButtons.add(btnEdit);
            if (Objects.equals(session.get().maNhanVien(), d.maNhanVienLap())) {
                lblError.setForeground(TEXT_MUTED);
                lblError.setText("Người lập không tự duyệt — cần một quản lý kho khác duyệt phiếu này.");
            } else {
                JButton btnApprove = createButton("Duyệt", ButtonKind.PRIMARY);
                btnApprove.addActionListener(e -> approve());
                pnlButtons.add(btnApprove);
            }
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

    private void approve() {
        int answer = JOptionPane.showConfirmDialog(this, "Duyệt phiếu hủy " + detail.maPhieu() + "?\nTồn kho của "
                + detail.dong().size() + " lô sẽ bị trừ ngay, không thể hoàn tác.", "Duyệt phiếu hủy",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            InventoryActor actor = session.get();
            String ma = detail.maPhieu();
            run(() -> service.approve(actor, ma));
        }
    }

    private void cancelDisposal() {
        String reason = JOptionPane.showInputDialog(this, "Lý do hủy phiếu " + detail.maPhieu()
                + " (bắt buộc; tồn kho không thay đổi):", "Hủy phiếu", JOptionPane.WARNING_MESSAGE);
        if (reason != null) {
            InventoryActor actor = session.get();
            String ma = detail.maPhieu();
            run(() -> service.cancel(actor, ma, reason));
        }
    }

    private void run(Supplier<DisposalDetailResponse> call) {
        lblError.setForeground(RED);
        lblError.setText(" ");
        new SwingWorker<DisposalDetailResponse, Void>() {
            @Override
            protected DisposalDetailResponse doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                try {
                    DisposalDetailResponse updated = get();
                    changed = true;
                    render(updated);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setForeground(RED);
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private static final class LineModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<DisposalLineResponse> rows;

        LineModel(List<DisposalLineResponse> rows) {
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
            DisposalLineResponse l = rows.get(row);
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
