package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryCatalogSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Màn hình "Vật tư & thuốc": thống kê, bộ lọc, danh sách có phân trang, thêm/xem/sửa, xuất file.
 *
 * <p>Tồn kho = tồn khả dụng (lô chưa hết hạn), hạn sử dụng = hạn gần nhất còn tồn.
 * Mọi truy vấn chạy trong {@link SwingWorker}.</p>
 */
public final class InventoryItemPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InventoryItemPanel.class);
    static final int PAGE_SIZE = 10;
    private static final String NO_DATA = "—";
    private static final Color PURPLE = new Color(0x6A3DB0);
    private static final String[] COLUMNS = {"STT", "Mã", "Tên vật tư & thuốc", "Phân loại", "ĐVT",
            "Tồn kho", "Định mức", "Hạn sử dụng", "Trạng thái", "Thao tác"};
    private static final int COL_ACTIONS = 9;
    private static final java.time.format.DateTimeFormatter DATE = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] STOCK_STATUSES = {"Tất cả", "Còn hàng", "Sắp hết", "Thiếu", "Hết hàng"};

    private final transient InventoryItemService service;
    private final transient Supplier<Set<Role>> sessionRoles;

    // Thống kê
    private final JLabel lblTotalValue = statValue(NUMBER_BLUE_DARK);
    private final JLabel lblMedicineValue = statValue(NUMBER_BLUE);
    private final JLabel lblSupplyValue = statValue(PURPLE);
    private final JLabel lblRestockValue = statValue(ORANGE);
    private final JLabel lblRestockCaption = new JLabel("Dưới định mức");

    // Bộ lọc
    private final HintTextField txtSearch = new HintTextField("Tìm kiếm theo mã, tên...", 18);
    private final JToggleButton tglAllTypes = createSegment("Tất cả");
    private final JToggleButton tglMedicine = createSegment("Thuốc");
    private final JToggleButton tglSupply = createSegment("Vật tư y tế");
    private final JComboBox<String> cmbStockStatus = new JComboBox<>(STOCK_STATUSES);
    private final JButton btnAdd = createButton("Thêm mới", ButtonKind.PRIMARY);
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final Timer searchDelay = new Timer(300, e -> reload());

    // Danh sách
    private final ItemTableModel tableModel = new ItemTableModel();
    private final JTable tblItems = new JTable(tableModel);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<InventoryItemResponse> allRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(InventoryCatalogSummaryResponse summary, List<InventoryItemResponse> rows) {
    }

    public InventoryItemPanel(InventoryItemService service, Supplier<Set<Role>> sessionRoles) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.sessionRoles = Objects.requireNonNull(sessionRoles, "sessionRoles must not be null");
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        ScrollablePage pnlPage = new ScrollablePage();
        pnlPage.setLayout(new BoxLayout(pnlPage, BoxLayout.Y_AXIS));
        pnlPage.setBackground(BACKGROUND);
        pnlPage.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));
        addFullWidth(pnlPage, buildHeader());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildStats());
        pnlPage.add(Box.createVerticalStrut(18));
        addFullWidth(pnlPage, buildFilterCard());
        pnlPage.add(Box.createVerticalStrut(18));
        addFullWidth(pnlPage, buildListCard());

        JScrollPane scrPage = new JScrollPane(pnlPage);
        scrPage.setBorder(BorderFactory.createEmptyBorder());
        scrPage.getViewport().setBackground(BACKGROUND);
        scrPage.getVerticalScrollBar().setUnitIncrement(18);
        add(scrPage, BorderLayout.CENTER);

        wireEvents();
        reload();
    }

    /** Tải lại thống kê + danh sách theo bộ lọc hiện tại, quay về trang 1. */
    public void reload() {
        searchDelay.stop();
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        String keyword = txtSearch.getText();
        InventoryItemType type = selectedType();
        Set<Role> roles = sessionRoles.get();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải danh mục…");
        runningLoad = new SwingWorker<>() {
            @Override
            protected LoadResult doInBackground() {
                return new LoadResult(service.summarize(roles), service.search(roles, keyword, type));
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    LoadResult result = get();
                    applySummary(result.summary());
                    allRows = result.rows();
                    currentPage = 0;
                    showPage();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    allRows = List.of();
                    tableModel.setPage(List.of(), 0);
                    rebuildPager(0);
                    lblPageInfo.setForeground(RED);
                    lblPageInfo.setText(describe(e.getCause()));
                }
            }
        };
        runningLoad.execute();
    }

    // ------------------------------------------------------------------ layout

    private static void addFullWidth(JPanel target, JComponent child) {
        child.setAlignmentX(LEFT_ALIGNMENT);
        target.add(child);
    }

    private JComponent buildHeader() {
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Vật tư & thuốc");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel(
                "Quản lý danh mục, số lượng tồn kho và thông tin chi tiết các loại vật tư, thuốc dùng trong phòng khám.");
        lblSubtitle.setFont(font(Font.PLAIN, 15));
        lblSubtitle.setForeground(TEXT_MUTED);
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(6));
        pnlHeader.add(lblSubtitle);
        return pnlHeader;
    }

    private JComponent buildStats() {
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 18, 0));
        pnlStats.setOpaque(false);
        pnlStats.add(statCard("Tổng mặt hàng", lblTotalValue, new JLabel("Thuốc & vật tư")));
        pnlStats.add(statCard("Thuốc y tế", lblMedicineValue, new JLabel("Mặt hàng")));
        pnlStats.add(statCard("Vật tư tiêu hao", lblSupplyValue, new JLabel("Mặt hàng")));
        pnlStats.add(statCard("Cần bổ sung", lblRestockValue, lblRestockCaption));
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlStats.getPreferredSize().height));
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel(NO_DATA);
        lblValue.setFont(font(Font.BOLD, 40));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static JComponent statCard(String title, JLabel lblValue, JLabel lblCaption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(4));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    private JComponent buildFilterCard() {
        Card pnlFilterCard = new Card(new BorderLayout(14, 0), 16);

        JPanel pnlLeft = new JPanel(new BorderLayout(0, 6));
        pnlLeft.setOpaque(false);
        pnlLeft.add(caption("KHU VỰC BỘ LỌC"), BorderLayout.NORTH);
        JPanel pnlLeftControls = new JPanel(new BorderLayout(12, 0));
        pnlLeftControls.setOpaque(false);
        pnlLeftControls.add(buildSearchBox(), BorderLayout.CENTER);
        pnlLeftControls.add(buildTypeSegments(), BorderLayout.EAST);
        pnlLeft.add(pnlLeftControls, BorderLayout.CENTER);

        JPanel pnlStatus = new JPanel(new BorderLayout(0, 6));
        pnlStatus.setOpaque(false);
        pnlStatus.add(caption("Trạng thái"), BorderLayout.NORTH);
        styleCombo(cmbStockStatus);
        cmbStockStatus.setPreferredSize(new Dimension(220, 40));
        cmbStockStatus.setToolTipText("Lọc theo trạng thái tồn khả dụng so với định mức");
        pnlStatus.add(cmbStockStatus, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlButtons.setOpaque(false);
        btnAdd.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnAdd.setIconTextGap(8);
        btnAdd.setFont(font(Font.BOLD, 15));
        btnExport.setIcon(new GlyphIcon(MenuGlyph.UPLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        pnlButtons.add(btnAdd);
        pnlButtons.add(btnExport);
        JPanel pnlButtonsAligned = new JPanel(new BorderLayout());
        pnlButtonsAligned.setOpaque(false);
        pnlButtonsAligned.add(pnlButtons, BorderLayout.SOUTH);

        JPanel pnlRight = new JPanel(new BorderLayout(12, 0));
        pnlRight.setOpaque(false);
        pnlRight.add(pnlStatus, BorderLayout.CENTER);
        pnlRight.add(pnlButtonsAligned, BorderLayout.EAST);

        pnlFilterCard.add(pnlLeft, BorderLayout.CENTER);
        pnlFilterCard.add(pnlRight, BorderLayout.EAST);
        pnlFilterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlFilterCard.getPreferredSize().height));
        return pnlFilterCard;
    }

    private JComponent buildSearchBox() {
        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(280, 40));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);
        return pnlSearch;
    }

    private JComponent buildTypeSegments() {
        return createSegmentBar(tglAllTypes, tglAllTypes, tglMedicine, tglSupply);
    }

    private static JLabel caption(String text) {
        JLabel lblCaption = new JLabel(text);
        lblCaption.setFont(font(Font.PLAIN, 14));
        lblCaption.setForeground(TEXT);
        return lblCaption;
    }

    private JComponent buildListCard() {
        Card pnlListCard = new Card(new BorderLayout(0, 14), 18);
        JLabel lblListTitle = new JLabel("DANH SÁCH VẬT TƯ & THUỐC");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        pnlListCard.add(lblListTitle, BorderLayout.NORTH);

        tblItems.setFont(font(Font.PLAIN, 15));
        tblItems.setForeground(TEXT);
        tblItems.setRowHeight(50);
        tblItems.setShowVerticalLines(false);
        tblItems.setGridColor(BORDER);
        tblItems.setRowSelectionAllowed(false);
        tblItems.setFocusable(false);
        tblItems.setBackground(SURFACE);
        tblItems.setDefaultRenderer(Object.class, new CellRenderer());
        int[] widths = {50, 75, 230, 135, 65, 85, 95, 125, 110, 115};
        for (int i = 0; i < widths.length; i++) {
            tblItems.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhItems = tblItems.getTableHeader();
        tbhItems.setReorderingAllowed(false);
        tbhItems.setResizingAllowed(false);
        tbhItems.setPreferredSize(new Dimension(10, 50));
        tbhItems.setDefaultRenderer(new HeaderRenderer());

        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setBackground(SURFACE);
        pnlTable.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        pnlTable.add(tbhItems, BorderLayout.NORTH);
        pnlTable.add(tblItems, BorderLayout.CENTER);
        pnlListCard.add(pnlTable, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        lblPageInfo.setForeground(TEXT);
        pnlPager.onPageSelected(this::goToPage);
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);
        pnlListCard.add(pnlFooter, BorderLayout.SOUTH);
        return pnlListCard;
    }

    private void wireEvents() {
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchDelay.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchDelay.restart();
            }
        });
        searchDelay.setRepeats(false);
        txtSearch.addActionListener(e -> reload());
        for (JToggleButton tglSegment : new JToggleButton[] {tglAllTypes, tglMedicine, tglSupply}) {
            tglSegment.addActionListener(e -> reload());
        }
        btnAdd.addActionListener(e -> openDialog(null, false));
        btnExport.addActionListener(e -> exportCurrentList());
        cmbStockStatus.addActionListener(e -> {
            currentPage = 0;
            showPage();
        });

        MouseAdapter actionClicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblItems.rowAtPoint(e.getPoint());
                int column = tblItems.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                InventoryItemResponse item = tableModel.rowAt(row);
                if (column == COL_ACTIONS) {
                    Rectangle cell = tblItems.getCellRect(row, column, false);
                    boolean edit = e.getX() - cell.x > cell.width / 2;
                    openDialog(item, !edit);
                } else if (e.getClickCount() == 2) {
                    openDialog(item, true);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean onActions = tblItems.columnAtPoint(e.getPoint()) == COL_ACTIONS
                        && tblItems.rowAtPoint(e.getPoint()) >= 0;
                tblItems.setCursor(Cursor.getPredefinedCursor(onActions ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblItems.addMouseListener(actionClicks);
        tblItems.addMouseMotionListener(actionClicks);
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(InventoryCatalogSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongMatHang()));
        lblMedicineValue.setText(String.valueOf(s.soThuoc()));
        lblSupplyValue.setText(String.valueOf(s.soVatTu()));
        if (s.coDuLieuTonKho()) {
            lblRestockValue.setText(String.valueOf(s.soCanBoSung()));
            lblRestockValue.setForeground(ORANGE);
            lblRestockCaption.setText("Dưới định mức");
            lblRestockCaption.setForeground(TEXT);
        } else {
            lblRestockValue.setText(NO_DATA);
            lblRestockValue.setForeground(BORDER_STRONG);
            lblRestockCaption.setText("Chưa có dữ liệu tồn kho");
            lblRestockCaption.setForeground(TEXT_MUTED);
        }
        tglAllTypes.setText("Tất cả (" + s.tongMatHang() + ")");
        tglMedicine.setText("Thuốc (" + s.soThuoc() + ")");
        tglSupply.setText("Vật tư y tế (" + s.soVatTu() + ")");
    }

    /** Lọc theo trạng thái tồn ở phía giao diện (dữ liệu đã tải theo từ khóa + loại). */
    private List<InventoryItemResponse> visibleRows() {
        StockLevel level = switch (cmbStockStatus.getSelectedIndex()) {
            case 1 -> StockLevel.DU;
            case 2 -> StockLevel.SAP_HET;
            case 3 -> StockLevel.THIEU;
            case 4 -> StockLevel.HET_HANG;
            default -> null;
        };
        return level == null ? allRows : allRows.stream().filter(r -> r.mucTon() == level).toList();
    }

    private void showPage() {
        List<InventoryItemResponse> rows = visibleRows();
        int total = rows.size();
        int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(total, from + PAGE_SIZE);
        tableModel.setPage(rows.subList(from, to), from);
        lblPageInfo.setForeground(TEXT);
        lblPageInfo.setText(total == 0
                ? "Không có mặt hàng nào khớp bộ lọc."
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " mặt hàng");
        rebuildPager(total == 0 ? 0 : pageCount);
    }

    private void rebuildPager(int pageCount) {
        pnlPager.update(currentPage, pageCount);
    }

    private void goToPage(int page) {
        currentPage = page;
        showPage();
    }

    // ----------------------------------------------------------------- actions

    private void openDialog(InventoryItemResponse existing, boolean viewOnly) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        InventoryItemDialog dlgItem = new InventoryItemDialog(owner, service, sessionRoles, existing, viewOnly);
        dlgItem.setVisible(true);
        dlgItem.savedItem().ifPresent(saved -> reload());
    }

    /** Xuất toàn bộ danh sách đang lọc (mọi trang) ra file Excel .xlsx có định dạng. */
    private void exportCurrentList() {
        if (visibleRows().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách vật tư & thuốc");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("vat-tu-thuoc.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<InventoryItemResponse> snapshot = visibleRows();
        btnExport.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                writeExcel(target, snapshot);
                return null;
            }

            @Override
            protected void done() {
                btnExport.setEnabled(true);
                try {
                    get();
                    lblPageInfo.setForeground(GREEN);
                    lblPageInfo.setText("Đã xuất " + snapshot.size() + " mặt hàng ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblPageInfo.setForeground(RED);
                    lblPageInfo.setText("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<InventoryItemResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã", 10, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tên vật tư & thuốc", 36, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Phân loại", 16, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("ĐVT", 9, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tồn kho", 11, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Định mức", 11, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Hạn sử dụng gần nhất", 15, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Trạng thái", 13, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Hoạt chất", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Hàm lượng", 12, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("KSĐB", 8, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mô tả", 32, XlsxExport.Kind.TEXT));
        List<List<Object>> data = new java.util.ArrayList<>();
        int stt = 1;
        for (InventoryItemResponse r : rows) {
            data.add(java.util.Arrays.asList(stt++, r.ma(), r.ten(), categoryLabel(r.loai()), r.donViTinh(),
                    r.tonKho(), r.dinhMucToiThieu(), r.hanSuDungGanNhat(), r.mucTon().label(),
                    r.hoatChat(), r.hamLuong(), r.kiemSoatDacBiet() ? "Có" : null, r.moTa()));
        }
        XlsxExport.write(target, "Vat tu va thuoc", "DANH SÁCH VẬT TƯ & THUỐC",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(java.time.LocalDateTime.now()) + " · "
                        + rows.size() + " mặt hàng", columns, data);
    }

    static String categoryLabel(InventoryItemType type) {
        return type == InventoryItemType.THUOC ? "Thuốc" : "Vật tư tiêu hao";
    }

    private InventoryItemType selectedType() {
        if (tglMedicine.isSelected()) {
            return InventoryItemType.THUOC;
        }
        return tglSupply.isSelected() ? InventoryItemType.VAT_TU : null;
    }

    /** Thông báo lỗi cho người dùng; lỗi ngoài dự kiến được ghi log đầy đủ để tra cứu. */
    static String describe(Throwable cause) {
        if (cause instanceof PermissionDeniedException) {
            return "Tài khoản hiện tại không có quyền thực hiện thao tác này.";
        }
        if (cause instanceof InventoryValidationException) {
            return cause.getMessage();
        }
        LOG.error("Lỗi khi xử lý dữ liệu kho", cause);
        if (cause instanceof org.springframework.dao.PessimisticLockingFailureException
                || cause instanceof org.springframework.dao.ConcurrencyFailureException) {
            return "Dữ liệu đang được người khác cập nhật. Vui lòng thử lại sau giây lát.";
        }
        if (cause instanceof org.springframework.dao.DataAccessResourceFailureException
                || cause instanceof org.springframework.transaction.CannotCreateTransactionException) {
            return "Không kết nối được cơ sở dữ liệu. Kiểm tra SQL Server rồi thử lại.";
        }
        return "Không thực hiện được thao tác (lỗi hệ thống, chi tiết đã ghi vào log).";
    }

    // ------------------------------------------------------------------- table

    private static final class ItemTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<InventoryItemResponse> rows = List.of();
        private int offset;

        void setPage(List<InventoryItemResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        InventoryItemResponse rowAt(int row) {
            return rows.get(row);
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
            InventoryItemResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.ma();
                case 2 -> r.ten();
                case 3 -> categoryLabel(r.loai());
                case 4 -> r.donViTinh();
                case 5 -> r.tonKho();
                case 6 -> r.dinhMucToiThieu();
                case 7 -> r.hanSuDungGanNhat() == null ? NO_DATA : DATE.format(r.hanSuDungGanNhat());
                case 8 -> r.mucTon();
                default -> "";
            };
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lblCell = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lblCell.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            lblCell.setHorizontalAlignment(SwingConstants.LEFT);
            lblCell.setForeground(NO_DATA.equals(value) ? TEXT_MUTED : TEXT);
            lblCell.setIcon(null);
            if (value instanceof StockLevel level) {
                lblCell.setText(level.label());
                lblCell.setIconTextGap(8);
                lblCell.setIcon(new DotIcon(switch (level) {
                    case HET_HANG, THIEU -> RED;
                    case SAP_HET -> ORANGE;
                    case DU -> GREEN;
                }, 11));
            }
            if (column == COL_ACTIONS) {
                lblCell.setText("<html><span style='color:#26366B'>[Xem]</span>&nbsp;&nbsp;"
                        + "<span style='color:#26366B'>[Sửa]</span></html>");
            }
            return lblCell;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel lblHeaderCell = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lblHeaderCell.setFont(font(Font.BOLD, 15));
            lblHeaderCell.setForeground(TEXT);
            lblHeaderCell.setBackground(HEADER_ROW);
            lblHeaderCell.setOpaque(true);
            lblHeaderCell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 8)));
            return lblHeaderCell;
        }
    }

    /** Trang co theo chiều ngang khung nhìn, chỉ cuộn dọc. */
    private static final class ScrollablePage extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 40, 18);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
