package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Chi tiết một hộp cấp cứu: thông tin, tình trạng từng mặt hàng và các thao tác kiểm tra / bổ sung / thay thế / sửa. */
final class EmergencyKitDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"#", "Mặt hàng", "ĐVT", "Tối thiểu", "Hiện có", "Hạn dùng", "Đánh giá",
            "Kho dùng được"};
    private static final int COL_RESULT = 6;

    private final transient EmergencyKitService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openLog;
    private final transient List<String> locations;
    private final String maHop;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JLabel lblMessage = new JLabel(" ");

    EmergencyKitDetailDialog(Window owner, EmergencyKitService service, InventoryItemService itemService,
                             Supplier<InventoryActor> session, String maHop, List<String> locations,
                             Consumer<String> openLog) {
        super(owner, "Hộp cấp cứu " + maHop, ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        this.openLog = Objects.requireNonNull(openLog);
        this.locations = List.copyOf(locations);
        this.maHop = maHop;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        pnlBody.add(new JLabel("Đang tải…"), BorderLayout.CENTER);
        setContentPane(pnlBody);
        setSize(1080, 640);
        setLocationRelativeTo(owner);
        load();
    }

    boolean changed() {
        return changed;
    }

    private void load() {
        Set<com.quanlynhakhoa.features.auth.enums.Role> roles = session.get().roles();
        new SwingWorker<EmergencyKitDetailResponse, Void>() {
            @Override
            protected EmergencyKitDetailResponse doInBackground() {
                return service.getDetail(roles, maHop);
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    pnlBody.removeAll();
                    lblMessage.setForeground(RED);
                    lblMessage.setText(InventoryItemPanel.describe(e.getCause()));
                    pnlBody.add(lblMessage, BorderLayout.CENTER);
                    pnlBody.revalidate();
                }
            }
        }.execute();
    }

    private void render(EmergencyKitDetailResponse d) {
        pnlBody.removeAll();
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel(d.maHop() + " — " + d.tenHop());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        Color[] c = EmergencyKitPanel.statusColors(d.trangThai());
        lblStatus.apply(d.trangThai().label(), c[0], c[1], c[2]);
        lblStatus.setToolTipText(EmergencyKitPanel.statusTooltip(d.trangThai()));
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Loại hộp", d.loai().label());
        info(pnlInfo, 0, 2, "Vị trí", d.viTri());
        info(pnlInfo, 1, 0, "Kiểm tra gần nhất", d.kiemTraGanNhat() == null ? "Chưa kiểm tra"
                : InventoryFormat.dateTime(d.kiemTraGanNhat()) + " · " + d.nguoiKiemTraGanNhat());
        JLabel lblDue = info(pnlInfo, 1, 2, "Kiểm tra lại trước", InventoryFormat.date(d.hanKiemTraTiep())
                + (d.denHanKiemTra() ? " (đã đến hạn)" : ""));
        if (d.denHanKiemTra()) {
            lblDue.setForeground(RED);
        }
        info(pnlInfo, 2, 0, "Chu kỳ / ngưỡng cận hạn", d.chuKyKiemTraNgay() + " ngày / " + d.nguongHanDungNgay()
                + " ngày");
        info(pnlInfo, 2, 2, "Người tạo", d.nguoiTao() + " · " + InventoryFormat.dateTime(d.thoiDiemTao()));
        if (d.ghiChu() != null) {
            info(pnlInfo, 3, 0, "Ghi chú", d.ghiChu());
        }
        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(pnlTitle, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblItems = new JTable(new ItemModel(d.matHang()));
        tblItems.setFocusable(false);
        tblItems.setRowSelectionAllowed(false);
        styleTable(tblItems, 38, new int[] {40, 340, 70, 90, 90, 130, 110, 130}, Set.of(0, 3, 4, 7));
        tblItems.getColumnModel().getColumn(COL_RESULT).setCellRenderer(new EmergencyKitCheckDialog.ResultRenderer());
        JLabel lblItems = new JLabel("MẶT HÀNG TRONG HỘP — \"Kho dùng được\" là tồn còn hạn sau ngưỡng, lấy được khi bổ "
                + "sung / thay thế");
        lblItems.setFont(font(Font.PLAIN, 13));
        lblItems.setForeground(TEXT_MUTED);
        JPanel pnlItems = new JPanel(new BorderLayout(0, 6));
        pnlItems.setOpaque(false);
        pnlItems.add(lblItems, BorderLayout.NORTH);
        pnlItems.add(tableScroll(tblItems), BorderLayout.CENTER);
        pnlBody.add(pnlItems, BorderLayout.CENTER);

        lblMessage.setFont(font(Font.PLAIN, 13));
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        JButton btnLog = createButton("Nhật ký", ButtonKind.OUTLINE);
        btnLog.addActionListener(e -> {
            dispose();
            openLog.accept(maHop);
        });
        JButton btnEdit = createButton("Sửa", ButtonKind.OUTLINE);
        btnEdit.addActionListener(e -> {
            EmergencyKitEditorDialog dlg = new EmergencyKitEditorDialog(this, service, itemService, session, maHop,
                    locations);
            dlg.setVisible(true);
            dlg.saved().ifPresent(saved -> afterChange("Đã lưu thông tin hộp."));
        });
        JButton btnCheck = createButton("Kiểm tra", ButtonKind.OUTLINE);
        btnCheck.addActionListener(e -> {
            if (EmergencyKitCheckDialog.open(this, service, session, d)) {
                afterChange("Đã lưu kết quả kiểm tra.");
            }
        });
        JButton btnRestock = createButton("Bổ sung", ButtonKind.OUTLINE);
        btnRestock.addActionListener(e -> {
            if (EmergencyKitRestockDialog.open(this, service, session, d, KitLogType.BO_SUNG)) {
                afterChange("Đã bổ sung từ kho.");
            }
        });
        JButton btnReplace = createButton("Thay thế", ButtonKind.OUTLINE);
        btnReplace.addActionListener(e -> {
            if (EmergencyKitRestockDialog.open(this, service, session, d, KitLogType.THAY_THE)) {
                afterChange("Đã thay thế hàng cận/hết hạn.");
            }
        });
        JButton btnClose = createButton("Đóng", ButtonKind.PRIMARY);
        btnClose.addActionListener(e -> dispose());
        for (JButton b : new JButton[] {btnLog, btnEdit, btnCheck, btnRestock, btnReplace, btnClose}) {
            pnlButtons.add(b);
        }
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblMessage, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlBody.add(pnlFooter, BorderLayout.SOUTH);
        pnlBody.revalidate();
        pnlBody.repaint();
    }

    private void afterChange(String message) {
        changed = true;
        load();
        javax.swing.SwingUtilities.invokeLater(() -> {
            lblMessage.setForeground(GREEN);
            lblMessage.setText(message);
        });
    }

    private static JLabel info(JPanel target, int row, int column, String label, String value) {
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
        return lblValue;
    }

    private static final class ItemModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<EmergencyKitItemResponse> rows;

        ItemModel(List<EmergencyKitItemResponse> rows) {
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
            EmergencyKitItemResponse i = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> i.maMatHang() + " — " + i.tenMatHang() + (i.kiemSoatDacBiet() ? " (KSĐB)" : "");
                case 2 -> i.donViTinh();
                case 3 -> i.soLuongToiThieu();
                case 4 -> i.soLuongHienCo();
                case 5 -> InventoryFormat.date(i.hanDung());
                case COL_RESULT -> i.danhGia();
                default -> i.tonKhoDung();
            };
        }
    }
}
