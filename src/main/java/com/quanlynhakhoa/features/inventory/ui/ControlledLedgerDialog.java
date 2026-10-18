package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledLedgerEntry;
import com.quanlynhakhoa.features.inventory.service.ControlledDrugService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.TimeRange;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

/**
 * Sổ theo dõi thuốc kiểm soát đặc biệt (nhật ký sử dụng): mọi biến động tồn của thuốc KSĐB theo thời gian,
 * có tồn sau từng dòng; lọc theo thuốc và thời gian, xuất Excel để lưu sổ.
 */
final class ControlledLedgerDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "Tất cả thuốc kiểm soát";
    private static final String[] COLUMNS = {"Thời điểm", "Chứng từ", "Loại", "Thuốc", "Số lô", "Nhập", "Xuất",
            "Tồn sau", "Người nhận / Ghi chú", "Người thực hiện"};

    private final transient ControlledDrugService service;
    private final transient Supplier<InventoryActor> session;

    private final JComboBox<Object> cmbDrug = new JComboBox<>();
    private final JComboBox<TimeRange> cmbTime = new JComboBox<>(TimeRange.values());
    private final LedgerModel model = new LedgerModel();
    private final JTable tblLedger = new JTable(model);
    private final JLabel lblInfo = new JLabel(" ");
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private transient List<ControlledLedgerEntry> all = List.of();

    ControlledLedgerDialog(Window owner, ControlledDrugService service, Supplier<InventoryActor> session,
                           List<ControlledDrugRowResponse> drugs, String maThuoc) {
        super(owner, "Sổ theo dõi thuốc kiểm soát đặc biệt", ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        cmbDrug.addItem(ALL);
        for (ControlledDrugRowResponse d : drugs) {
            cmbDrug.addItem(new DrugOption(d.maThuoc(), d.tenDayDu()));
            if (d.maThuoc().equals(maThuoc)) {
                cmbDrug.setSelectedIndex(cmbDrug.getItemCount() - 1);
            }
        }
        setContentPane(buildContent());
        cmbDrug.addActionListener(e -> applyFilter());
        cmbTime.addActionListener(e -> applyFilter());
        btnExport.addActionListener(e -> export());
        setSize(1400, 700);
        setLocationRelativeTo(owner);
        load();
    }

    private record DrugOption(String ma, String ten) {
        @Override
        public String toString() {
            return ma + " — " + ten;
        }
    }

    private JPanel buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 12));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        JLabel lblSub = new JLabel("Dựng từ phiếu nhập, xuất, hoàn trả, kiểm kê và hủy đã hoàn tất — không sửa trực tiếp.");
        lblSub.setFont(font(Font.PLAIN, 13));
        lblSub.setForeground(TEXT_MUTED);
        styleCombo(cmbDrug);
        cmbDrug.setPreferredSize(new Dimension(380, 40));
        cmbDrug.setFocusable(false);
        cmbTime.setFocusable(false);
        styleCombo(cmbTime);
        cmbTime.setPreferredSize(new Dimension(180, 40));
        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlFilters.setOpaque(false);
        pnlFilters.add(cmbDrug);
        pnlFilters.add(javax.swing.Box.createHorizontalStrut(12));
        pnlFilters.add(cmbTime);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        pnlTop.setOpaque(false);
        JPanel pnlTitles = new JPanel(new BorderLayout(0, 4));
        pnlTitles.setOpaque(false);
        pnlTitles.add(lblTitle, BorderLayout.NORTH);
        pnlTitles.add(lblSub, BorderLayout.CENTER);
        pnlTop.add(pnlTitles, BorderLayout.NORTH);
        pnlTop.add(pnlFilters, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        tblLedger.setFocusable(false);
        tblLedger.setRowSelectionAllowed(false);
        styleTable(tblLedger, 36, new int[] {150, 90, 95, 240, 80, 65, 65, 85, 280, 150}, Set.of(5, 6, 7));
        pnlRoot.add(tableScroll(tblLedger), BorderLayout.CENTER);

        lblInfo.setFont(font(Font.PLAIN, 14));
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        JButton btnClose = createButton("Đóng", ButtonKind.PRIMARY);
        btnClose.addActionListener(e -> dispose());
        pnlButtons.add(btnExport);
        pnlButtons.add(btnClose);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblInfo, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private void load() {
        lblInfo.setForeground(TEXT_MUTED);
        lblInfo.setText("Đang tải sổ theo dõi…");
        var roles = session.get().roles();
        new SwingWorker<List<ControlledLedgerEntry>, Void>() {
            @Override
            protected List<ControlledLedgerEntry> doInBackground() {
                return service.ledger(roles, null);
            }

            @Override
            protected void done() {
                try {
                    all = get();
                    applyFilter();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblInfo.setForeground(RED);
                    lblInfo.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private List<ControlledLedgerEntry> filtered() {
        String drug = cmbDrug.getSelectedItem() instanceof DrugOption o ? o.ma() : null;
        TimeRange range = (TimeRange) cmbTime.getSelectedItem();
        LocalDate today = LocalDate.now();
        List<ControlledLedgerEntry> rows = new ArrayList<>();
        for (ControlledLedgerEntry e : all) {
            boolean matchesDrug = drug == null || drug.equals(e.maThuoc());
            boolean matchesTime = range == null || e.thoiDiem() == null || range.contains(e.thoiDiem().toLocalDate(), today);
            if (matchesDrug && matchesTime) {
                rows.add(e);
            }
        }
        return rows;
    }

    private void applyFilter() {
        List<ControlledLedgerEntry> rows = filtered();
        model.setRows(rows);
        int in = rows.stream().mapToInt(ControlledLedgerEntry::nhap).sum();
        int out = rows.stream().mapToInt(ControlledLedgerEntry::xuat).sum();
        lblInfo.setForeground(TEXT);
        lblInfo.setText(rows.isEmpty() ? "Chưa có biến động nào trong bộ lọc này."
                : rows.size() + " dòng · tổng nhập " + in + " · tổng xuất " + out);
        btnExport.setEnabled(!rows.isEmpty());
    }

    private void export() {
        List<ControlledLedgerEntry> rows = filtered();
        JFileChooser fch = new JFileChooser();
        fch.setDialogTitle("Xuất sổ theo dõi thuốc kiểm soát");
        fch.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fch.setSelectedFile(new File("so-theo-doi-thuoc-ksdb.xlsx"));
        if (fch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fch.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        String scope = String.valueOf(cmbDrug.getSelectedItem());
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                writeExcel(target, rows, scope);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblInfo.setForeground(GREEN);
                    lblInfo.setText("Đã xuất " + rows.size() + " dòng ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblInfo.setForeground(RED);
                    lblInfo.setText("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<ControlledLedgerEntry> rows, String scope) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("Thời điểm", 17, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Chứng từ", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Loại", 11, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Mã thuốc", 10, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tên thuốc", 26, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Số lô", 11, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Nhập", 8, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Xuất", 8, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Tồn sau", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Người nhận / Ghi chú", 34, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người thực hiện", 20, XlsxExport.Kind.TEXT));
        List<List<Object>> data = new ArrayList<>();
        for (ControlledLedgerEntry e : rows) {
            data.add(java.util.Arrays.asList(InventoryFormat.dateTime(e.thoiDiem()), e.maChungTu(), e.loai(), e.maThuoc(),
                    e.tenThuoc(), e.soLo(), e.nhap() == 0 ? null : e.nhap(), e.xuat() == 0 ? null : e.xuat(), e.tonSau(),
                    e.doiTac(), e.nguoiThucHien()));
        }
        XlsxExport.write(target, "So theo doi KSDB", "SỔ THEO DÕI THUỐC KIỂM SOÁT ĐẶC BIỆT",
                "N08 Dental · " + scope + " · Xuất lúc " + InventoryFormat.dateTime(LocalDateTime.now()) + " · "
                        + rows.size() + " dòng", columns, data);
    }

    private static final class LedgerModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<ControlledLedgerEntry> rows = List.of();

        void setRows(List<ControlledLedgerEntry> data) {
            rows = List.copyOf(data);
            fireTableDataChanged();
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
            ControlledLedgerEntry e = rows.get(row);
            return switch (column) {
                case 0 -> InventoryFormat.dateTime(e.thoiDiem());
                case 1 -> e.maChungTu();
                case 2 -> e.loai();
                case 3 -> e.maThuoc() + " " + e.tenThuoc();
                case 4 -> e.soLo();
                case 5 -> e.nhap() == 0 ? "" : e.nhap();
                case 6 -> e.xuat() == 0 ? "" : e.xuat();
                case 7 -> e.tonSau();
                case 8 -> e.doiTac();
                default -> e.nguoiThucHien();
            };
        }
    }
}
