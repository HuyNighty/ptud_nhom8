package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.StockMonitorAdjustRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockMonitorService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.HeaderRenderer;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ScrollablePage;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
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
 * Màn hình "Tồn kho" (UC36): tồn thực tế từng mặt hàng so với định mức, giá trị kho, vị trí lưu trữ; điều chỉnh nhanh
 * định mức / vị trí / cờ đã đặt hàng, xem lịch sử biến động và xuất Excel.
 */
public final class StockMonitorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 8;
    private static final String[] COLUMNS = {"STT", "Mã SKU", "Tên mặt hàng", "Phân loại", "ĐVT", "Tồn thực tế",
            "Định mức", "Vị trí lưu trữ", "Trạng thái", "Thao tác"};
    private static final int COL_STOCK = 5;
    private static final int COL_STATUS = 8;
    private static final int COL_ACTIONS = 9;
    private static final String ALL_TYPES = "Tất cả";
    private static final String ALL_LOCATIONS = "Tất cả vị trí";
    private static final String NO_LOCATION = "Chưa xếp vị trí";
    private static final Color LINK = new Color(0x2F5BD3);

    private final transient StockMonitorService service;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblItems = statValue();
    private final JLabel lblQuantity = statValue();
    private final JLabel lblValue = statValue();
    private final JLabel lblBelow = statValue();

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm vật tư, thuốc, mã hàng...", 30);
    private final JButton btnActions = createButton("Thao tác", ButtonKind.OUTLINE);
    private final JToggleButton tglAll = createDarkChip("Tất cả");
    private final JToggleButton tglLow = createDarkChip("Hơi thấp/Sắp hết");
    private final JToggleButton tglShort = createDarkChip("Thiếu/Hết hàng");
    private final JToggleButton tglEnough = createDarkChip("Đủ tồn");
    private final JComboBox<String> cmbType = new JComboBox<>(new String[] {ALL_TYPES, "Thuốc", "Vật tư y tế"});
    private final JComboBox<String> cmbLocation = new JComboBox<>(new String[] {ALL_LOCATIONS});
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final JButton btnAdjust = createButton("Điều chỉnh nhanh", ButtonKind.OUTLINE);
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final StockTableModel tableModel = new StockTableModel();
    private final JTable tblStock = new JTable(tableModel);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<StockMonitorRowResponse> allRows = List.of();
    private transient List<StockMonitorRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<List<StockMonitorRowResponse>, Void> runningLoad;

    public StockMonitorPanel(StockMonitorService service, Supplier<InventoryActor> session) {
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

    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải tồn kho…");
        runningLoad = new SwingWorker<>() {
            @Override
            protected List<StockMonitorRowResponse> doInBackground() {
                return service.list(roles);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    allRows = get();
                    applySummary(StockMonitorService.summarize(allRows));
                    refreshLocations();
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
        JLabel lblTitle = new JLabel("Tồn kho");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Theo dõi lượng hàng tồn, giá trị kho, so sánh tồn thực tế với định mức an toàn "
                + "và quản lý chi tiết vị trí lưu kho.");
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
        pnlStats.add(statCard("Tổng mặt hàng", lblItems, "Mã hàng (SKU)", null));
        pnlStats.add(statCard("Tổng số lượng tồn", lblQuantity, "Đơn vị (Cái/Hộp/Ống...)", null));
        pnlStats.add(statCard("Giá trị tồn kho", lblValue, "Tổng giá trị ước tính", null));
        pnlStats.add(statCard("Dưới định mức", lblBelow, "Cần bổ sung", RED));
        return pnlStats;
    }

    private static JLabel statValue() {
        JLabel lbl = new JLabel("—");
        lbl.setFont(font(Font.BOLD, 38));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private static Card statCard(String title, JLabel lblStatValue, String caption, Color dot) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.BOLD, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(TEXT);
        if (dot != null) {
            lblCaption.setIcon(new DotIcon(dot, 11));
            lblCaption.setIconTextGap(8);
        }
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(4));
        pnlStatCard.add(lblStatValue);
        pnlStatCard.add(Box.createVerticalStrut(2));
        pnlStatCard.add(lblCaption);
        return pnlStatCard;
    }

    private JComponent buildListCard() {
        JPanel pnlList = new JPanel();
        pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
        pnlList.setBackground(new Color(0xF7F8FB));
        pnlList.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1, true), BorderFactory.createEmptyBorder(0, 12, 0, 10)));
        pnlSearch.setPreferredSize(new Dimension(600, 44));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);
        btnActions.setIcon(new GlyphIcon(MenuGlyph.SLIDERS, 18, TEXT));
        btnActions.setIconTextGap(10);
        btnActions.setFont(font(Font.PLAIN, 15));
        btnActions.setPreferredSize(new Dimension(btnActions.getPreferredSize().width + 10, 44));
        JPanel pnlSearchRow = new JPanel(new BorderLayout(14, 0));
        pnlSearchRow.setOpaque(false);
        pnlSearchRow.add(pnlSearch, BorderLayout.CENTER);
        pnlSearchRow.add(btnActions, BorderLayout.EAST);
        addFullWidth(pnlList, pnlSearchRow);
        pnlList.add(Box.createVerticalStrut(14));

        JPanel pnlChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlChips.setOpaque(false);
        ButtonGroup grpChips = new ButtonGroup();
        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglLow, tglShort, tglEnough}) {
            grpChips.add(tgl);
            pnlChips.add(tgl);
            pnlChips.add(Box.createHorizontalStrut(12));
        }
        tglAll.setSelected(true);
        addFullWidth(pnlList, pnlChips);
        pnlList.add(Box.createVerticalStrut(16));

        styleCombo(cmbType);
        styleCombo(cmbLocation);
        cmbType.setFont(font(Font.PLAIN, 15));
        cmbLocation.setFont(font(Font.PLAIN, 15));
        cmbType.setPreferredSize(new Dimension(340, 44));
        cmbLocation.setPreferredSize(new Dimension(310, 44));
        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 18, TEXT));
        btnExport.setIconTextGap(10);
        btnExport.setFont(font(Font.PLAIN, 15));
        btnAdjust.setIcon(new GlyphIcon(MenuGlyph.GEAR, 18, TEXT));
        btnAdjust.setIconTextGap(10);
        btnAdjust.setFont(font(Font.PLAIN, 15));
        JPanel pnlFilters = new JPanel(new GridBagLayout());
        pnlFilters.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.gridy = 0;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 8, 0);
        pnlFilters.add(filterLabel("Phân loại"), gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 22, 8, 0);
        pnlFilters.add(filterLabel("Vị trí lưu trữ"), gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 0, 0);
        pnlFilters.add(cmbType, gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 22, 0, 0);
        pnlFilters.add(cmbLocation, gc);
        gc.gridx = 2;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.EAST;
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlButtons.setOpaque(false);
        btnExport.setPreferredSize(new Dimension(btnExport.getPreferredSize().width + 8, 44));
        btnAdjust.setPreferredSize(new Dimension(btnAdjust.getPreferredSize().width + 8, 44));
        pnlButtons.add(btnExport);
        pnlButtons.add(btnAdjust);
        pnlFilters.add(pnlButtons, gc);
        addFullWidth(pnlList, pnlFilters);
        pnlList.add(Box.createVerticalStrut(18));

        JLabel lblListTitle = new JLabel("DANH SÁCH TỒN KHO CHI TIẾT");
        lblListTitle.setFont(font(Font.BOLD, 16));
        lblListTitle.setForeground(TEXT);
        JPanel pnlTitle = new JPanel(new BorderLayout());
        pnlTitle.setOpaque(false);
        pnlTitle.add(lblListTitle, BorderLayout.WEST);
        addFullWidth(pnlList, pnlTitle);
        pnlList.add(Box.createVerticalStrut(10));
        addFullWidth(pnlList, buildTableCard());
        pnlList.add(Box.createVerticalStrut(16));

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        pnlPager.onPageSelected(page -> {
            currentPage = page;
            showPage();
        });
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);
        addFullWidth(pnlList, pnlFooter);
        pnlList.setOpaque(false);
        return pnlList;
    }

    private static JLabel filterLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font(Font.PLAIN, 16));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private JComponent buildTableCard() {
        Card pnlCard = new Card(new BorderLayout(), 0);
        tblStock.setFont(font(Font.PLAIN, 15));
        tblStock.setForeground(TEXT);
        tblStock.setRowHeight(56);
        tblStock.setShowVerticalLines(false);
        tblStock.setGridColor(BORDER);
        tblStock.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblStock.setSelectionBackground(SELECTED_FILL);
        tblStock.setFocusable(false);
        tblStock.setBackground(SURFACE);
        tblStock.setDefaultRenderer(Object.class, new PlainRenderer());
        tblStock.getColumnModel().getColumn(COL_STOCK).setCellRenderer(new StockRenderer());
        tblStock.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new LevelRenderer());
        tblStock.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(new LinkRenderer());
        int[] widths = {55, 95, 270, 115, 75, 110, 95, 135, 150, 130};
        for (int i = 0; i < widths.length; i++) {
            tblStock.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhStock = tblStock.getTableHeader();
        tbhStock.setReorderingAllowed(false);
        tbhStock.setResizingAllowed(false);
        tbhStock.setPreferredSize(new Dimension(10, 52));
        tbhStock.setDefaultRenderer(new HeaderRenderer());
        JPanel pnlGrid = new JPanel(new BorderLayout());
        pnlGrid.setOpaque(false);
        pnlGrid.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlGrid.add(tbhStock, BorderLayout.NORTH);
        pnlGrid.add(tblStock, BorderLayout.CENTER);
        pnlCard.add(pnlGrid, BorderLayout.CENTER);
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
        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglLow, tglShort, tglEnough}) {
            tgl.addActionListener(e -> applyFilters());
        }
        cmbType.addActionListener(e -> applyFilters());
        cmbLocation.addActionListener(e -> applyFilters());
        btnExport.addActionListener(e -> exportExcel());
        btnAdjust.addActionListener(e -> selected().ifPresentOrElse(this::openAdjust, this::askToSelect));
        btnActions.addActionListener(e -> showActionMenu());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblStock.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                StockMonitorRowResponse r = tableModel.rowAt(row);
                if (tblStock.columnAtPoint(e.getPoint()) == COL_ACTIONS || e.getClickCount() == 2) {
                    openHistory(r);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean hand = tblStock.rowAtPoint(e.getPoint()) >= 0
                        && tblStock.columnAtPoint(e.getPoint()) == COL_ACTIONS;
                tblStock.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblStock.addMouseListener(clicks);
        tblStock.addMouseMotionListener(clicks);
    }

    private Optional<StockMonitorRowResponse> selected() {
        int row = tblStock.getSelectedRow();
        return row < 0 ? Optional.empty() : Optional.of(tableModel.rowAt(row));
    }

    private void askToSelect() {
        lblPageInfo.setForeground(ORANGE);
        lblPageInfo.setText("Chọn một mặt hàng trong bảng trước.");
    }

    private void showActionMenu() {
        JPopupMenu pmnActions = new JPopupMenu();
        Optional<StockMonitorRowResponse> current = selected();
        JMenuItem mnuHistory = new JMenuItem("Xem lịch sử mặt hàng đang chọn");
        mnuHistory.addActionListener(e -> current.ifPresent(this::openHistory));
        JMenuItem mnuAdjust = new JMenuItem("Điều chỉnh nhanh mặt hàng đang chọn");
        mnuAdjust.addActionListener(e -> current.ifPresent(this::openAdjust));
        boolean ordered = current.map(StockMonitorRowResponse::daDatHang).orElse(false);
        JMenuItem mnuOrdered = new JMenuItem(ordered ? "Bỏ đánh dấu đã đặt hàng" : "Đánh dấu đã đặt hàng");
        mnuOrdered.addActionListener(e -> current.ifPresent(r -> toggleOrdered(r)));
        for (JMenuItem mnu : new JMenuItem[] {mnuHistory, mnuAdjust, mnuOrdered}) {
            mnu.setEnabled(current.isPresent());
            mnu.setFont(font(Font.PLAIN, 14));
            pmnActions.add(mnu);
        }
        pmnActions.addSeparator();
        JMenuItem mnuReload = new JMenuItem("Tải lại dữ liệu");
        mnuReload.setFont(font(Font.PLAIN, 14));
        mnuReload.addActionListener(e -> reload());
        pmnActions.add(mnuReload);
        if (current.isEmpty()) {
            JMenuItem mnuHint = new JMenuItem("(Chọn một dòng trong bảng để thao tác)");
            mnuHint.setEnabled(false);
            pmnActions.add(mnuHint, 0);
        }
        pmnActions.show(btnActions, 0, btnActions.getHeight() + 4);
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(StockMonitorSummaryResponse s) {
        lblItems.setText(String.format(Locale.ROOT, "%,d", s.tongMatHang()).replace(',', '.'));
        lblQuantity.setText(String.format(Locale.ROOT, "%,d", s.tongSoLuongTon()).replace(',', '.'));
        lblValue.setText(InventoryFormat.money(s.giaTriTonKho()));
        lblBelow.setText(String.valueOf(s.duoiDinhMuc()));
        tglLow.setToolTipText("Tồn khả dụng còn trên định mức nhưng dưới định mức + 20%.");
    }

    private void refreshLocations() {
        Object selectedLocation = cmbLocation.getSelectedItem();
        Set<String> locations = new TreeSet<>();
        boolean anyMissing = false;
        for (StockMonitorRowResponse r : allRows) {
            if (r.viTriLuuTru() == null) {
                anyMissing = true;
            } else {
                locations.add(r.viTriLuuTru());
            }
        }
        boolean missing = anyMissing;
        EmergencyKitPanel.ActionGuard.run(cmbLocation, () -> {
            cmbLocation.removeAllItems();
            cmbLocation.addItem(ALL_LOCATIONS);
            locations.forEach(cmbLocation::addItem);
            if (missing) {
                cmbLocation.addItem(NO_LOCATION);
            }
            boolean keep = selectedLocation != null && (locations.contains(selectedLocation)
                    || (missing && NO_LOCATION.equals(selectedLocation)));
            cmbLocation.setSelectedItem(keep ? selectedLocation : ALL_LOCATIONS);
        });
    }

    static String typeLabel(InventoryItemType loai) {
        return loai == InventoryItemType.THUOC ? "Thuốc" : "Vật tư y tế";
    }

    static String levelLabel(StockLevel level) {
        return level == StockLevel.DU ? "An toàn" : level.label();
    }

    static Color levelColor(StockLevel level) {
        return switch (level) {
            case HET_HANG, THIEU -> RED;
            case SAP_HET -> ORANGE;
            case DU -> GREEN;
        };
    }

    private void applyFilters() {
        searchDelay.stop();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String type = (String) cmbType.getSelectedItem();
        String location = (String) cmbLocation.getSelectedItem();
        Predicate<StockMonitorRowResponse> base = r -> {
            boolean matchesKeyword = keyword.isEmpty() || r.ma().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.ten().toLowerCase(Locale.ROOT).contains(keyword)
                    || (r.viTriLuuTru() != null && r.viTriLuuTru().toLowerCase(Locale.ROOT).contains(keyword));
            boolean matchesType = type == null || ALL_TYPES.equals(type) || typeLabel(r.loai()).equals(type);
            boolean matchesLocation = location == null || ALL_LOCATIONS.equals(location)
                    || (NO_LOCATION.equals(location) ? r.viTriLuuTru() == null : location.equals(r.viTriLuuTru()));
            return matchesKeyword && matchesType && matchesLocation;
        };
        List<StockMonitorRowResponse> matching = allRows.stream().filter(base).toList();
        long low = matching.stream().filter(r -> r.mucTon() == StockLevel.SAP_HET).count();
        long shortage = matching.stream().filter(r -> r.mucTon() == StockLevel.THIEU
                || r.mucTon() == StockLevel.HET_HANG).count();
        long enough = matching.stream().filter(r -> r.mucTon() == StockLevel.DU).count();
        tglAll.setText("Tất cả (" + matching.size() + ")");
        tglLow.setText("Hơi thấp/Sắp hết (" + low + ")");
        tglShort.setText("Thiếu/Hết hàng (" + shortage + ")");
        tglEnough.setText("Đủ tồn (" + enough + ")");
        filteredRows = matching.stream().filter(r -> {
            if (tglLow.isSelected()) {
                return r.mucTon() == StockLevel.SAP_HET;
            }
            if (tglShort.isSelected()) {
                return r.mucTon() == StockLevel.THIEU || r.mucTon() == StockLevel.HET_HANG;
            }
            return !tglEnough.isSelected() || r.mucTon() == StockLevel.DU;
        }).toList();
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
                ? (allRows.isEmpty() ? "Danh mục chưa có thuốc / vật tư — thêm ở mục Vật tư & thuốc."
                        : "Không có mặt hàng nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " mặt hàng");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void openHistory(StockMonitorRowResponse r) {
        new StockHistoryDialog(owner(), service, session, r).setVisible(true);
    }

    private void openAdjust(StockMonitorRowResponse r) {
        List<String> locations = allRows.stream().map(StockMonitorRowResponse::viTriLuuTru)
                .filter(Objects::nonNull).distinct().sorted().toList();
        StockAdjustDialog dlg = new StockAdjustDialog(owner(), service, session, r, locations);
        dlg.setVisible(true);
        dlg.saved().ifPresent(saved -> afterSave("Đã cập nhật " + saved.ma() + " — " + saved.ten() + "."));
    }

    private void toggleOrdered(StockMonitorRowResponse r) {
        InventoryActor actor = session.get();
        StockMonitorAdjustRequest request = new StockMonitorAdjustRequest(r.dinhMucToiThieu(), r.viTriLuuTru(),
                !r.daDatHang());
        new SwingWorker<StockMonitorRowResponse, Void>() {
            @Override
            protected StockMonitorRowResponse doInBackground() {
                return service.adjust(actor, r.loai(), r.ma(), request);
            }

            @Override
            protected void done() {
                try {
                    StockMonitorRowResponse saved = get();
                    afterSave((saved.daDatHang() ? "Đã đánh dấu đã đặt hàng: " : "Đã bỏ đánh dấu đặt hàng: ")
                            + saved.ma() + ".");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblPageInfo.setForeground(RED);
                    lblPageInfo.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void afterSave(String message) {
        reload();
        Timer show = new Timer(400, e -> {
            lblPageInfo.setForeground(GREEN);
            lblPageInfo.setText(message);
        });
        show.setRepeats(false);
        show.start();
    }

    private void exportExcel() {
        if (filteredRows.isEmpty()) {
            lblPageInfo.setForeground(TEXT_MUTED);
            lblPageInfo.setText("Không có dữ liệu để xuất.");
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách tồn kho");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("ton-kho.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<StockMonitorRowResponse> snapshot = filteredRows;
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

    static void writeExcel(File target, List<StockMonitorRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã SKU", 10, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tên mặt hàng", 34, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Phân loại", 13, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("ĐVT", 8, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Tồn thực tế", 12, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Tồn còn hạn", 12, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Định mức", 10, XlsxExport.Kind.INTEGER),
                new XlsxExport.Column("Vị trí lưu trữ", 15, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Trạng thái", 12, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Đã đặt hàng", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Hạn gần nhất", 13, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Giá trị tồn (đ)", 16, XlsxExport.Kind.MONEY));
        List<List<Object>> data = new ArrayList<>();
        int index = 0;
        for (StockMonitorRowResponse r : rows) {
            List<Object> row = new ArrayList<>();
            row.add(++index);
            row.add(r.ma());
            row.add(r.ten());
            row.add(typeLabel(r.loai()));
            row.add(r.donViTinh());
            row.add(r.tonThucTe());
            row.add(r.tonKhaDung());
            row.add(r.dinhMucToiThieu());
            row.add(r.viTriLuuTru());
            row.add(levelLabel(r.mucTon()));
            row.add(r.daDatHang() ? "Có" : "");
            row.add(r.hanGanNhat());
            row.add(r.giaTri());
            data.add(row);
        }
        XlsxExport.write(target, "Ton kho", "DANH SÁCH TỒN KHO CHI TIẾT",
                "Xuất lúc " + InventoryFormat.dateTime(java.time.LocalDateTime.now()) + " · " + rows.size()
                        + " mặt hàng · giá trị theo giá nhập gần nhất của từng lô", columns, data);
    }

    // ------------------------------------------------------------------- table

    private static class PlainRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
            lbl.setFont(tbl.getFont());
            lbl.setForeground(TEXT);
            lbl.setBackground(selected ? SELECTED_FILL : SURFACE);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            lbl.setToolTipText(null);
            return lbl;
        }
    }

    /** Tồn thực tế; cam + chú thích khi có lô đã hết hạn còn nằm trong kho. */
    private static final class StockRenderer extends PlainRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            StockMonitorRowResponse r = (StockMonitorRowResponse) value;
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, r.tonThucTe(), selected, focused, row, column);
            if (r.tonHetHan() > 0) {
                lbl.setText(r.tonThucTe() + " *");
                lbl.setForeground(ORANGE);
                lbl.setToolTipText("Trong đó " + r.tonHetHan() + " " + r.donViTinh().toLowerCase(Locale.ROOT)
                        + " thuộc lô đã hết hạn (không tính vào tồn khả dụng) — cần lập phiếu hủy.");
            }
            return lbl;
        }
    }

    /** Chấm màu + nhãn mức tồn; dòng phụ "Đã đặt hàng" nếu có. */
    private static final class LevelRenderer extends PlainRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            StockMonitorRowResponse r = (StockMonitorRowResponse) value;
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, "", selected, focused, row, column);
            lbl.setIcon(new DotIcon(levelColor(r.mucTon()), 12));
            lbl.setIconTextGap(8);
            lbl.setText(r.daDatHang()
                    ? "<html>" + levelLabel(r.mucTon()) + "<br><span style='font-size:9px;color:#5E6678'>"
                    + "Đã đặt hàng</span></html>"
                    : levelLabel(r.mucTon()));
            lbl.setToolTipText("Tồn còn hạn " + r.tonKhaDung() + " / định mức " + r.dinhMucToiThieu()
                    + (r.daDatHang() ? " · đã đặt hàng với nhà cung cấp" : ""));
            return lbl;
        }
    }

    private static final class LinkRenderer extends PlainRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, "Xem lịch sử", selected, focused, row,
                    column);
            lbl.setForeground(LINK);
            return lbl;
        }
    }

    private static final class StockTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<StockMonitorRowResponse> rows = List.of();
        private int offset;

        void setPage(List<StockMonitorRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        StockMonitorRowResponse rowAt(int row) {
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
            StockMonitorRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.ma();
                case 2 -> r.ten();
                case 3 -> typeLabel(r.loai());
                case 4 -> r.donViTinh();
                case 6 -> r.dinhMucToiThieu();
                case 7 -> r.viTriLuuTru() == null ? "—" : r.viTriLuuTru();
                default -> r;
            };
        }
    }
}
