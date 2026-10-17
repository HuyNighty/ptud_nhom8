package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.CellRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.LinkActionCell;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
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
 * Màn hình "Kiểm kê" (UC28): thống kê, lọc, danh sách đợt; tạo đợt (chụp tồn sổ sách), cập nhật số đếm,
 * hoàn tất bởi người khác người lập (điều chỉnh tồn theo chênh lệch) và hủy đợt.
 */
public final class StocktakePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 8;
    static final String PENDING_LABEL = "Đang kiểm kê";
    private static final String[] COLUMNS = {"STT", "Mã đợt", "Ngày kiểm kê", "Người kiểm kê", "Tổng mặt hàng",
            "Chênh lệch thực tế", "Trạng thái", "Thao tác"};
    private static final int COL_STATUS = 6;
    private static final int COL_ACTIONS = 7;
    private static final Color OPEN_FILL = new Color(0xFFF6D6);
    private static final Color OPEN_BORDER = new Color(0xF1DB8E);
    private static final Color MATCH_FILL = new Color(0xE3F5EA);
    private static final Color MATCH_BORDER = new Color(0xA9DDBC);
    private static final Color DIFF_FILL = new Color(0xFFE9DC);
    private static final Color DIFF_BORDER = new Color(0xF2B48F);

    /** Bộ lọc "Trạng thái" (gồm cả kết quả khớp/lệch như các thẻ thống kê). */
    enum StatusFilter {
        ALL("Trạng thái", r -> true),
        OPEN(PENDING_LABEL, r -> r.trangThai() == VoucherStatus.CHO_XU_LY),
        MATCHED("Đã hoàn thành — khớp", r -> r.trangThai() == VoucherStatus.DA_HOAN_THANH && !r.lech()),
        MISMATCHED("Đã hoàn thành — có lệch", r -> r.trangThai() == VoucherStatus.DA_HOAN_THANH && r.lech()),
        CANCELLED("Đã hủy", r -> r.trangThai() == VoucherStatus.DA_HUY);

        final String label;
        final transient Predicate<StocktakeRowResponse> test;

        StatusFilter(String label, Predicate<StocktakeRowResponse> test) {
            this.label = label;
            this.test = test;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final transient StocktakeService service;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotalValue = statValue(TEXT);
    private final JLabel lblOpenValue = statValue(TEXT);
    private final JLabel lblMatchValue = statValue(TEXT);
    private final JLabel lblDiffValue = statValue(TEXT);

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm mã đợt, người kiểm...", 20);
    private final JToggleButton tglTabAll = createTab("Tất cả");
    private final JToggleButton tglTabOpen = createTab("Đang thực hiện");
    private final JToggleButton tglTabDone = createTab("Đã hoàn thành");
    private final JComboBox<StatusFilter> cmbStatus = new JComboBox<>(StatusFilter.values());
    private final JComboBox<TimeRange> cmbTimeRange = new JComboBox<>(TimeRange.values());
    private final JButton btnCreate = createButton("Tạo đợt kiểm kê", ButtonKind.PRIMARY);
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final StocktakeTableModel tableModel = new StocktakeTableModel();
    private final JTable tblStocktakes = new JTable(tableModel);
    private final LinkActionCell actionCell = new LinkActionCell("Cập nhật", COL_STATUS);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<StocktakeRowResponse> allRows = List.of();
    private transient List<StocktakeRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(StocktakeSummaryResponse summary, List<StocktakeRowResponse> rows) {
    }

    public StocktakePanel(StocktakeService service, Supplier<InventoryActor> session) {
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
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, buildListCard());

        JScrollPane scrPage = new JScrollPane(pnlPage);
        scrPage.setBorder(BorderFactory.createEmptyBorder());
        scrPage.getViewport().setBackground(BACKGROUND);
        scrPage.getVerticalScrollBar().setUnitIncrement(18);
        add(scrPage, BorderLayout.CENTER);

        wireEvents();
        reload();
    }

    /** Tải lại thống kê và danh sách đợt; giữ nguyên bộ lọc đang chọn. */
    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải đợt kiểm kê…");
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
        JLabel lblTitle = new JLabel("Kiểm kê");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Quản lý các đợt kiểm kê, đối soát thực tế so với sổ sách, ghi nhận hao hụt/lệch "
                + "và cập nhật số lượng tồn kho.");
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
        pnlStats.add(statCard("Tổng đợt kiểm kê", lblTotalValue, "Trong năm nay"));
        pnlStats.add(statCard("Đang kiểm kê", lblOpenValue, "Đợt kiểm kê dở dang").tint(OPEN_FILL, OPEN_BORDER));
        pnlStats.add(statCard("Khớp dữ liệu", lblMatchValue, "Số liệu chính xác").tint(MATCH_FILL, MATCH_BORDER));
        pnlStats.add(statCard("Lệch tồn kho", lblDiffValue, "Số đợt có lệch tồn").tint(DIFF_FILL, DIFF_BORDER));
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static Card statCard(String title, JLabel lblValue, String caption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.BOLD, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(6));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    /** Một khung chứa: tìm kiếm + tab, bộ lọc + nút, tiêu đề, bảng và phân trang (theo Figma). */
    private JComponent buildListCard() {
        Card pnlCard = new Card(new BorderLayout(), 0);

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(340, 44));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);
        JPanel pnlTabs = createTabBar(tglTabAll, tglTabAll, tglTabOpen, tglTabDone);
        JPanel pnlRow1 = new JPanel(new BorderLayout(24, 0));
        pnlRow1.setOpaque(false);
        pnlRow1.add(pnlSearch, BorderLayout.WEST);
        pnlRow1.add(pnlTabs, BorderLayout.CENTER);

        styleCombo(cmbStatus);
        cmbStatus.setPreferredSize(new Dimension(230, 44));
        styleCombo(cmbTimeRange);
        cmbTimeRange.setPreferredSize(new Dimension(190, 44));
        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlFilters.setOpaque(false);
        pnlFilters.add(cmbStatus);
        pnlFilters.add(Box.createHorizontalStrut(14));
        pnlFilters.add(cmbTimeRange);
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        btnExport.setIcon(new GlyphIcon(MenuGlyph.UPLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCreate);
        pnlButtons.add(btnExport);
        JPanel pnlRow2 = new JPanel(new BorderLayout());
        pnlRow2.setOpaque(false);
        pnlRow2.add(pnlFilters, BorderLayout.WEST);
        pnlRow2.add(pnlButtons, BorderLayout.EAST);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 16));
        pnlTop.setOpaque(false);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        pnlTop.add(pnlRow1, BorderLayout.NORTH);
        pnlTop.add(pnlRow2, BorderLayout.CENTER);

        JLabel lblListTitle = new JLabel("DANH SÁCH ĐỢT KIỂM KÊ KHO");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        lblListTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), BorderFactory.createEmptyBorder(18, 22, 16, 22)));

        tblStocktakes.setFont(font(Font.PLAIN, 15));
        tblStocktakes.setForeground(TEXT);
        tblStocktakes.setRowHeight(60);
        tblStocktakes.setShowVerticalLines(false);
        tblStocktakes.setGridColor(BORDER);
        tblStocktakes.setRowSelectionAllowed(false);
        tblStocktakes.setFocusable(false);
        tblStocktakes.setBackground(SURFACE);
        tblStocktakes.setDefaultRenderer(Object.class, new CellRenderer());
        tblStocktakes.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer(PENDING_LABEL));
        tblStocktakes.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {55, 90, 130, 160, 145, 230, 165, 190};
        for (int i = 0; i < widths.length; i++) {
            tblStocktakes.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhStocktakes = tblStocktakes.getTableHeader();
        tbhStocktakes.setReorderingAllowed(false);
        tbhStocktakes.setResizingAllowed(false);
        tbhStocktakes.setPreferredSize(new Dimension(10, 52));
        tbhStocktakes.setDefaultRenderer(new HeaderRenderer());

        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setOpaque(false);
        pnlTable.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        pnlTable.add(lblListTitle, BorderLayout.NORTH);
        JPanel pnlGrid = new JPanel(new BorderLayout());
        pnlGrid.setOpaque(false);
        pnlGrid.add(tbhStocktakes, BorderLayout.NORTH);
        pnlGrid.add(tblStocktakes, BorderLayout.CENTER);
        pnlTable.add(pnlGrid, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), BorderFactory.createEmptyBorder(14, 22, 14, 22)));
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        pnlPager.onPageSelected(page -> {
            currentPage = page;
            showPage();
        });
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);

        pnlCard.add(pnlTop, BorderLayout.NORTH);
        pnlCard.add(pnlTable, BorderLayout.CENTER);
        pnlCard.add(pnlFooter, BorderLayout.SOUTH);
        return pnlCard;
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
        cmbStatus.addActionListener(e -> applyFilters());
        cmbTimeRange.addActionListener(e -> applyFilters());
        for (JToggleButton tglTab : new JToggleButton[] {tglTabAll, tglTabOpen, tglTabDone}) {
            tglTab.addActionListener(e -> applyFilters());
        }
        btnCreate.addActionListener(e -> createStocktake());
        btnExport.addActionListener(e -> exportExcel());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblStocktakes.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                StocktakeRowResponse r = tableModel.rowAt(row);
                if (tblStocktakes.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    RowAction action = hit(row, e.getX(), r.trangThai());
                    if (action != null) {
                        perform(action, r);
                    }
                } else if (e.getClickCount() == 2) {
                    perform(RowAction.VIEW, r);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tblStocktakes.rowAtPoint(e.getPoint());
                boolean hand = row >= 0 && tblStocktakes.columnAtPoint(e.getPoint()) == COL_ACTIONS
                        && hit(row, e.getX(), tableModel.rowAt(row).trangThai()) != null;
                tblStocktakes.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblStocktakes.addMouseListener(clicks);
        tblStocktakes.addMouseMotionListener(clicks);
    }

    private RowAction hit(int row, int x, VoucherStatus status) {
        Rectangle cell = tblStocktakes.getCellRect(row, COL_ACTIONS, false);
        return actionCell.hitTest(status, x - cell.x, cell.width, tblStocktakes.getFontMetrics(actionCell.getFont()));
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(StocktakeSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongTrongNam()));
        lblOpenValue.setText(String.valueOf(s.dangKiemKe()));
        lblMatchValue.setText(String.valueOf(s.khop()));
        lblDiffValue.setText(String.valueOf(s.lech()));
    }

    /** "Khớp" / "Lệch 3 (Thiếu 2, Thừa 1)" / "Đang đối soát (đã đếm 5/12 lô)" / "— (Đã hủy)". */
    static String differenceText(StocktakeRowResponse r) {
        return switch (r.trangThai()) {
            case CHO_XU_LY -> "Đang đối soát (đã đếm " + r.daDem() + "/" + r.soLo() + " lô)";
            case DA_HUY -> "— (Đã hủy)";
            case DA_HOAN_THANH -> differenceText(r.thieu(), r.thua());
        };
    }

    static String differenceText(int thieu, int thua) {
        if (thieu == 0 && thua == 0) {
            return "Khớp";
        }
        List<String> parts = new ArrayList<>();
        if (thieu > 0) {
            parts.add("Thiếu " + thieu);
        }
        if (thua > 0) {
            parts.add("Thừa " + thua);
        }
        return "Lệch " + (thieu + thua) + " (" + String.join(", ", parts) + ")";
    }

    static String statusLabel(VoucherStatus status) {
        return status == VoucherStatus.CHO_XU_LY ? PENDING_LABEL : status.label();
    }

    private void applyFilters() {
        searchDelay.stop();
        LocalDate today = LocalDate.now();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        StatusFilter status = (StatusFilter) cmbStatus.getSelectedItem();
        TimeRange range = (TimeRange) cmbTimeRange.getSelectedItem();
        List<StocktakeRowResponse> matching = new ArrayList<>();
        int open = 0;
        int done = 0;
        for (StocktakeRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maKiemKe().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.nguoiKiemKe().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesStatus = status == null || status.test.test(r);
            boolean matchesTime = range == null || range.contains(r.ngayKiemKe(), today);
            if (matchesKeyword && matchesStatus && matchesTime) {
                matching.add(r);
                open += r.trangThai() == VoucherStatus.CHO_XU_LY ? 1 : 0;
                done += r.trangThai() == VoucherStatus.DA_HOAN_THANH ? 1 : 0;
            }
        }
        tglTabAll.setText("Tất cả (" + matching.size() + ")");
        tglTabOpen.setText("Đang thực hiện (" + open + ")");
        tglTabDone.setText("Đã hoàn thành (" + done + ")");
        VoucherStatus tab = tglTabOpen.isSelected() ? VoucherStatus.CHO_XU_LY
                : tglTabDone.isSelected() ? VoucherStatus.DA_HOAN_THANH : null;
        filteredRows = tab == null ? List.copyOf(matching) : matching.stream().filter(r -> r.trangThai() == tab).toList();
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
                ? (allRows.isEmpty() ? "Chưa có đợt kiểm kê nào — bấm \"Tạo đợt kiểm kê\" để bắt đầu."
                        : "Không có đợt kiểm kê nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " đợt kiểm kê");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private void perform(RowAction action, StocktakeRowResponse row) {
        switch (action) {
            case VIEW -> openDetail(row.maKiemKe());
            case PRINT -> runDetail(row.maKiemKe(), d -> StocktakePrinter.print(this, d));
            case EDIT -> runDetail(row.maKiemKe(), this::editHeader);
            case PROCESS -> openCount(row.maKiemKe());
        }
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void createStocktake() {
        StocktakeHeaderDialog dlgCreate = new StocktakeHeaderDialog(owner(), service, session, null);
        dlgCreate.setVisible(true);
        dlgCreate.saved().ifPresent(created -> {
            reload();
            showInfo("Đã tạo đợt " + created.maKiemKe() + " — chụp tồn sổ sách của " + created.dong().size()
                    + " lô. Bấm \"Cập nhật\" để nhập số đếm.");
        });
    }

    private void editHeader(StocktakeDetailResponse detail) {
        StocktakeHeaderDialog dlgEdit = new StocktakeHeaderDialog(owner(), service, session, detail);
        dlgEdit.setVisible(true);
        dlgEdit.saved().ifPresent(saved -> {
            reload();
            showInfo("Đã cập nhật đợt " + saved.maKiemKe() + ".");
        });
    }

    void openCount(String maKiemKe) {
        runDetail(maKiemKe, detail -> {
            StocktakeCountDialog dlgCount = new StocktakeCountDialog(owner(), service, session, detail);
            dlgCount.setVisible(true);
            if (dlgCount.changed()) {
                reload();
            }
        });
    }

    private void openDetail(String maKiemKe) {
        runDetail(maKiemKe, detail -> {
            StocktakeDetailDialog dlgDetail = new StocktakeDetailDialog(owner(), service, session, detail, this::openCount);
            dlgDetail.setVisible(true);
            if (dlgDetail.changed()) {
                reload();
            }
        });
    }

    private void runDetail(String maKiemKe, Consumer<StocktakeDetailResponse> onLoaded) {
        Set<Role> roles = session.get().roles();
        new SwingWorker<StocktakeDetailResponse, Void>() {
            @Override
            protected StocktakeDetailResponse doInBackground() {
                return service.getDetail(roles, maKiemKe);
            }

            @Override
            protected void done() {
                try {
                    onLoaded.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(StocktakePanel.this, InventoryItemPanel.describe(e.getCause()),
                            "Kiểm kê", JOptionPane.WARNING_MESSAGE);
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

    private void exportExcel() {
        if (filteredRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách đợt kiểm kê");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("dot-kiem-ke.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<StocktakeRowResponse> snapshot = filteredRows;
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
                    showInfo("Đã xuất " + snapshot.size() + " đợt kiểm kê ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    JOptionPane.showMessageDialog(StocktakePanel.this, "Không ghi được file: " + e.getCause().getMessage(),
                            "Xuất Excel", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<StocktakeRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã đợt", 10, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày kiểm kê", 14, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Phạm vi", 14, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người kiểm kê", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Tổng mặt hàng", 14, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Số lô", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Thiếu", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Thừa", 9, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Chênh lệch thực tế", 28, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Trạng thái", 16, XlsxExport.Kind.CENTER));
        List<List<Object>> data = new ArrayList<>();
        int stt = 1;
        for (StocktakeRowResponse r : rows) {
            boolean done = r.trangThai() == VoucherStatus.DA_HOAN_THANH;
            data.add(java.util.Arrays.asList(stt++, r.maKiemKe(), r.ngayKiemKe(), r.phamVi().label(), r.nguoiKiemKe(),
                    r.tongMatHang(), r.soLo(), done ? r.thieu() : null, done ? r.thua() : null, differenceText(r),
                    statusLabel(r.trangThai())));
        }
        XlsxExport.write(target, "Dot kiem ke", "DANH SÁCH ĐỢT KIỂM KÊ KHO",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(LocalDateTime.now()) + " · " + rows.size() + " đợt",
                columns, data);
    }

    // ------------------------------------------------------------------- table

    private static final class StocktakeTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<StocktakeRowResponse> rows = List.of();
        private int offset;

        void setPage(List<StocktakeRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        StocktakeRowResponse rowAt(int row) {
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
            StocktakeRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maKiemKe();
                case 2 -> InventoryFormat.date(r.ngayKiemKe());
                case 3 -> r.nguoiKiemKe();
                case 4 -> r.tongMatHang();
                case 5 -> differenceText(r);
                default -> r.trangThai();
            };
        }
    }
}
