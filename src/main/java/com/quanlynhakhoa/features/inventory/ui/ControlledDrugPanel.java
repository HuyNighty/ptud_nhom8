package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.service.ControlledDrugService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.CellRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ScrollablePage;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Màn hình "Thuốc kiểm soát đặc biệt" (UC33): danh mục thuốc KSĐB theo nhóm (gây nghiện, hướng thần, tiền chất),
 * tồn/hạn/trạng thái, sổ theo dõi (nhật ký sử dụng) và lập phiếu xuất (bắt buộc người nhận, phê duyệt kép).
 */
public final class ControlledDrugPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 8;
    private static final String[] COLUMNS = {"STT", "Mã thuốc", "Tên thuốc & Hoạt chất", "Phân loại", "ĐVT", "Tồn kho",
            "Hạn sử dụng", "Trạng thái", "Thao tác"};
    private static final int COL_NAME = 2;
    private static final int COL_STATUS = 7;
    private static final int COL_ACTIONS = 8;

    /** Bộ lọc "Trạng thái". */
    enum StatusFilter {
        ALL("Tất cả", r -> true),
        SAFE("An toàn", r -> r.mucDo() == NoticeSeverity.INFO),
        LOW("Sắp hết / thiếu hàng", ControlledDrugRowResponse::thieuHang),
        EXPIRY("Cận hạn / hết hạn", ControlledDrugRowResponse::canHan);

        final String label;
        final transient Predicate<ControlledDrugRowResponse> test;

        StatusFilter(String label, Predicate<ControlledDrugRowResponse> test) {
            this.label = label;
            this.test = test;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final transient ControlledDrugService service;
    private final transient StockOutService stockOutService;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblKinds = statValue();
    private final JLabel lblStock = statValue();
    private final JLabel lblExpiring = statValue();
    private final JLabel lblPending = statValue();

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm theo tên thuốc, hoạt chất, mã thuốc...", 24);
    private final JComboBox<StatusFilter> cmbStatus = new JComboBox<>(StatusFilter.values());
    private final JButton btnCreate = createButton("Tạo phiếu xuất", ButtonKind.PRIMARY);
    private final JButton btnLedger = createButton("Nhật ký sử dụng", ButtonKind.OUTLINE);
    private final JToggleButton tglAll = createTab("Tất cả");
    private final Map<ControlledDrugGroup, JToggleButton> groupTabs = new EnumMap<>(ControlledDrugGroup.class);
    private final JToggleButton tglUnclassified = createTab("Chưa phân loại");
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final DrugTableModel tableModel = new DrugTableModel();
    private final JTable tblDrugs = new JTable(tableModel);
    private final LinkPairCell actionCell = new LinkPairCell();
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<ControlledDrugRowResponse> allRows = List.of();
    private transient List<ControlledDrugRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(ControlledDrugSummaryResponse summary, List<ControlledDrugRowResponse> rows) {
    }

    public ControlledDrugPanel(ControlledDrugService service, StockOutService stockOutService,
                               InventoryItemService itemService, Supplier<InventoryActor> session) {
        this.service = Objects.requireNonNull(service);
        this.stockOutService = Objects.requireNonNull(stockOutService);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        for (ControlledDrugGroup g : ControlledDrugGroup.values()) {
            groupTabs.put(g, createTab(g.tabLabel()));
        }
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
        pnlPage.add(Box.createVerticalStrut(14));
        List<JToggleButton> tabs = new ArrayList<>();
        tabs.add(tglAll);
        tabs.addAll(groupTabs.values());
        tabs.add(tglUnclassified);
        addFullWidth(pnlPage, createTabBar(tglAll, tabs.toArray(JToggleButton[]::new)));
        pnlPage.add(Box.createVerticalStrut(18));
        JLabel lblListTitle = new JLabel("DANH SÁCH THUỐC KIỂM SOÁT ĐẶC BIỆT");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        JPanel pnlTitle = new JPanel(new BorderLayout());
        pnlTitle.setOpaque(false);
        pnlTitle.add(lblListTitle, BorderLayout.WEST);
        addFullWidth(pnlPage, pnlTitle);
        pnlPage.add(Box.createVerticalStrut(10));
        addFullWidth(pnlPage, buildTableCard());
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

    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải danh mục thuốc kiểm soát…");
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
        JLabel lblTitle = new JLabel("Thuốc kiểm soát đặc biệt");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Quản lý và giám sát chặt chẽ các loại thuốc thuộc danh mục kiểm soát đặc biệt "
                + "(gây nghiện, hướng thần, tiền chất) theo quy định.");
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
        pnlStats.add(statCard("Tổng loại thuốc", lblKinds, "Hoạt chất biệt dược", null));
        pnlStats.add(statCard("Tổng tồn kho", lblStock, "Đơn vị (Ống/Viên/Lọ)", null));
        pnlStats.add(statCard("Cận hạn & Hết hạn", lblExpiring, "Cần xử lý gấp", RED));
        pnlStats.add(statCard("Yêu cầu chờ duyệt", lblPending, "Cần phê duyệt kép", new Color(0xE5B300)));
        return pnlStats;
    }

    private static JLabel statValue() {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(TEXT);
        return lblValue;
    }

    private static Card statCard(String title, JLabel lblValue, String caption, Color dot) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.BOLD, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT);
        if (dot != null) {
            lblCaption.setIcon(new DotIcon(dot, 10));
            lblCaption.setIconTextGap(8);
        }
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(6));
        pnlStatCard.add(lblValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    private JComponent buildToolbar() {
        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(420, 44));
        pnlSearch.setMinimumSize(new Dimension(200, 44));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        styleCombo(cmbStatus);
        cmbStatus.setPreferredSize(new Dimension(230, 44));
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        btnLedger.setFont(font(Font.PLAIN, 15));

        JPanel pnlToolbar = new JPanel(new GridBagLayout());
        pnlToolbar.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 6, 0);
        gc.gridx = 1;
        JLabel lblStatus = new JLabel("Trạng thái");
        lblStatus.setFont(font(Font.BOLD, 15));
        lblStatus.setForeground(TEXT);
        pnlToolbar.add(lblStatus, gc);
        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        pnlToolbar.add(pnlSearch, gc);
        gc.weightx = 0;
        gc.insets = new Insets(0, 16, 0, 0);
        gc.gridx = 1;
        pnlToolbar.add(cmbStatus, gc);
        gc.insets = new Insets(0, 12, 0, 0);
        gc.gridx = 2;
        pnlToolbar.add(btnCreate, gc);
        gc.gridx = 3;
        pnlToolbar.add(btnLedger, gc);
        return pnlToolbar;
    }

    private JComponent buildTableCard() {
        Card pnlCard = new Card(new BorderLayout(), 0);
        tblDrugs.setFont(font(Font.PLAIN, 15));
        tblDrugs.setForeground(TEXT);
        tblDrugs.setRowHeight(66);
        tblDrugs.setShowVerticalLines(true);
        tblDrugs.setGridColor(BORDER);
        tblDrugs.setRowSelectionAllowed(false);
        tblDrugs.setFocusable(false);
        tblDrugs.setBackground(SURFACE);
        tblDrugs.setDefaultRenderer(Object.class, new CellRenderer());
        tblDrugs.getColumnModel().getColumn(COL_NAME).setCellRenderer(new NameRenderer());
        tblDrugs.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new SeverityRenderer());
        tblDrugs.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {55, 95, 260, 115, 70, 90, 120, 160, 160};
        for (int i = 0; i < widths.length; i++) {
            tblDrugs.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhDrugs = tblDrugs.getTableHeader();
        tbhDrugs.setReorderingAllowed(false);
        tbhDrugs.setResizingAllowed(false);
        tbhDrugs.setPreferredSize(new Dimension(10, 52));
        tbhDrugs.setDefaultRenderer(new HeaderRenderer());
        JPanel pnlGrid = new JPanel(new BorderLayout());
        pnlGrid.setOpaque(false);
        pnlGrid.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlGrid.add(tbhDrugs, BorderLayout.NORTH);
        pnlGrid.add(tblDrugs, BorderLayout.CENTER);
        pnlCard.add(pnlGrid, BorderLayout.CENTER);
        return pnlCard;
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
        cmbStatus.addActionListener(e -> applyFilters());
        tglAll.addActionListener(e -> applyFilters());
        tglUnclassified.addActionListener(e -> applyFilters());
        groupTabs.values().forEach(t -> t.addActionListener(e -> applyFilters()));
        btnLedger.addActionListener(e -> openLedger(null));
        btnCreate.addActionListener(e -> createStockOut());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblDrugs.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                ControlledDrugRowResponse r = tableModel.rowAt(row);
                if (tblDrugs.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    int link = hit(row, e.getX());
                    if (link == 0) {
                        openLedger(r.maThuoc());
                    } else if (link == 1) {
                        openDetail(r.maThuoc());
                    }
                } else if (e.getClickCount() == 2) {
                    openDetail(r.maThuoc());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tblDrugs.rowAtPoint(e.getPoint());
                boolean hand = row >= 0 && tblDrugs.columnAtPoint(e.getPoint()) == COL_ACTIONS && hit(row, e.getX()) >= 0;
                tblDrugs.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblDrugs.addMouseListener(clicks);
        tblDrugs.addMouseMotionListener(clicks);
    }

    private int hit(int row, int x) {
        Rectangle cell = tblDrugs.getCellRect(row, COL_ACTIONS, false);
        return actionCell.hitTest(x - cell.x, tblDrugs.getFontMetrics(actionCell.getFont()));
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(ControlledDrugSummaryResponse s) {
        lblKinds.setText(String.valueOf(s.tongLoai()));
        lblStock.setText(String.valueOf(s.tongTon()));
        lblExpiring.setText(String.valueOf(s.canHanHetHan()));
        lblPending.setText(String.valueOf(s.choDuyet()));
    }

    private void applyFilters() {
        searchDelay.stop();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        StatusFilter status = (StatusFilter) cmbStatus.getSelectedItem();
        List<ControlledDrugRowResponse> matching = new ArrayList<>();
        Map<ControlledDrugGroup, Integer> counts = new EnumMap<>(ControlledDrugGroup.class);
        int unclassified = 0;
        for (ControlledDrugRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maThuoc().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.tenDayDu().toLowerCase(Locale.ROOT).contains(keyword)
                    || (r.hoatChat() != null && r.hoatChat().toLowerCase(Locale.ROOT).contains(keyword));
            if (matchesKeyword && (status == null || status.test.test(r))) {
                matching.add(r);
                if (r.nhom() == null) {
                    unclassified++;
                } else {
                    counts.merge(r.nhom(), 1, Integer::sum);
                }
            }
        }
        tglAll.setText("Tất cả (" + matching.size() + ")");
        groupTabs.forEach((g, t) -> t.setText(g.tabLabel() + " (" + counts.getOrDefault(g, 0) + ")"));
        tglUnclassified.setText("Chưa phân loại (" + unclassified + ")");
        // Chỉ hiện tab "Chưa phân loại" khi còn thuốc chưa gán nhóm (Figma chỉ có 3 nhóm).
        boolean anyUnclassified = allRows.stream().anyMatch(r -> r.nhom() == null);
        tglUnclassified.setVisible(anyUnclassified);
        if (!anyUnclassified && tglUnclassified.isSelected()) {
            tglAll.setSelected(true);
        }
        Predicate<ControlledDrugRowResponse> tab = r -> true;
        if (tglUnclassified.isSelected()) {
            tab = r -> r.nhom() == null;
        }
        for (Map.Entry<ControlledDrugGroup, JToggleButton> e : groupTabs.entrySet()) {
            if (e.getValue().isSelected()) {
                ControlledDrugGroup g = e.getKey();
                tab = r -> r.nhom() == g;
            }
        }
        filteredRows = matching.stream().filter(tab).toList();
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
                ? (allRows.isEmpty() ? "Chưa có thuốc kiểm soát đặc biệt — đánh dấu \"Kiểm soát đặc biệt\" khi thêm thuốc "
                        + "ở mục Vật tư & thuốc." : "Không có thuốc nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " loại thuốc");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void openDetail(String maThuoc) {
        ControlledDrugDetailDialog dlg = new ControlledDrugDetailDialog(owner(), service, session, maThuoc,
                this::openLedger);
        dlg.setVisible(true);
        if (dlg.changed()) {
            reload();
        }
    }

    void openLedger(String maThuoc) {
        new ControlledLedgerDialog(owner(), service, session, allRows, maThuoc).setVisible(true);
    }

    private void createStockOut() {
        StockOutReceiptDialog dlg = new StockOutReceiptDialog(owner(), stockOutService, itemService, session, null, true);
        dlg.setVisible(true);
        dlg.savedReceipt().ifPresent(saved -> {
            reload();
            lblPageInfo.setForeground(GREEN);
            lblPageInfo.setText("Đã lập phiếu " + saved.maPhieu() + " — chờ một quản lý kho khác bấm \"Xuất kho\" "
                    + "(phê duyệt kép) ở mục Xuất kho.");
        });
    }

    // ------------------------------------------------------------------- table

    /** Màu viên trạng thái theo mức độ: xanh an toàn, cam sắp hết, đỏ thiếu/cận hạn/hết hạn. */
    static Color[] severityColors(NoticeSeverity s) {
        return switch (s) {
            case CRITICAL -> new Color[] {RED, new Color(0xFDE2E2), new Color(0xB42323)};
            case WARNING -> new Color[] {ORANGE, new Color(0xFFEBD2), new Color(0x8F4A00)};
            case INFO -> new Color[] {GREEN, new Color(0xDDF3E4), new Color(0x1E6B34)};
        };
    }

    private static final class SeverityRenderer implements TableCellRenderer {
        private final StatusPill lblStatus = new StatusPill();
        private final JPanel pnlCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 19));

        SeverityRenderer() {
            pnlCell.setBackground(SURFACE);
            pnlCell.add(lblStatus);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            ControlledDrugRowResponse r = (ControlledDrugRowResponse) value;
            Color[] c = severityColors(r.mucDo());
            lblStatus.apply(r.trangThai(), c[0], c[1], c[2]);
            return pnlCell;
        }
    }

    /** Tên thuốc + hàm lượng (dòng 1), "Hoạt chất: …" (dòng 2). */
    private static final class NameRenderer implements TableCellRenderer {
        private final JPanel pnlCell = new JPanel();
        private final JLabel lblName = new JLabel();
        private final JLabel lblActive = new JLabel();

        NameRenderer() {
            pnlCell.setLayout(new BoxLayout(pnlCell, BoxLayout.Y_AXIS));
            pnlCell.setBackground(SURFACE);
            pnlCell.setBorder(BorderFactory.createEmptyBorder(11, 16, 8, 8));
            lblName.setFont(font(Font.PLAIN, 15));
            lblName.setForeground(TEXT);
            lblActive.setFont(font(Font.PLAIN, 13));
            lblActive.setForeground(TEXT_MUTED);
            pnlCell.add(lblName);
            pnlCell.add(Box.createVerticalStrut(3));
            pnlCell.add(lblActive);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            ControlledDrugRowResponse r = (ControlledDrugRowResponse) value;
            lblName.setText(r.tenDayDu());
            lblActive.setText("Hoạt chất: " + (r.hoatChat() == null || r.hoatChat().isBlank() ? "—" : r.hoatChat()));
            return pnlCell;
        }
    }

    /** Hai liên kết "Nhật ký" · "Xem". */
    private static final class LinkPairCell extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final String[] LINKS = {"Nhật ký", "Xem"};
        private static final int PADDING = 16;
        private static final int GAP = 22;

        LinkPairCell() {
            setFont(font(Font.PLAIN, 15));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            return this;
        }

        /** @return 0 = Nhật ký, 1 = Xem, -1 = không trúng */
        int hitTest(int x, FontMetrics fm) {
            int cursor = PADDING;
            for (int i = 0; i < LINKS.length; i++) {
                int w = fm.stringWidth(LINKS[i]);
                if (x >= cursor - 3 && x <= cursor + w + 3) {
                    return i;
                }
                cursor += w + GAP;
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int cursor = PADDING;
            g2.setColor(new Color(0x2F5BD3));
            for (String link : LINKS) {
                g2.drawString(link, cursor, baseline);
                cursor += fm.stringWidth(link) + GAP;
            }
            g2.dispose();
        }
    }

    private static final class DrugTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<ControlledDrugRowResponse> rows = List.of();
        private int offset;

        void setPage(List<ControlledDrugRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        ControlledDrugRowResponse rowAt(int row) {
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
            ControlledDrugRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maThuoc();
                case 3 -> r.nhom() == null ? "Chưa phân loại" : r.nhom().label();
                case 4 -> r.donViTinh();
                case 5 -> r.tonKho();
                case 6 -> InventoryFormat.date(r.hanGanNhat());
                case 8 -> "";
                default -> r;
            };
        }
    }
}
