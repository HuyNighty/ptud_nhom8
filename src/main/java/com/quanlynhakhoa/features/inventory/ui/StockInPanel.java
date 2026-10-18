package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.SupplierResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.StockInService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ActionCell;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.CellRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.RowAction;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ScrollablePage;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.StatusRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.TimeRange;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
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
import javax.swing.JSeparator;
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
import javax.swing.table.TableCellRenderer;

/**
 * Màn hình "Nhập kho" (UC30): thống kê, bộ lọc, danh sách phiếu, tạo/xem/in/sửa và xác nhận nhập kho.
 * Mọi truy vấn chạy trong {@link SwingWorker}; người thao tác lấy từ phiên qua {@code session}.
 */
public final class StockInPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 10;
    private static final String[] COLUMNS =
            {"STT", "Mã phiếu", "Ngày nhập", "Nhà cung cấp", "Người nhập", "Tổng tiền", "Trạng thái", "Thao tác"};
    private static final int COL_STATUS = 6;
    private static final int COL_ACTIONS = 7;
    private static final Color AMBER = new Color(0xD9A300);
    private static final String ALL_SUPPLIERS = "Nhà cung cấp";

    private final transient StockInService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotalValue = statValue(TEXT);
    private final JLabel lblMonthValue = statValue(TEXT);
    private final JLabel lblMonthCaption = new JLabel(" ");
    private final JLabel lblPendingValue = statValue(AMBER);
    private final JLabel lblCompletedValue = statValue(GREEN);

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm theo mã phiếu, nhà cung cấp...", 24);
    private final JComboBox<Object> cmbSupplier = new JComboBox<>(new Object[] {ALL_SUPPLIERS});
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final JButton btnCreate = createButton("Tạo phiếu nhập", ButtonKind.PRIMARY);
    private final JToggleButton tglAll = createSegment("Tất cả");
    private final JToggleButton tglThisMonth = createSegment("Tháng này");
    private final JToggleButton tglPending = createSegment("Chờ xử lý");
    private final JToggleButton tglCompleted = createSegment("Đã hoàn thành");
    private final JComboBox<TimeRange> cmbTimeRange = new JComboBox<>(TimeRange.values());
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final ReceiptTableModel tableModel = new ReceiptTableModel();
    private final JTable tblReceipts = new JTable(tableModel);
    private final ActionCell actionCell = new ActionCell("Nhập kho", COL_STATUS);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<StockInRowResponse> allRows = List.of();
    private transient List<StockInRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(StockInSummaryResponse summary, List<StockInRowResponse> rows,
                              List<SupplierResponse> suppliers) {
    }

    public StockInPanel(StockInService service, InventoryItemService itemService, Supplier<InventoryActor> session) {
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        ScrollablePage pnlPage = new ScrollablePage();
        pnlPage.setLayout(new BoxLayout(pnlPage, BoxLayout.Y_AXIS));
        pnlPage.setBackground(BACKGROUND);
        pnlPage.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));
        addFullWidth(pnlPage, buildHeader());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildStats());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildToolbar());
        pnlPage.add(Box.createVerticalStrut(14));
        addFullWidth(pnlPage, buildSegmentsRow());
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

    /** Tải lại thống kê, danh sách phiếu và nhà cung cấp; giữ nguyên bộ lọc đang chọn. */
    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<com.quanlynhakhoa.features.auth.enums.Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải phiếu nhập…");
        runningLoad = new SwingWorker<>() {
            @Override
            protected LoadResult doInBackground() {
                return new LoadResult(service.summarize(roles), service.list(roles), service.listSuppliers(roles));
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    LoadResult result = get();
                    applySummary(result.summary());
                    applySuppliers(result.suppliers());
                    allRows = result.rows();
                    applyFilters();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    allRows = List.of();
                    filteredRows = List.of();
                    tableModel.setPage(List.of(), 0);
                    pnlPager.update(0, 0);
                    lblPageInfo.setForeground(RED);
                    lblPageInfo.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        };
        runningLoad.execute();
    }

    // ------------------------------------------------------------------ layout

    private static void addFullWidth(JPanel target, JComponent child) {
        child.setAlignmentX(LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getMaximumSize().height));
        target.add(child);
    }

    private static JComponent buildHeader() {
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Nhập kho");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel(
                "Quản lý các phiếu nhập kho, theo dõi lịch sử nhập vật tư, thuốc và tạo phiếu nhập mới.");
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
        pnlStats.add(statCard("Tổng phiếu nhập", lblTotalValue, caption("Tất cả các đợt nhập")));
        pnlStats.add(statCard("Giá trị nhập tháng này", lblMonthValue, lblMonthCaption));
        pnlStats.add(statCard("Chờ nhập kho", lblPendingValue, caption("Đang kiểm tra/chờ nhập kho")));
        pnlStats.add(statCard("Đã hoàn thành", lblCompletedValue, caption("Nhập kho thành công")));
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlStats.getPreferredSize().height));
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static JLabel caption(String text) {
        JLabel lblCaption = new JLabel(text);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT);
        return lblCaption;
    }

    private static JComponent statCard(String title, JLabel lblValue, JLabel lblCaption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        lblCaption.setFont(font(Font.PLAIN, 15));
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(6));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    private JComponent buildToolbar() {
        JPanel pnlToolbar = new JPanel(new BorderLayout(12, 0));
        pnlToolbar.setOpaque(false);

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(520, 44));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        styleCombo(cmbSupplier);
        cmbSupplier.setPreferredSize(new Dimension(240, 44));
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeft.setOpaque(false);
        pnlLeft.add(pnlSearch);
        pnlLeft.add(Box.createHorizontalStrut(12));
        pnlLeft.add(cmbSupplier);

        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlRight.setOpaque(false);
        pnlRight.add(btnExport);
        pnlRight.add(btnCreate);

        pnlToolbar.add(pnlLeft, BorderLayout.CENTER);
        pnlToolbar.add(pnlRight, BorderLayout.EAST);
        return pnlToolbar;
    }

    private JComponent buildSegmentsRow() {
        JPanel pnlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlRow.setOpaque(false);
        pnlRow.add(createSegmentBar(tglAll, tglAll, tglThisMonth, tglPending, tglCompleted));
        pnlRow.add(Box.createHorizontalStrut(14));
        JSeparator sepFilters = new JSeparator(SwingConstants.VERTICAL);
        sepFilters.setPreferredSize(new Dimension(1, 34));
        sepFilters.setForeground(BORDER);
        pnlRow.add(sepFilters);
        pnlRow.add(Box.createHorizontalStrut(14));
        styleCombo(cmbTimeRange);
        cmbTimeRange.setPreferredSize(new Dimension(210, 44));
        JLabel lblCalendar = new JLabel(new GlyphIcon(MenuGlyph.CALENDAR, 20, TEXT));
        JPanel pnlTime = new JPanel(new BorderLayout(6, 0));
        pnlTime.setOpaque(false);
        pnlTime.add(lblCalendar, BorderLayout.WEST);
        pnlTime.add(cmbTimeRange, BorderLayout.CENTER);
        pnlRow.add(pnlTime);
        return pnlRow;
    }

    private JComponent buildListCard() {
        Card pnlListCard = new Card(new BorderLayout(0, 14), 18);
        JLabel lblListTitle = new JLabel("DANH SÁCH PHIẾU NHẬP KHO");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        pnlListCard.add(lblListTitle, BorderLayout.NORTH);

        tblReceipts.setFont(font(Font.PLAIN, 15));
        tblReceipts.setForeground(TEXT);
        tblReceipts.setRowHeight(54);
        tblReceipts.setShowVerticalLines(false);
        tblReceipts.setGridColor(BORDER);
        tblReceipts.setRowSelectionAllowed(false);
        tblReceipts.setFocusable(false);
        tblReceipts.setBackground(SURFACE);
        tblReceipts.setDefaultRenderer(Object.class, new CellRenderer());
        tblReceipts.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());
        tblReceipts.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {50, 95, 110, 220, 150, 130, 160, 190};
        for (int i = 0; i < widths.length; i++) {
            tblReceipts.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhReceipts = tblReceipts.getTableHeader();
        tbhReceipts.setReorderingAllowed(false);
        tbhReceipts.setResizingAllowed(false);
        tbhReceipts.setPreferredSize(new Dimension(10, 50));
        tbhReceipts.setDefaultRenderer(new HeaderRenderer());

        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setBackground(SURFACE);
        pnlTable.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        pnlTable.add(tbhReceipts, BorderLayout.NORTH);
        pnlTable.add(tblReceipts, BorderLayout.CENTER);
        pnlListCard.add(pnlTable, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        pnlPager.onPageSelected(page -> {
            currentPage = page;
            showPage();
        });
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);
        pnlListCard.add(pnlFooter, BorderLayout.SOUTH);
        return pnlListCard;
    }

    private void wireEvents() {
        searchDelay.setRepeats(false);
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
        txtSearch.addActionListener(e -> applyFilters());
        cmbSupplier.addActionListener(e -> applyFilters());
        cmbTimeRange.addActionListener(e -> applyFilters());
        for (JToggleButton tglSegment : new JToggleButton[] {tglAll, tglThisMonth, tglPending, tglCompleted}) {
            tglSegment.addActionListener(e -> applyFilters());
        }
        btnCreate.addActionListener(e -> openEditor(null));
        btnExport.addActionListener(e -> exportCsv());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblReceipts.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                StockInRowResponse receipt = tableModel.rowAt(row);
                if (tblReceipts.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    Rectangle cell = tblReceipts.getCellRect(row, COL_ACTIONS, false);
                    RowAction action = actionCell.hitTest(receipt.trangThai(), e.getX() - cell.x,
                            tblReceipts.getFontMetrics(actionCell.getFont()));
                    if (action != null) {
                        perform(action, receipt);
                    }
                } else if (e.getClickCount() == 2) {
                    perform(RowAction.VIEW, receipt);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tblReceipts.rowAtPoint(e.getPoint());
                boolean hand = false;
                if (row >= 0 && tblReceipts.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    Rectangle cell = tblReceipts.getCellRect(row, COL_ACTIONS, false);
                    hand = actionCell.hitTest(tableModel.rowAt(row).trangThai(), e.getX() - cell.x,
                            tblReceipts.getFontMetrics(actionCell.getFont())) != null;
                }
                tblReceipts.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblReceipts.addMouseListener(clicks);
        tblReceipts.addMouseMotionListener(clicks);
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(StockInSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongPhieu()));
        lblMonthValue.setText(InventoryFormat.money(s.giaTriThangNay()));
        lblPendingValue.setText(String.valueOf(s.soChoXuLy()));
        lblCompletedValue.setText(String.valueOf(s.soHoanThanh()));
        lblMonthCaption.setText(compareWithLastMonth(s.giaTriThangNay(), s.giaTriThangTruoc()));
        tglAll.setText("Tất cả (" + s.tongPhieu() + ")");
        tglPending.setText("<html>Chờ xử lý <span style='color:#B08600'>(" + s.soChoXuLy() + ")</span></html>");
        tglCompleted.setText("<html>Đã hoàn thành <span style='color:#1E6B34'>(" + s.soHoanThanh() + ")</span></html>");
    }

    /** "Nhiều hơn 12% tháng trước" (xanh) / "Ít hơn 5% tháng trước" (đỏ) / chưa có dữ liệu. */
    static String compareWithLastMonth(BigDecimal thisMonth, BigDecimal lastMonth) {
        if (lastMonth == null || lastMonth.signum() == 0) {
            return "<html><span style='color:#5E6678'>Chưa có dữ liệu tháng trước</span></html>";
        }
        BigDecimal pct = thisMonth.subtract(lastMonth).multiply(BigDecimal.valueOf(100))
                .divide(lastMonth, 0, RoundingMode.HALF_UP);
        if (pct.signum() == 0) {
            return "<html>Bằng tháng trước</html>";
        }
        boolean up = pct.signum() > 0;
        return "<html><span style='color:" + (up ? "#2E7D32" : "#D32F2F") + "'>"
                + (up ? "Nhiều hơn " : "Ít hơn ") + pct.abs() + "%</span> tháng trước</html>";
    }

    private void applySuppliers(List<SupplierResponse> suppliers) {
        Object selected = cmbSupplier.getSelectedItem();
        String selectedCode = selected instanceof SupplierResponse s ? s.ma() : null;
        cmbSupplier.removeAllItems();
        cmbSupplier.addItem(ALL_SUPPLIERS);
        for (SupplierResponse s : suppliers) {
            cmbSupplier.addItem(s);
            if (s.ma().equals(selectedCode)) {
                cmbSupplier.setSelectedItem(s);
            }
        }
    }

    private void applyFilters() {
        searchDelay.stop();
        LocalDate today = LocalDate.now();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        Object supplier = cmbSupplier.getSelectedItem();
        String supplierCode = supplier instanceof SupplierResponse s ? s.ma() : null;
        TimeRange range = (TimeRange) cmbTimeRange.getSelectedItem();
        Predicate<StockInRowResponse> segment = r -> true;
        if (tglThisMonth.isSelected()) {
            segment = r -> TimeRange.THIS_MONTH.contains(r.ngayNhap(), today);
        } else if (tglPending.isSelected()) {
            segment = r -> r.trangThai() == VoucherStatus.CHO_XU_LY;
        } else if (tglCompleted.isSelected()) {
            segment = r -> r.trangThai() == VoucherStatus.DA_HOAN_THANH;
        }
        List<StockInRowResponse> result = new ArrayList<>();
        for (StockInRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maPhieu().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.tenNhaCungCap().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesSupplier = supplierCode == null || supplierCode.equals(r.maNhaCungCap());
            boolean matchesTime = range == null || range.contains(r.ngayNhap(), today);
            if (matchesKeyword && matchesSupplier && matchesTime && segment.test(r)) {
                result.add(r);
            }
        }
        filteredRows = List.copyOf(result);
        currentPage = 0;
        showPage();
    }

    private void showPage() {
        int total = filteredRows.size();
        int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(total, from + PAGE_SIZE);
        tableModel.setPage(filteredRows.subList(from, to), from);
        lblPageInfo.setForeground(TEXT);
        lblPageInfo.setText(total == 0
                ? (allRows.isEmpty() ? "Chưa có phiếu nhập nào — bấm \"Tạo phiếu nhập\" để bắt đầu."
                        : "Không có phiếu nhập nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " phiếu nhập");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private void perform(RowAction action, StockInRowResponse row) {
        switch (action) {
            case VIEW -> openDetail(row.maPhieu());
            case PRINT -> printReceipt(row.maPhieu());
            case EDIT -> editNote(row.maPhieu());
            case PROCESS -> receive(row.maPhieu());
        }
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    /** @param maPhieu null = tạo phiếu mới */
    void openEditor(String maPhieu) {
        if (maPhieu == null) {
            showEditor(null);
            return;
        }
        runDetail(maPhieu, this::showEditor);
    }

    private void showEditor(StockInDetailResponse existing) {
        StockInReceiptDialog dlgReceipt = new StockInReceiptDialog(owner(), service, itemService, session, existing);
        dlgReceipt.setVisible(true);
        dlgReceipt.savedReceipt().ifPresent(saved -> {
            reload();
            showInfo("Đã lưu phiếu " + saved.maPhieu() + " (" + saved.trangThai().label() + ").");
        });
    }

    private void openDetail(String maPhieu) {
        runDetail(maPhieu, detail -> {
            StockInDetailDialog dlgDetail = new StockInDetailDialog(owner(), service, session, detail, this::openEditor);
            dlgDetail.setVisible(true);
            if (dlgDetail.changed()) {
                reload();
            }
        });
    }

    private void printReceipt(String maPhieu) {
        runDetail(maPhieu, detail -> StockInPrinter.print(this, detail));
    }

    /** Phiếu đã hoàn thành: chỉ sửa ghi chú, không đổi số lượng/lô đã cộng vào tồn. */
    private void editNote(String maPhieu) {
        runDetail(maPhieu, detail -> {
            Object input = JOptionPane.showInputDialog(this,
                    "Phiếu đã nhập kho nên chỉ sửa được ghi chú (tồn kho giữ nguyên).",
                    "Sửa ghi chú " + maPhieu, JOptionPane.PLAIN_MESSAGE, null, null,
                    detail.ghiChu() == null ? "" : detail.ghiChu());
            if (input == null) {
                return;
            }
            InventoryActor actor = session.get();
            runWrite(() -> service.updateNote(actor, maPhieu, input.toString()), "Đã cập nhật ghi chú phiếu " + maPhieu + ".");
        });
    }

    private void receive(String maPhieu) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Xác nhận nhập kho phiếu " + maPhieu + "?\nSố lượng trên phiếu sẽ được cộng vào tồn kho theo từng lô.",
                "Nhập kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        InventoryActor actor = session.get();
        runWrite(() -> service.complete(actor, maPhieu), "Đã nhập kho phiếu " + maPhieu + ".");
    }

    private void runDetail(String maPhieu, java.util.function.Consumer<StockInDetailResponse> onLoaded) {
        Set<com.quanlynhakhoa.features.auth.enums.Role> roles = session.get().roles();
        new SwingWorker<StockInDetailResponse, Void>() {
            @Override
            protected StockInDetailResponse doInBackground() {
                return service.getDetail(roles, maPhieu);
            }

            @Override
            protected void done() {
                try {
                    onLoaded.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void runWrite(Supplier<StockInDetailResponse> call, String successMessage) {
        new SwingWorker<StockInDetailResponse, Void>() {
            @Override
            protected StockInDetailResponse doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                try {
                    get();
                    reload();
                    showInfo(successMessage);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            lblPageInfo.setForeground(GREEN);
            lblPageInfo.setText(message);
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Nhập kho", JOptionPane.WARNING_MESSAGE);
    }

    /** Xuất danh sách phiếu đang lọc ra file Excel .xlsx có định dạng. */
    private void exportCsv() {
        if (filteredRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách phiếu nhập");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("phieu-nhap-kho.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<StockInRowResponse> snapshot = filteredRows;
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
                    showInfo("Đã xuất " + snapshot.size() + " phiếu nhập ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<StockInRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã phiếu", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày nhập", 13, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Nhà cung cấp", 32, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người nhập", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Tổng tiền (đ)", 16, XlsxExport.Kind.MONEY),
                new XlsxExport.Column("Trạng thái", 15, XlsxExport.Kind.CENTER));
        List<List<Object>> data = new ArrayList<>();
        int stt = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (StockInRowResponse r : rows) {
            data.add(List.of(stt++, r.maPhieu(), r.ngayNhap(), r.tenNhaCungCap(), r.nguoiLap(), r.tongTien(),
                    r.trangThai().label()));
            if (r.trangThai() == VoucherStatus.DA_HOAN_THANH) {
                total = total.add(r.tongTien());
            }
        }
        XlsxExport.write(target, "Phieu nhap kho", "DANH SÁCH PHIẾU NHẬP KHO",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(java.time.LocalDateTime.now()) + " · "
                        + rows.size() + " phiếu · Tổng giá trị đã nhập kho: " + InventoryFormat.money(total),
                columns, data);
    }

    // ------------------------------------------------------------------- table

    private static final class ReceiptTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<StockInRowResponse> rows = List.of();
        private int offset;

        void setPage(List<StockInRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        StockInRowResponse rowAt(int row) {
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
            StockInRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maPhieu();
                case 2 -> InventoryFormat.date(r.ngayNhap());
                case 3 -> r.tenNhaCungCap();
                case 4 -> r.nguoiLap();
                case 5 -> InventoryFormat.money(r.tongTien());
                default -> r.trangThai();
            };
        }
    }
}
