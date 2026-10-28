package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitLogLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitLogResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitRowResponse;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Nhật ký kiểm tra / bổ sung / thay thế hộp cấp cứu (mới nhất trước), xem chi tiết từng mặt hàng và xuất Excel. */
final class EmergencyKitLogDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "Tất cả hộp";
    private static final String[] LOG_COLUMNS = {"Thời điểm", "Mã hộp", "Tên hộp", "Hoạt động", "Kết quả",
            "Người thực hiện", "Phiếu xuất", "Ghi chú"};
    private static final String[] LINE_COLUMNS = {"Mặt hàng", "ĐVT", "Trước", "Sau", "Hạn dùng", "Đánh giá"};
    private static final int COL_RESULT = 4;

    private final transient EmergencyKitService service;
    private final transient Supplier<InventoryActor> session;
    private final JComboBox<String> cmbKit = new JComboBox<>();
    private final LogModel logModel = new LogModel();
    private final JTable tblLogs = new JTable(logModel);
    private final LineModel lineModel = new LineModel();
    private final JTable tblLines = new JTable(lineModel);
    private final JLabel lblInfo = new JLabel(" ");
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private transient List<EmergencyKitLogResponse> logs = List.of();

    EmergencyKitLogDialog(Window owner, EmergencyKitService service, Supplier<InventoryActor> session,
                          List<EmergencyKitRowResponse> kits, String maHop) {
        super(owner, "Nhật ký kiểm tra hộp cấp cứu", ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        cmbKit.addItem(ALL);
        kits.forEach(k -> cmbKit.addItem(k.maHop() + " — " + k.tenHop()));
        if (maHop != null) {
            for (int i = 0; i < cmbKit.getItemCount(); i++) {
                if (cmbKit.getItemAt(i).startsWith(maHop + " ")) {
                    cmbKit.setSelectedIndex(i);
                }
            }
        }
        setContentPane(buildContent());
        cmbKit.addActionListener(e -> load());
        setSize(1300, 720);
        setLocationRelativeTo(owner);
        load();
    }

    private JPanel buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 12));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        styleCombo(cmbKit);
        cmbKit.setPreferredSize(new Dimension(380, 38));
        cmbKit.setFocusable(false);
        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 16, TEXT));
        btnExport.addActionListener(e -> export());
        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlFilter.setOpaque(false);
        pnlFilter.add(fieldLabel("Hộp"));
        pnlFilter.add(cmbKit);
        pnlFilter.add(btnExport);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 10));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(pnlFilter, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        styleTable(tblLogs, 36, new int[] {150, 100, 250, 105, 115, 170, 105, 225}, Set.of());
        tblLogs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLogs.getColumnModel().getColumn(COL_RESULT).setCellRenderer(new OutcomeRenderer());
        tblLogs.getSelectionModel().addListSelectionListener(e -> {
            int row = tblLogs.getSelectedRow();
            lineModel.setRows(row < 0 ? List.of() : logs.get(row).dong());
        });
        styleTable(tblLines, 34, new int[] {380, 70, 80, 80, 130, 110}, Set.of(2, 3));
        tblLines.setFocusable(false);
        tblLines.setRowSelectionAllowed(false);
        tblLines.getColumnModel().getColumn(5).setCellRenderer(new EmergencyKitCheckDialog.ResultRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, selected, focused, row, column);
                if (value == null) {
                    ((JLabel) c).setText("—");
                }
                return c;
            }
        });
        JLabel lblLines = new JLabel("CHI TIẾT MẶT HÀNG CỦA DÒNG ĐANG CHỌN");
        lblLines.setFont(font(Font.PLAIN, 13));
        lblLines.setForeground(TEXT_MUTED);
        JPanel pnlLines = new JPanel(new BorderLayout(0, 6));
        pnlLines.setOpaque(false);
        pnlLines.add(lblLines, BorderLayout.NORTH);
        pnlLines.add(tableScroll(tblLines), BorderLayout.CENTER);
        JSplitPane splContent = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll(tblLogs), pnlLines);
        splContent.setResizeWeight(0.6);
        splContent.setBorder(BorderFactory.createEmptyBorder());
        splContent.setOpaque(false);
        pnlRoot.add(splContent, BorderLayout.CENTER);

        lblInfo.setFont(font(Font.PLAIN, 13));
        JButton btnClose = createButton("Đóng", ButtonKind.PRIMARY);
        btnClose.addActionListener(e -> dispose());
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblInfo, BorderLayout.CENTER);
        pnlFooter.add(btnClose, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private String selectedKit() {
        String selected = (String) cmbKit.getSelectedItem();
        return selected == null || ALL.equals(selected) ? null : selected.substring(0, selected.indexOf(' '));
    }

    private void load() {
        Set<Role> roles = session.get().roles();
        String kit = selectedKit();
        lblInfo.setForeground(TEXT_MUTED);
        lblInfo.setText("Đang tải nhật ký…");
        new SwingWorker<List<EmergencyKitLogResponse>, Void>() {
            @Override
            protected List<EmergencyKitLogResponse> doInBackground() {
                return service.logs(roles, kit);
            }

            @Override
            protected void done() {
                try {
                    logs = get();
                    logModel.setRows(logs);
                    lineModel.setRows(List.of());
                    if (!logs.isEmpty()) {
                        tblLogs.setRowSelectionInterval(0, 0);
                    }
                    lblInfo.setForeground(TEXT);
                    lblInfo.setText(logs.isEmpty() ? "Chưa có lần kiểm tra / bổ sung nào."
                            : logs.size() + " lần ghi nhận · chọn một dòng để xem từng mặt hàng.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblInfo.setForeground(RED);
                    lblInfo.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    static String outcome(EmergencyKitLogResponse g) {
        if (g.loai() != KitLogType.KIEM_TRA) {
            return "Đã xuất kho";
        }
        return Boolean.TRUE.equals(g.dat()) ? "Đạt" : "Không đạt";
    }

    private void export() {
        if (logs.isEmpty()) {
            lblInfo.setForeground(TEXT_MUTED);
            lblInfo.setText("Không có dữ liệu để xuất.");
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất nhật ký hộp cấp cứu");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("nhat-ky-hop-cap-cuu.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<EmergencyKitLogResponse> snapshot = logs;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                writeExcel(target, snapshot);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblInfo.setForeground(GREEN);
                    lblInfo.setText("Đã xuất " + snapshot.size() + " lần ghi nhận ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblInfo.setForeground(RED);
                    lblInfo.setText("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    /** Mỗi mặt hàng của mỗi lần ghi nhận là một dòng. */
    static void writeExcel(File target, List<EmergencyKitLogResponse> logs) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("Thời điểm", 17, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã hộp", 10, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tên hộp", 28, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Hoạt động", 12, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Kết quả", 12, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Người thực hiện", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Phiếu xuất", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mặt hàng", 34, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("ĐVT", 8, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Trước", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Sau", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Hạn dùng", 12, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Đánh giá", 11, XlsxExport.Kind.CENTER));
        List<List<Object>> rows = new ArrayList<>();
        for (EmergencyKitLogResponse g : logs) {
            for (EmergencyKitLogLineResponse l : g.dong()) {
                List<Object> row = new ArrayList<>();
                row.add(InventoryFormat.dateTime(g.thoiDiem()));
                row.add(g.maHop());
                row.add(g.tenHop());
                row.add(g.loai().label());
                row.add(outcome(g));
                row.add(g.nguoiThucHien());
                row.add(g.maPhieuXuat());
                row.add(l.maMatHang() + " — " + l.tenMatHang());
                row.add(l.donViTinh());
                row.add(l.soLuongTruoc());
                row.add(l.soLuongSau());
                row.add(l.hanDungSau());
                row.add(l.ketQua() == null ? null : l.ketQua().label());
                rows.add(row);
            }
        }
        XlsxExport.write(target, "Nhat ky hop cap cuu", "NHẬT KÝ KIỂM TRA HỘP THUỐC CẤP CỨU",
                "Xuất lúc " + InventoryFormat.dateTime(java.time.LocalDateTime.now()) + " · " + logs.size()
                        + " lần ghi nhận", columns, rows);
    }

    /** Kết quả: Đạt xanh, Không đạt đỏ, bổ sung/thay thế xám. */
    private static final class OutcomeRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            lbl.setFont(font(Font.BOLD, 14));
            lbl.setForeground("Đạt".equals(value) ? GREEN : "Không đạt".equals(value) ? RED : TEXT_MUTED);
            return lbl;
        }
    }

    private static final class LogModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<EmergencyKitLogResponse> rows = List.of();

        void setRows(List<EmergencyKitLogResponse> data) {
            rows = List.copyOf(data);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return LOG_COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return LOG_COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            EmergencyKitLogResponse g = rows.get(row);
            return switch (column) {
                case 0 -> InventoryFormat.dateTime(g.thoiDiem());
                case 1 -> g.maHop();
                case 2 -> g.tenHop();
                case 3 -> g.loai().label();
                case COL_RESULT -> outcome(g);
                case 5 -> g.nguoiThucHien();
                case 6 -> g.maPhieuXuat() == null ? "—" : g.maPhieuXuat();
                default -> g.ghiChu() == null ? "" : g.ghiChu();
            };
        }
    }

    private static final class LineModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<EmergencyKitLogLineResponse> rows = List.of();

        void setRows(List<EmergencyKitLogLineResponse> data) {
            rows = List.copyOf(data);
            fireTableDataChanged();
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
            EmergencyKitLogLineResponse l = rows.get(row);
            return switch (column) {
                case 0 -> l.maMatHang() + " — " + l.tenMatHang();
                case 1 -> l.donViTinh();
                case 2 -> l.soLuongTruoc();
                case 3 -> l.soLuongSau();
                case 4 -> InventoryFormat.date(l.hanDungSau());
                default -> l.ketQua();
            };
        }
    }
}
