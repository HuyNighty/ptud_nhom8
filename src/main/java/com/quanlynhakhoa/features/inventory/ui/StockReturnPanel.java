package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockReturnService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ButtonActionCell;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.CellRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.DotStatusRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.RowAction;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ScrollablePage;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.TimeRange;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

/**
 * Màn hình "Hoàn trả kho" (UC32): thống kê, lọc theo lý do / thời gian / trạng thái, danh sách phiếu,
 * lập phiếu từ phiếu xuất gốc, kiểm duyệt (nhận lại kho) và hủy phiếu.
 * Mọi truy vấn chạy trong {@link SwingWorker}; người thao tác lấy từ phiên qua {@code session}.
 */
public final class StockReturnPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 8;
    static final String PENDING_LABEL = "Chờ kiểm duyệt";
    private static final String[] COLUMNS = {"STT", "Mã phiếu", "Ngày hoàn trả", "Nguồn hoàn trả", "Người hoàn trả",
            "Người nhận", "Trạng thái", "Thao tác"};
    private static final int COL_STATUS = 6;
    private static final int COL_ACTIONS = 7;
    private static final String ALL_REASONS = "Tất cả lý do";
    private static final Color PENDING_FILL = new Color(0xFFF3E6);
    private static final Color PENDING_BORDER = new Color(0xF4D2A6);
    private static final Color PENDING_TEXT = new Color(0xC2600A);
    private static final Color DONE_FILL = new Color(0xEAF7EE);
    private static final Color DONE_BORDER = new Color(0xC3E5CC);
    private static final Color DONE_TEXT = new Color(0x1E6B34);

    private final transient StockReturnService service;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotalValue = statValue(TEXT);
    private final JLabel lblAmountValue = statValue(TEXT);
    private final JLabel lblPendingValue = statValue(ORANGE);
    private final JLabel lblCompletedValue = statValue(GREEN);

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm mã phiếu, người hoàn trả, phòng khám...", 24);
    private final JComboBox<Object> cmbReason = new JComboBox<>();
    private final JComboBox<TimeRange> cmbTimeRange = new JComboBox<>(TimeRange.values());
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final JButton btnCreate = createButton("Tạo phiếu hoàn trả", ButtonKind.PRIMARY);
    private final JToggleButton tglTabAll = createTab("Tất cả");
    private final JToggleButton tglTabPending = createTab(PENDING_LABEL);
    private final JToggleButton tglTabCompleted = createTab("Đã hoàn thành");
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final ReturnTableModel tableModel = new ReturnTableModel();
    private final JTable tblReturns = new JTable(tableModel);
    private final ButtonActionCell actionCell = new ButtonActionCell("Duyệt", COL_STATUS);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<StockReturnRowResponse> allRows = List.of();
    private transient List<StockReturnRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(StockReturnSummaryResponse summary, List<StockReturnRowResponse> rows) {
    }

    public StockReturnPanel(StockReturnService service, Supplier<InventoryActor> session) {
        this.service = Objects.requireNonNull(service);
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
        pnlPage.add(Box.createVerticalStrut(20));
        addFullWidth(pnlPage, buildToolbar());
        pnlPage.add(Box.createVerticalStrut(16));
        addFullWidth(pnlPage, createTabBar(tglTabAll, tglTabAll, tglTabPending, tglTabCompleted));
        pnlPage.add(Box.createVerticalStrut(18));
        addFullWidth(pnlPage, buildListCard());
        pnlPage.add(Box.createVerticalStrut(14));
        addFullWidth(pnlPage, buildFooter());

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
        lblPageInfo.setText("Đang tải phiếu hoàn trả…");
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
        JLabel lblTitle = new JLabel("Hoàn trả kho");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel(
                "Quản lý các phiếu hoàn trả vật tư, thuốc từ các phòng khám, bác sĩ hoặc hoàn trả lại nhà cung cấp.");
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
        pnlStats.add(statCard("Tổng phiếu hoàn trả", lblTotalValue, "Phiếu hoàn trả", TEXT, null));
        pnlStats.add(statCard("Giá trị hoàn trả", lblAmountValue, "Tổng giá trị đã nhận lại", TEXT, null));
        Card pnlPending = statCard("Chờ kiểm duyệt", lblPendingValue, "Phiếu chờ duyệt", PENDING_TEXT, PENDING_FILL);
        pnlPending.tint(PENDING_FILL, PENDING_BORDER);
        Card pnlDone = statCard("Đã hoàn thành", lblCompletedValue, "Đã nhận lại kho", DONE_TEXT, DONE_FILL);
        pnlDone.tint(DONE_FILL, DONE_BORDER);
        pnlStats.add(pnlPending);
        pnlStats.add(pnlDone);
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static Card statCard(String title, JLabel lblValue, String caption, Color captionColor, Color fill) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(fill == null ? TEXT : captionColor);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(6));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    /** Bộ lọc có nhãn phía trên: Tìm kiếm · Lý do · Thời gian · Xuất Excel · Tạo phiếu hoàn trả. */
    private JComponent buildToolbar() {
        JPanel pnlToolbar = new JPanel(new GridBagLayout());
        pnlToolbar.setOpaque(false);

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(320, 44));
        pnlSearch.setMinimumSize(new Dimension(180, 44));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        cmbReason.addItem(ALL_REASONS);
        Arrays.stream(ReturnReason.values()).forEach(cmbReason::addItem);
        styleCombo(cmbReason);
        cmbReason.setPreferredSize(new Dimension(200, 44));
        styleCombo(cmbTimeRange);
        cmbTimeRange.setRenderer(new TimeRangeRenderer(cmbTimeRange.getRenderer()));
        cmbTimeRange.setPreferredSize(new Dimension(200, 44));

        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));

        addLabeled(pnlToolbar, 0, "Tìm kiếm", pnlSearch, 1.0);
        addLabeled(pnlToolbar, 1, "Lý do", cmbReason, 0);
        addLabeled(pnlToolbar, 2, "Thời gian", cmbTimeRange, 0);
        addLabeled(pnlToolbar, 3, null, btnExport, 0);
        addLabeled(pnlToolbar, 4, null, btnCreate, 0);
        return pnlToolbar;
    }

    private static void addLabeled(JPanel target, int column, String label, JComponent field, double weight) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = column;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, column == 0 ? 0 : 16, 8, 0);
        JLabel lblField = new JLabel(label == null ? " " : label);
        lblField.setFont(font(Font.PLAIN, 16));
        lblField.setForeground(TEXT);
        target.add(lblField, gc);
        gc.gridy = 1;
        gc.insets = new Insets(0, column == 0 ? 0 : 16, 0, 0);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = weight;
        target.add(field, gc);
    }

    private JComponent buildListCard() {
        Card pnlListCard = new Card(new BorderLayout(), 0);
        JLabel lblListTitle = new JLabel("DANH SÁCH PHIẾU HOÀN TRẢ KHO");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        lblListTitle.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        pnlListCard.add(lblListTitle, BorderLayout.NORTH);

        tblReturns.setFont(font(Font.PLAIN, 15));
        tblReturns.setForeground(TEXT);
        tblReturns.setRowHeight(60);
        tblReturns.setShowVerticalLines(false);
        tblReturns.setGridColor(BORDER);
        tblReturns.setRowSelectionAllowed(false);
        tblReturns.setFocusable(false);
        tblReturns.setBackground(SURFACE);
        tblReturns.setDefaultRenderer(Object.class, new CellRenderer());
        tblReturns.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new DotStatusRenderer(PENDING_LABEL));
        tblReturns.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {55, 95, 130, 230, 170, 140, 160, 200};
        for (int i = 0; i < widths.length; i++) {
            tblReturns.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhReturns = tblReturns.getTableHeader();
        tbhReturns.setReorderingAllowed(false);
        tbhReturns.setResizingAllowed(false);
        tbhReturns.setPreferredSize(new Dimension(10, 52));
        tbhReturns.setDefaultRenderer(new HeaderRenderer());

        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setOpaque(false);
        pnlTable.setBorder(BorderFactory.createEmptyBorder(0, 1, 8, 1));
        pnlTable.add(tbhReturns, BorderLayout.NORTH);
        pnlTable.add(tblReturns, BorderLayout.CENTER);
        pnlListCard.add(pnlTable, BorderLayout.CENTER);
        return pnlListCard;
    }

    private JComponent buildFooter() {
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        pnlPager.onPageSelected(page -> {
            currentPage = page;
            showPage();
        });
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);
        return pnlFooter;
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
        cmbReason.addActionListener(e -> applyFilters());
        cmbTimeRange.addActionListener(e -> applyFilters());
        for (JToggleButton tglTab : new JToggleButton[] {tglTabAll, tglTabPending, tglTabCompleted}) {
            tglTab.addActionListener(e -> applyFilters());
        }
        btnCreate.addActionListener(e -> openEditor(null));
        btnExport.addActionListener(e -> exportExcel());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblReturns.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                StockReturnRowResponse receipt = tableModel.rowAt(row);
                if (tblReturns.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    Rectangle cell = tblReturns.getCellRect(row, COL_ACTIONS, false);
                    RowAction action = actionCell.hitTest(receipt.trangThai(), e.getX() - cell.x,
                            tblReturns.getFontMetrics(actionCell.getFont()));
                    if (action != null) {
                        perform(action, receipt);
                    }
                } else if (e.getClickCount() == 2) {
                    perform(RowAction.VIEW, receipt);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tblReturns.rowAtPoint(e.getPoint());
                boolean hand = false;
                if (row >= 0 && tblReturns.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    Rectangle cell = tblReturns.getCellRect(row, COL_ACTIONS, false);
                    hand = actionCell.hitTest(tableModel.rowAt(row).trangThai(), e.getX() - cell.x,
                            tblReturns.getFontMetrics(actionCell.getFont())) != null;
                }
                tblReturns.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblReturns.addMouseListener(clicks);
        tblReturns.addMouseMotionListener(clicks);
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(StockReturnSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongPhieu()));
        lblAmountValue.setText(InventoryFormat.money(s.giaTriHoanTra()));
        lblPendingValue.setText(String.valueOf(s.soChoDuyet()));
        lblCompletedValue.setText(String.valueOf(s.soHoanThanh()));
    }

    /** "Phòng khám 01 (Dư thừa)" — lý do "Khác" không ghi kèm. */
    static String sourceText(String nguon, ReturnReason lyDo) {
        return lyDo == ReturnReason.KHAC ? nguon : nguon + " (" + lyDo.label() + ")";
    }

    private void applyFilters() {
        searchDelay.stop();
        LocalDate today = LocalDate.now();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        ReturnReason reason = cmbReason.getSelectedItem() instanceof ReturnReason r ? r : null;
        TimeRange range = (TimeRange) cmbTimeRange.getSelectedItem();
        List<StockReturnRowResponse> matching = new ArrayList<>();
        int pending = 0;
        int completed = 0;
        for (StockReturnRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maPhieu().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.maPhieuXuat().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.nguoiHoanTra().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.nguonHoanTra().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesReason = reason == null || r.lyDo() == reason;
            boolean matchesTime = range == null || range.contains(r.ngayHoanTra(), today);
            if (matchesKeyword && matchesReason && matchesTime) {
                matching.add(r);
                pending += r.trangThai() == VoucherStatus.CHO_XU_LY ? 1 : 0;
                completed += r.trangThai() == VoucherStatus.DA_HOAN_THANH ? 1 : 0;
            }
        }
        tglTabAll.setText("Tất cả (" + matching.size() + ")");
        tglTabPending.setText(PENDING_LABEL + " (" + pending + ")");
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
                ? (allRows.isEmpty() ? "Chưa có phiếu hoàn trả nào — bấm \"Tạo phiếu hoàn trả\" để bắt đầu."
                        : "Không có phiếu hoàn trả nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " phiếu hoàn trả");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private void perform(RowAction action, StockReturnRowResponse row) {
        switch (action) {
            case VIEW -> openDetail(row.maPhieu());
            case PRINT -> runDetail(row.maPhieu(), detail -> StockReturnPrinter.print(this, detail));
            case EDIT -> editNote(row.maPhieu());
            case PROCESS -> approve(row.maPhieu());
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

    private void showEditor(StockReturnDetailResponse existing) {
        StockReturnReceiptDialog dlgReturn = new StockReturnReceiptDialog(owner(), service, session, existing);
        dlgReturn.setVisible(true);
        dlgReturn.savedReceipt().ifPresent(saved -> {
            reload();
            showInfo("Đã lưu phiếu " + saved.maPhieu() + " (" + PENDING_LABEL.toLowerCase(Locale.ROOT) + ").");
        });
    }

    private void openDetail(String maPhieu) {
        runDetail(maPhieu, detail -> {
            StockReturnDetailDialog dlgDetail = new StockReturnDetailDialog(owner(), service, session, detail,
                    this::openEditor);
            dlgDetail.setVisible(true);
            if (dlgDetail.changed()) {
                reload();
            }
        });
    }

    /** Phiếu đã duyệt: chỉ sửa ghi chú, không đổi số lượng đã nhận lại kho. */
    private void editNote(String maPhieu) {
        runDetail(maPhieu, detail -> {
            Object input = JOptionPane.showInputDialog(this,
                    "Phiếu đã được duyệt nên chỉ sửa được ghi chú (tồn kho giữ nguyên).",
                    "Sửa ghi chú " + maPhieu, JOptionPane.PLAIN_MESSAGE, null, null,
                    detail.ghiChu() == null ? "" : detail.ghiChu());
            if (input == null) {
                return;
            }
            InventoryActor actor = session.get();
            runWrite(() -> service.updateNote(actor, maPhieu, input.toString()),
                    "Đã cập nhật ghi chú phiếu " + maPhieu + ".");
        });
    }

    private void approve(String maPhieu) {
        runDetail(maPhieu, detail -> {
            String effect = detail.lyDo().restock()
                    ? "Số lượng trên phiếu sẽ được cộng lại vào tồn của đúng lô đã xuất."
                    : "Hàng trả vì lỗi: chỉ ghi nhận, KHÔNG cộng lại vào tồn khả dụng.";
            int answer = JOptionPane.showConfirmDialog(this,
                    "Duyệt phiếu hoàn trả " + maPhieu + " (từ phiếu xuất " + detail.maPhieuXuat() + ")?\n" + effect,
                    "Kiểm duyệt hoàn trả", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
            InventoryActor actor = session.get();
            runWrite(() -> service.approve(actor, maPhieu), "Đã duyệt phiếu " + maPhieu + " — hàng đã nhận lại kho.");
        });
    }

    private void runDetail(String maPhieu, Consumer<StockReturnDetailResponse> onLoaded) {
        Set<Role> roles = session.get().roles();
        new SwingWorker<StockReturnDetailResponse, Void>() {
            @Override
            protected StockReturnDetailResponse doInBackground() {
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

    private void runWrite(Supplier<StockReturnDetailResponse> call, String successMessage) {
        new SwingWorker<StockReturnDetailResponse, Void>() {
            @Override
            protected StockReturnDetailResponse doInBackground() {
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
        JOptionPane.showMessageDialog(this, message, "Hoàn trả kho", JOptionPane.WARNING_MESSAGE);
    }

    /** Xuất danh sách phiếu đang lọc ra file Excel .xlsx có định dạng. */
    private void exportExcel() {
        if (filteredRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách phiếu hoàn trả");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("phieu-hoan-tra.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<StockReturnRowResponse> snapshot = filteredRows;
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
                    showInfo("Đã xuất " + snapshot.size() + " phiếu hoàn trả ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<StockReturnRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã phiếu", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày hoàn trả", 14, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Phiếu xuất gốc", 13, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Nguồn hoàn trả", 26, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Lý do", 12, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người hoàn trả", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người nhận", 20, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Giá trị (đ)", 15, XlsxExport.Kind.MONEY),
                new XlsxExport.Column("Trạng thái", 16, XlsxExport.Kind.CENTER));
        List<List<Object>> data = new ArrayList<>();
        int stt = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (StockReturnRowResponse r : rows) {
            data.add(Arrays.asList(stt++, r.maPhieu(), r.ngayHoanTra(), r.maPhieuXuat(), r.nguonHoanTra(),
                    r.lyDo().label(), r.nguoiHoanTra(), r.nguoiNhan(), r.tongTien(), statusLabel(r.trangThai())));
            if (r.trangThai() == VoucherStatus.DA_HOAN_THANH) {
                total = total.add(r.tongTien());
            }
        }
        XlsxExport.write(target, "Phieu hoan tra", "DANH SÁCH PHIẾU HOÀN TRẢ KHO",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(LocalDateTime.now()) + " · "
                        + rows.size() + " phiếu · Tổng giá trị đã nhận lại kho: " + InventoryFormat.money(total),
                columns, data);
    }

    static String statusLabel(VoucherStatus status) {
        return status == VoucherStatus.CHO_XU_LY ? PENDING_LABEL : status.label();
    }

    // ------------------------------------------------------------------- table

    /** Mục "Tất cả" của bộ lọc thời gian hiển thị "Tất cả thời gian" vì nhãn "Thời gian" đã ở phía trên. */
    private static final class TimeRangeRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        private final transient javax.swing.ListCellRenderer<? super TimeRange> base;

        TimeRangeRenderer(javax.swing.ListCellRenderer<? super TimeRange> base) {
            this.base = base;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focused) {
            Component c = base.getListCellRendererComponent((JList<TimeRange>) list, (TimeRange) value, index,
                    selected, focused);
            if (value == TimeRange.ALL && c instanceof JLabel lbl) {
                lbl.setText("Tất cả thời gian");
            }
            return c;
        }
    }

    private static final class ReturnTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<StockReturnRowResponse> rows = List.of();
        private int offset;

        void setPage(List<StockReturnRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        StockReturnRowResponse rowAt(int row) {
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
            StockReturnRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maPhieu();
                case 2 -> InventoryFormat.date(r.ngayHoanTra());
                case 3 -> sourceText(r.nguonHoanTra(), r.lyDo());
                case 4 -> r.nguoiHoanTra();
                case 5 -> r.nguoiNhan() == null ? "—" : r.nguoiNhan();
                default -> r.trangThai();
            };
        }
    }
}
