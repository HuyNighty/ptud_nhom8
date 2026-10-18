package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ActionCell;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.CellRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.RowAction;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ScrollablePage;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.StatusRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.TimeRange;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

/**
 * Màn hình "Xuất kho" (UC31): thống kê, lọc theo lý do / thời gian / trạng thái, danh sách phiếu,
 * tạo phiếu theo gợi ý FEFO và xác nhận xuất kho (trừ tồn, không cho âm tồn).
 * Mọi truy vấn chạy trong {@link SwingWorker}; người thao tác lấy từ phiên qua {@code session}.
 */
public final class StockOutPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 10;
    private static final String[] COLUMNS =
            {"STT", "Mã phiếu", "Ngày xuất", "Lý do xuất", "Người nhận", "Người xuất", "Trạng thái", "Thao tác"};
    private static final int COL_STATUS = 6;
    private static final int COL_ACTIONS = 7;
    /** Lý do hiện thẳng trên thanh lọc (theo Figma); các lý do còn lại nằm trong menu mũi tên. */
    private static final List<StockOutReason> BAR_REASONS =
            List.of(StockOutReason.DIEU_TRI, StockOutReason.HUY_HAO_HUT, StockOutReason.TRA_NCC);

    private final transient StockOutService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotalValue = statValue(TEXT);
    private final JLabel lblMonthValue = statValue(TEXT);
    private final JLabel lblPendingValue = statValue(AMBER);
    private final JLabel lblCompletedValue = statValue(GREEN);

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm mã phiếu, người nhận...", 24);
    private final JToggleButton tglReasonAll = createSegment("Tất cả");
    private final Map<StockOutReason, JToggleButton> reasonToggles = new EnumMap<>(StockOutReason.class);
    private final JToggleButton tglReasonMore = createSegment("");
    private final JPopupMenu pmnMoreReasons = new JPopupMenu();
    private final JComboBox<TimeRange> cmbTimeRange = new JComboBox<>(TimeRange.values());
    private final JButton btnCreate = createButton("Tạo phiếu xuất", ButtonKind.PRIMARY);
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final JToggleButton tglTabAll = createTab("Tất cả");
    private final JToggleButton tglTabPending = createTab("Chờ xử lý");
    private final JToggleButton tglTabCompleted = createTab("Đã hoàn thành");
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final ReceiptTableModel tableModel = new ReceiptTableModel();
    private final JTable tblReceipts = new JTable(tableModel);
    private final ActionCell actionCell = new ActionCell("Xuất kho", COL_STATUS);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient StockOutReason moreReason;
    private transient JToggleButton lastReasonToggle = tglReasonAll;
    private transient List<StockOutRowResponse> allRows = List.of();
    private transient List<StockOutRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(StockOutSummaryResponse summary, List<StockOutRowResponse> rows) {
    }

    public StockOutPanel(StockOutService service, InventoryItemService itemService, Supplier<InventoryActor> session) {
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        for (StockOutReason reason : BAR_REASONS) {
            reasonToggles.put(reason, createSegment(reason.shortLabel()));
        }

        ScrollablePage pnlPage = new ScrollablePage();
        pnlPage.setLayout(new BoxLayout(pnlPage, BoxLayout.Y_AXIS));
        pnlPage.setBackground(BACKGROUND);
        pnlPage.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));
        addFullWidth(pnlPage, buildHeader());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildStats());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildToolbar());
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

    /** Tải lại thống kê và danh sách phiếu; giữ nguyên bộ lọc đang chọn. */
    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải phiếu xuất…");
        runningLoad = new SwingWorker<>() {
            @Override
            protected LoadResult doInBackground() {
                return new LoadResult(service.summarize(roles), service.list(roles));
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
        JLabel lblTitle = new JLabel("Xuất kho");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel(
                "Quản lý các phiếu xuất kho: xuất điều trị, hủy hao hụt, trả nhà cung cấp — xuất theo FEFO, không âm tồn.");
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
        pnlStats.add(statCard("Tổng phiếu xuất", lblTotalValue, "Thống kê toàn bộ"));
        pnlStats.add(statCard("Giá trị xuất tháng này", lblMonthValue, "Tổng giá trị (giá vốn)"));
        pnlStats.add(statCard("Chờ xuất kho", lblPendingValue, "Phiếu chờ duyệt/xuất"));
        pnlStats.add(statCard("Đã hoàn thành", lblCompletedValue, "Số phiếu đã xuất"));
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlStats.getPreferredSize().height));
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static JComponent statCard(String title, JLabel lblValue, String caption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT_MUTED);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(6));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    /** Hàng công cụ: ô tìm kiếm · Lý do xuất (segment + mũi tên) · Tạo phiếu xuất · Xuất Excel. */
    private JComponent buildToolbar() {
        JPanel pnlToolbar = new JPanel(new BorderLayout(12, 0));
        pnlToolbar.setOpaque(false);

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        JLabel lblReason = new JLabel("Lý do xuất");
        lblReason.setFont(font(Font.PLAIN, 15));
        lblReason.setForeground(TEXT_MUTED);
        tglReasonMore.setIcon(new GlyphIcon(MenuGlyph.CHEVRON_DOWN, 14, TEXT));
        tglReasonMore.setHorizontalTextPosition(SwingConstants.LEFT);
        tglReasonMore.setIconTextGap(6);
        tglReasonMore.setToolTipText("Lý do khác");
        List<JToggleButton> segments = new ArrayList<>();
        segments.add(tglReasonAll);
        segments.addAll(reasonToggles.values());
        segments.add(tglReasonMore);
        for (StockOutReason reason : StockOutReason.values()) {
            if (!BAR_REASONS.contains(reason)) {
                JMenuItem mnuReason = new JMenuItem(reason.label());
                mnuReason.setFont(font(Font.PLAIN, 14));
                mnuReason.addActionListener(e -> chooseMoreReason(reason));
                pmnMoreReasons.add(mnuReason);
            }
        }

        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlFilters.setOpaque(false);
        pnlFilters.add(Box.createHorizontalStrut(14));
        pnlFilters.add(lblReason);
        pnlFilters.add(Box.createHorizontalStrut(8));
        pnlFilters.add(createSegmentBar(tglReasonAll, segments.toArray(JToggleButton[]::new)));
        pnlFilters.add(Box.createHorizontalStrut(4));

        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlRight.setOpaque(false);
        pnlRight.add(pnlFilters);
        pnlRight.add(btnCreate);
        pnlRight.add(btnExport);

        // Ô tìm kiếm co giãn theo bề ngang còn lại để hàng công cụ không bị tràn ở màn hẹp.
        pnlSearch.setPreferredSize(new Dimension(360, 44));
        pnlSearch.setMinimumSize(new Dimension(180, 44));
        pnlToolbar.add(pnlSearch, BorderLayout.CENTER);
        pnlToolbar.add(pnlRight, BorderLayout.EAST);
        return pnlToolbar;
    }

    private JComponent buildListCard() {
        Card pnlListCard = new Card(new BorderLayout(0, 14), 18);
        styleCombo(cmbTimeRange);
        cmbTimeRange.setPreferredSize(new Dimension(170, 40));
        JPanel pnlTime = new JPanel(new BorderLayout(6, 0));
        pnlTime.setOpaque(false);
        pnlTime.add(new JLabel(new GlyphIcon(MenuGlyph.CALENDAR, 20, TEXT)), BorderLayout.WEST);
        pnlTime.add(cmbTimeRange, BorderLayout.CENTER);
        JPanel pnlTimeHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlTimeHolder.setOpaque(false);
        pnlTimeHolder.add(pnlTime);
        JPanel pnlTabs = createTabBar(tglTabAll, tglTabAll, tglTabPending, tglTabCompleted);
        JPanel pnlTabRow = new JPanel(new BorderLayout());
        pnlTabRow.setOpaque(false);
        pnlTabRow.setBorder(pnlTabs.getBorder());
        pnlTabs.setBorder(null);
        pnlTabRow.add(pnlTabs, BorderLayout.WEST);
        pnlTabRow.add(pnlTimeHolder, BorderLayout.EAST);
        pnlListCard.add(pnlTabRow, BorderLayout.NORTH);

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
        int[] widths = {50, 95, 110, 170, 190, 150, 160, 190};
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
        cmbTimeRange.addActionListener(e -> applyFilters());
        tglReasonAll.addActionListener(e -> reasonSelected(tglReasonAll));
        reasonToggles.values().forEach(tgl -> tgl.addActionListener(e -> reasonSelected(tgl)));
        tglReasonMore.addActionListener(e -> pmnMoreReasons.show(tglReasonMore, 0, tglReasonMore.getHeight()));
        pmnMoreReasons.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // không cần xử lý
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // Đóng menu mà không chọn: trả lại lựa chọn trước đó.
                SwingUtilities.invokeLater(() -> {
                    if (lastReasonToggle != tglReasonMore) {
                        lastReasonToggle.setSelected(true);
                    }
                });
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // xử lý ở popupMenuWillBecomeInvisible
            }
        });
        for (JToggleButton tglTab : new JToggleButton[] {tglTabAll, tglTabPending, tglTabCompleted}) {
            tglTab.addActionListener(e -> applyFilters());
        }
        btnCreate.addActionListener(e -> openEditor(null));
        btnExport.addActionListener(e -> exportExcel());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblReceipts.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                StockOutRowResponse receipt = tableModel.rowAt(row);
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

    private void reasonSelected(JToggleButton tgl) {
        lastReasonToggle = tgl;
        applyFilters();
    }

    private void chooseMoreReason(StockOutReason reason) {
        moreReason = reason;
        tglReasonMore.setText(reason.shortLabel());
        tglReasonMore.setSelected(true);
        lastReasonToggle = tglReasonMore;
        applyFilters();
    }

    /** Lý do đang lọc; null = tất cả. */
    private StockOutReason selectedReason() {
        if (tglReasonMore.isSelected()) {
            return moreReason;
        }
        for (Map.Entry<StockOutReason, JToggleButton> e : reasonToggles.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(StockOutSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongPhieu()));
        lblMonthValue.setText(InventoryFormat.money(s.giaTriThangNay()));
        lblPendingValue.setText(String.valueOf(s.soChoXuLy()));
        lblCompletedValue.setText(String.valueOf(s.soHoanThanh()));
    }

    private void applyFilters() {
        searchDelay.stop();
        LocalDate today = LocalDate.now();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        StockOutReason reason = selectedReason();
        TimeRange range = (TimeRange) cmbTimeRange.getSelectedItem();
        List<StockOutRowResponse> matching = new ArrayList<>();
        int pending = 0;
        int completed = 0;
        for (StockOutRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maPhieu().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.nguoiNhan().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesReason = reason == null || r.lyDo() == reason;
            boolean matchesTime = range == null || range.contains(r.ngayXuat(), today);
            if (matchesKeyword && matchesReason && matchesTime) {
                matching.add(r);
                pending += r.trangThai() == VoucherStatus.CHO_XU_LY ? 1 : 0;
                completed += r.trangThai() == VoucherStatus.DA_HOAN_THANH ? 1 : 0;
            }
        }
        // Số trên tab phản ánh các bộ lọc phía trên (tìm kiếm, lý do, thời gian).
        tglTabAll.setText("Tất cả (" + matching.size() + ")");
        tglTabPending.setText("Chờ xử lý (" + pending + ")");
        tglTabCompleted.setText("Đã hoàn thành (" + completed + ")");
        VoucherStatus tabStatus = tglTabPending.isSelected() ? VoucherStatus.CHO_XU_LY
                : tglTabCompleted.isSelected() ? VoucherStatus.DA_HOAN_THANH : null;
        filteredRows = tabStatus == null ? List.copyOf(matching)
                : matching.stream().filter(r -> r.trangThai() == tabStatus).toList();
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
                ? (allRows.isEmpty() ? "Chưa có phiếu xuất nào — bấm \"Tạo phiếu xuất\" để bắt đầu."
                        : "Không có phiếu xuất nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " phiếu xuất");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private void perform(RowAction action, StockOutRowResponse row) {
        switch (action) {
            case VIEW -> openDetail(row.maPhieu());
            case PRINT -> runDetail(row.maPhieu(), detail -> StockOutPrinter.print(this, detail));
            case EDIT -> editNote(row.maPhieu());
            case PROCESS -> issue(row.maPhieu());
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

    private void showEditor(StockOutDetailResponse existing) {
        StockOutReceiptDialog dlgReceipt = new StockOutReceiptDialog(owner(), service, itemService, session, existing);
        dlgReceipt.setVisible(true);
        dlgReceipt.savedReceipt().ifPresent(saved -> {
            reload();
            showInfo("Đã lưu phiếu " + saved.maPhieu() + " (" + saved.trangThai().label() + ").");
        });
    }

    private void openDetail(String maPhieu) {
        runDetail(maPhieu, detail -> {
            StockOutDetailDialog dlgDetail = new StockOutDetailDialog(owner(), service, session, detail, this::openEditor);
            dlgDetail.setVisible(true);
            if (dlgDetail.changed()) {
                reload();
            }
        });
    }

    /** Phiếu đã xuất: chỉ sửa ghi chú, không đổi lô/số lượng đã trừ tồn. */
    private void editNote(String maPhieu) {
        runDetail(maPhieu, detail -> {
            Object input = JOptionPane.showInputDialog(this,
                    "Phiếu đã xuất kho nên chỉ sửa được ghi chú (tồn kho giữ nguyên).",
                    "Sửa ghi chú " + maPhieu, JOptionPane.PLAIN_MESSAGE, null, null,
                    detail.ghiChu() == null ? "" : detail.ghiChu());
            if (input == null) {
                return;
            }
            InventoryActor actor = session.get();
            runWrite(() -> service.updateNote(actor, maPhieu, input.toString()), "Đã cập nhật ghi chú phiếu " + maPhieu + ".");
        });
    }

    private void issue(String maPhieu) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Xác nhận xuất kho phiếu " + maPhieu + "?\nSố lượng trên phiếu sẽ bị trừ khỏi tồn của từng lô.",
                "Xuất kho", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        InventoryActor actor = session.get();
        runWrite(() -> service.complete(actor, maPhieu), "Đã xuất kho phiếu " + maPhieu + ".");
    }

    private void runDetail(String maPhieu, Consumer<StockOutDetailResponse> onLoaded) {
        Set<Role> roles = session.get().roles();
        new SwingWorker<StockOutDetailResponse, Void>() {
            @Override
            protected StockOutDetailResponse doInBackground() {
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

    private void runWrite(Supplier<StockOutDetailResponse> call, String successMessage) {
        new SwingWorker<StockOutDetailResponse, Void>() {
            @Override
            protected StockOutDetailResponse doInBackground() {
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
        JOptionPane.showMessageDialog(this, message, "Xuất kho", JOptionPane.WARNING_MESSAGE);
    }

    /** Xuất danh sách phiếu đang lọc ra file Excel .xlsx có định dạng. */
    private void exportExcel() {
        if (filteredRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách phiếu xuất");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("phieu-xuat-kho.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<StockOutRowResponse> snapshot = filteredRows;
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
                    showInfo("Đã xuất " + snapshot.size() + " phiếu xuất ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<StockOutRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã phiếu", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày xuất", 13, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Lý do xuất", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người nhận", 28, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người xuất", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Giá trị (đ)", 16, XlsxExport.Kind.MONEY),
                new XlsxExport.Column("Trạng thái", 15, XlsxExport.Kind.CENTER));
        List<List<Object>> data = new ArrayList<>();
        int stt = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (StockOutRowResponse r : rows) {
            // Giá vốn chỉ được chốt khi xuất kho; phiếu chờ/hủy để trống thay vì ghi 0.
            data.add(java.util.Arrays.asList(stt++, r.maPhieu(), r.ngayXuat(), r.lyDo().label(), r.nguoiNhan(),
                    r.nguoiLap(), r.trangThai() == VoucherStatus.DA_HOAN_THANH ? r.tongTien() : null,
                    r.trangThai().label()));
            if (r.trangThai() == VoucherStatus.DA_HOAN_THANH) {
                total = total.add(r.tongTien());
            }
        }
        XlsxExport.write(target, "Phieu xuat kho", "DANH SÁCH PHIẾU XUẤT KHO",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(LocalDateTime.now()) + " · "
                        + rows.size() + " phiếu · Tổng giá trị đã xuất kho: " + InventoryFormat.money(total),
                columns, data);
    }

    // ------------------------------------------------------------------- table

    private static final class ReceiptTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<StockOutRowResponse> rows = List.of();
        private int offset;

        void setPage(List<StockOutRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        StockOutRowResponse rowAt(int row) {
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
            StockOutRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maPhieu();
                case 2 -> InventoryFormat.date(r.ngayXuat());
                case 3 -> r.lyDo().shortLabel();
                case 4 -> r.nguoiNhan();
                case 5 -> r.nguoiLap();
                default -> r.trangThai();
            };
        }
    }
}
