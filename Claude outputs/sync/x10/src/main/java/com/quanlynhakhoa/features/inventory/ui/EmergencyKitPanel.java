package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Màn hình "Hộp thuốc cấp cứu" (UC34): danh sách hộp tại các phòng, tình trạng (đủ / thiếu / cận hạn), kiểm tra định
 * kỳ, bổ sung / thay thế từ kho và nhật ký kiểm tra.
 */
public final class EmergencyKitPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 6;
    private static final String[] COLUMNS = {"STT", "Mã hộp", "Tên hộp cấp cứu", "Vị trí", "Số mặt hàng",
            "Kiểm tra gần nhất", "Hạn dùng kế tiếp", "Trạng thái", "Thao tác"};
    private static final int COL_NAME = 2;
    private static final int COL_LOCATION = 3;
    private static final int COL_LAST_CHECK = 5;
    private static final int COL_STATUS = 7;
    private static final int COL_ACTIONS = 8;
    private static final String ALL_LOCATIONS = "Vị trí";
    private static final String ALL_STATUSES = "Tình trạng";

    /** Các thao tác trên một dòng. */
    enum KitAction {
        DETAIL("Chi tiết"), CHECK("Kiểm tra"), RESTOCK("Bổ sung"), EDIT("Sửa"), REPLACE("Thay thế");

        final String label;

        KitAction(String label) {
            this.label = label;
        }

        /** Hai thao tác hợp lý nhất theo tình trạng (Figma); mọi thao tác khác có trong "Chi tiết". */
        static List<KitAction> forStatus(EmergencyKitStatus status) {
            return switch (status) {
                case THIEU -> List.of(RESTOCK, EDIT);
                case CAN_HAN, HET_HAN -> List.of(REPLACE, CHECK);
                case CHO_BO_SUNG -> List.of(DETAIL, RESTOCK);
                case CHUA_CAU_HINH -> List.of(EDIT, DETAIL);
                case AN_TOAN -> List.of(DETAIL, CHECK);
            };
        }
    }

    private final transient EmergencyKitService service;
    private final transient InventoryItemService itemService;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotal = statValue(TEXT);
    private final JLabel lblKinds = statValue(TEXT);
    private final JLabel lblRestock = statValue(ORANGE);
    private final JLabel lblExpiry = statValue(RED);

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm tên hộp, thuốc, vị trí...", 20);
    private final JToggleButton tglAll = createSegment("Tất cả");
    private final JToggleButton tglAntiShock = createSegment(EmergencyKitType.CHONG_SOC.tabLabel());
    private final JToggleButton tglBasic = createSegment(EmergencyKitType.CO_BAN.tabLabel());
    private final JToggleButton tglDue = createSegment("Đến hạn kiểm tra");
    private final JComboBox<String> cmbLocation = new JComboBox<>(new String[] {ALL_LOCATIONS});
    private final JComboBox<Object> cmbStatus = new JComboBox<>();
    private final JButton btnCreate = createButton("Tạo hộp cấp cứu", ButtonKind.PRIMARY);
    private final JButton btnLog = createButton("Nhật ký kiểm tra", ButtonKind.OUTLINE);
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final KitTableModel tableModel = new KitTableModel();
    private final JTable tblKits = new JTable(tableModel);
    private final ActionLinksCell actionCell = new ActionLinksCell();
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();
    private final JLabel lblWarning = new JLabel();
    private final Card pnlWarning = new Card(new BorderLayout(), 14, 12).tint(new Color(0xFDECEC), new Color(0xF5C2C2));

    private transient List<EmergencyKitRowResponse> allRows = List.of();
    private transient List<EmergencyKitRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(EmergencyKitSummaryResponse summary, List<EmergencyKitRowResponse> rows) {
    }

    public EmergencyKitPanel(EmergencyKitService service, InventoryItemService itemService,
                             Supplier<InventoryActor> session) {
        this.service = Objects.requireNonNull(service);
        this.itemService = Objects.requireNonNull(itemService);
        this.session = Objects.requireNonNull(session);
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        cmbStatus.addItem(ALL_STATUSES);
        for (EmergencyKitStatus s : EmergencyKitStatus.values()) {
            cmbStatus.addItem(s);
        }

        ScrollablePage pnlPage = new ScrollablePage();
        pnlPage.setLayout(new BoxLayout(pnlPage, BoxLayout.Y_AXIS));
        pnlPage.setBackground(BACKGROUND);
        pnlPage.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));
        addFullWidth(pnlPage, buildHeader());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, sectionTitle("KHỐI THỐNG KÊ"));
        pnlPage.add(Box.createVerticalStrut(10));
        addFullWidth(pnlPage, buildStats());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, sectionTitle("KHU VỰC BỘ LỌC & THAO TÁC NHANH"));
        pnlPage.add(Box.createVerticalStrut(10));
        addFullWidth(pnlPage, buildToolbar());
        pnlPage.add(Box.createVerticalStrut(22));
        addFullWidth(pnlPage, sectionTitle("BẢNG DANH SÁCH HỘP THUỐC CẤP CỨU & TÌNH TRẠNG"));
        pnlPage.add(Box.createVerticalStrut(10));
        addFullWidth(pnlPage, buildTableCard());
        pnlPage.add(Box.createVerticalStrut(14));
        addFullWidth(pnlPage, buildWarning());

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
        lblPageInfo.setText("Đang tải danh sách hộp cấp cứu…");
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
                    refreshLocations();
                    applyFilters();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    allRows = List.of();
                    filteredRows = List.of();
                    tableModel.setPage(List.of(), 0);
                    pnlPager.update(0, 0);
                    pnlWarning.setVisible(false);
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

    static JComponent sectionTitle(String text) {
        JLabel lblSection = new JLabel(text);
        lblSection.setFont(font(Font.BOLD, 15));
        lblSection.setForeground(TEXT);
        JPanel pnlSection = new JPanel(new BorderLayout());
        pnlSection.setOpaque(false);
        pnlSection.add(lblSection, BorderLayout.WEST);
        return pnlSection;
    }

    private static JComponent buildHeader() {
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Hộp thuốc cấp cứu");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Quản lý, kiểm tra, theo dõi số lượng và hạn sử dụng của các thuốc, vật tư trong "
                + "các hộp thuốc cấp cứu (chống sốc, cấp cứu cơ bản) tại các phòng khám.");
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
        pnlStats.add(statCard("Tổng hộp cấp cứu", lblTotal, "Hộp tại các phòng/khoa", TEXT));
        pnlStats.add(statCard("Tổng loại thuốc & vật tư", lblKinds, "Đang được lưu trữ", TEXT));
        pnlStats.add(statCard("Hộp cần bổ sung", lblRestock, "Thiếu hụt thuốc", ORANGE));
        pnlStats.add(statCard("Hộp cận/hết hạn", lblExpiry, "Sắp/Đã hết hạn dùng", RED));
        return pnlStats;
    }

    private static JLabel statValue(Color color) {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 40));
        lblValue.setForeground(color);
        return lblValue;
    }

    private static Card statCard(String title, JLabel lblValue, String caption, Color captionColor) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        JLabel lblCaption = new JLabel(caption);
        lblCaption.setFont(font(Font.PLAIN, 15));
        lblCaption.setForeground(captionColor);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(4));
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
        pnlSearch.setPreferredSize(new Dimension(330, 42));
        pnlSearch.setMinimumSize(new Dimension(200, 42));
        txtSearch.setFont(font(Font.PLAIN, 15));
        txtSearch.setForeground(TEXT);
        txtSearch.setBorder(BorderFactory.createEmptyBorder());
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 18, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtSearch, BorderLayout.CENTER);

        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglAntiShock, tglBasic, tglDue}) {
            tgl.setFont(font(Font.PLAIN, 15));
        }
        Card pnlSegments = createSegmentBar(tglAll, tglAll, tglAntiShock, tglBasic, tglDue);

        styleCombo(cmbLocation);
        styleCombo(cmbStatus);
        cmbLocation.setPreferredSize(new Dimension(185, 42));
        cmbStatus.setPreferredSize(new Dimension(185, 42));
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        btnLog.setIcon(new GlyphIcon(MenuGlyph.CLIPBOARD, 18, TEXT));
        btnLog.setIconTextGap(8);
        btnLog.setFont(font(Font.PLAIN, 15));
        Dimension buttonSize = new Dimension(Math.max(btnCreate.getPreferredSize().width,
                btnLog.getPreferredSize().width), 42);
        btnCreate.setPreferredSize(buttonSize);
        btnLog.setPreferredSize(buttonSize);

        JPanel pnlToolbar = new JPanel(new GridBagLayout());
        pnlToolbar.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.6;
        pnlToolbar.add(pnlSearch, gc);
        gc.gridx = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 14, 0, 0);
        pnlToolbar.add(pnlSegments, gc);
        gc.gridx = 2;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.EAST;
        pnlToolbar.add(btnCreate, gc);

        JPanel pnlCombos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlCombos.setOpaque(false);
        pnlCombos.add(cmbLocation);
        pnlCombos.add(Box.createHorizontalStrut(14));
        pnlCombos.add(cmbStatus);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(14, 0, 0, 0);
        pnlToolbar.add(pnlCombos, gc);
        gc.gridx = 2;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.EAST;
        pnlToolbar.add(btnLog, gc);
        return pnlToolbar;
    }

    private JComponent buildTableCard() {
        Card pnlCard = new Card(new BorderLayout(), 0);
        tblKits.setFont(font(Font.PLAIN, 15));
        tblKits.setForeground(TEXT);
        tblKits.setRowHeight(70);
        tblKits.setShowVerticalLines(false);
        tblKits.setGridColor(BORDER);
        tblKits.setRowSelectionAllowed(false);
        tblKits.setFocusable(false);
        tblKits.setBackground(SURFACE);
        tblKits.setDefaultRenderer(Object.class, new CellRenderer());
        tblKits.getColumnModel().getColumn(COL_NAME).setCellRenderer(new WrapRenderer());
        tblKits.getColumnModel().getColumn(COL_LOCATION).setCellRenderer(new WrapRenderer());
        tblKits.getColumnModel().getColumn(COL_LAST_CHECK).setCellRenderer(new LastCheckRenderer());
        tblKits.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new KitStatusRenderer());
        tblKits.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {55, 105, 245, 150, 115, 130, 130, 190, 205};
        for (int i = 0; i < widths.length; i++) {
            tblKits.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhKits = tblKits.getTableHeader();
        tbhKits.setReorderingAllowed(false);
        tbhKits.setResizingAllowed(false);
        tbhKits.setPreferredSize(new Dimension(10, 60));
        tbhKits.setDefaultRenderer(new WrappingHeaderRenderer());
        JPanel pnlGrid = new JPanel(new BorderLayout());
        pnlGrid.setOpaque(false);
        pnlGrid.setBorder(BorderFactory.createEmptyBorder(1, 1, 0, 1));
        pnlGrid.add(tbhKits, BorderLayout.NORTH);
        pnlGrid.add(tblKits, BorderLayout.CENTER);
        pnlCard.add(pnlGrid, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        lblPageInfo.setFont(font(Font.PLAIN, 15));
        pnlPager.onPageSelected(page -> {
            currentPage = page;
            showPage();
        });
        pnlFooter.add(lblPageInfo, BorderLayout.WEST);
        pnlFooter.add(pnlPager, BorderLayout.EAST);
        pnlCard.add(pnlFooter, BorderLayout.SOUTH);
        return pnlCard;
    }

    private JComponent buildWarning() {
        lblWarning.setFont(font(Font.PLAIN, 15));
        lblWarning.setForeground(new Color(0xB42323));
        lblWarning.setIcon(new GlyphIcon(MenuGlyph.WARNING, 20, new Color(0xB42323)));
        lblWarning.setIconTextGap(10);
        pnlWarning.add(lblWarning, BorderLayout.CENTER);
        pnlWarning.setVisible(false);
        return pnlWarning;
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
        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglAntiShock, tglBasic, tglDue}) {
            tgl.addActionListener(e -> applyFilters());
        }
        cmbLocation.addActionListener(e -> applyFilters());
        cmbStatus.addActionListener(e -> applyFilters());
        btnCreate.addActionListener(e -> openEditor(null));
        btnLog.addActionListener(e -> openLog(null));

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblKits.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                EmergencyKitRowResponse r = tableModel.rowAt(row);
                if (tblKits.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
                    KitAction action = hit(row, r, e.getX());
                    if (action != null) {
                        perform(action, r.maHop());
                    }
                } else if (e.getClickCount() == 2) {
                    perform(KitAction.DETAIL, r.maHop());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = tblKits.rowAtPoint(e.getPoint());
                boolean hand = row >= 0 && tblKits.columnAtPoint(e.getPoint()) == COL_ACTIONS
                        && hit(row, tableModel.rowAt(row), e.getX()) != null;
                tblKits.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblKits.addMouseListener(clicks);
        tblKits.addMouseMotionListener(clicks);
    }

    private KitAction hit(int row, EmergencyKitRowResponse r, int x) {
        Rectangle cell = tblKits.getCellRect(row, COL_ACTIONS, false);
        return actionCell.hitTest(KitAction.forStatus(r.trangThai()), x - cell.x,
                tblKits.getFontMetrics(actionCell.getFont()));
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(EmergencyKitSummaryResponse s) {
        lblTotal.setText(String.valueOf(s.tongHop()));
        lblKinds.setText(String.valueOf(s.tongLoaiMatHang()));
        lblRestock.setText(String.valueOf(s.hopCanBoSung()));
        lblExpiry.setText(String.valueOf(s.hopCanHetHan()));
        List<String> warnings = s.canhBao();
        if (warnings.isEmpty()) {
            pnlWarning.setVisible(false);
            return;
        }
        int shown = Math.min(2, warnings.size());
        StringBuilder text = new StringBuilder("<html>Cảnh báo: ");
        for (int i = 0; i < shown; i++) {
            text.append(escape(warnings.get(i))).append(' ');
        }
        if (warnings.size() > shown) {
            text.append("<i>(và ").append(warnings.size() - shown).append(" cảnh báo khác — xem chi tiết từng hộp)</i>");
        }
        lblWarning.setText(text.append("</html>").toString());
        lblWarning.setToolTipText("<html>" + String.join("<br>", warnings.stream().map(EmergencyKitPanel::escape)
                .toList()) + "</html>");
        pnlWarning.setVisible(true);
        revalidate();
    }

    /** Ngắt dòng theo độ rộng thật của chữ (HTML trong JLabel không tự xuống dòng theo độ rộng ô). */
    static String wrapHtml(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && fm.stringWidth(candidate) > width) {
                html.append(escape(line.toString())).append("<br>");
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        return html.append(escape(line.toString())).append("</html>").toString();
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void refreshLocations() {
        Object selected = cmbLocation.getSelectedItem();
        Set<String> locations = new TreeSet<>();
        allRows.forEach(r -> locations.add(r.viTri()));
        ActionGuard.run(cmbLocation, () -> {
            cmbLocation.removeAllItems();
            cmbLocation.addItem(ALL_LOCATIONS);
            locations.forEach(cmbLocation::addItem);
            cmbLocation.setSelectedItem(locations.contains(selected) ? selected : ALL_LOCATIONS);
        });
    }

    private void applyFilters() {
        searchDelay.stop();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        String location = (String) cmbLocation.getSelectedItem();
        EmergencyKitStatus status = cmbStatus.getSelectedItem() instanceof EmergencyKitStatus s ? s : null;
        List<EmergencyKitRowResponse> matching = new ArrayList<>();
        int antiShock = 0;
        int basic = 0;
        int due = 0;
        for (EmergencyKitRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maHop().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.tenHop().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.viTri().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.tuKhoa().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesLocation = location == null || ALL_LOCATIONS.equals(location) || location.equals(r.viTri());
            if (matchesKeyword && matchesLocation && (status == null || r.trangThai() == status)) {
                matching.add(r);
                if (r.loai() == EmergencyKitType.CHONG_SOC) {
                    antiShock++;
                } else {
                    basic++;
                }
                if (r.denHanKiemTra()) {
                    due++;
                }
            }
        }
        tglAll.setText("Tất cả (" + matching.size() + ")");
        tglAntiShock.setText(EmergencyKitType.CHONG_SOC.tabLabel() + " (" + antiShock + ")");
        tglBasic.setText(EmergencyKitType.CO_BAN.tabLabel() + " (" + basic + ")");
        tglDue.setText("Đến hạn kiểm tra (" + due + ")");
        filteredRows = matching.stream().filter(r -> {
            if (tglAntiShock.isSelected()) {
                return r.loai() == EmergencyKitType.CHONG_SOC;
            }
            if (tglBasic.isSelected()) {
                return r.loai() == EmergencyKitType.CO_BAN;
            }
            return !tglDue.isSelected() || r.denHanKiemTra();
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
                ? (allRows.isEmpty() ? "Chưa có hộp cấp cứu nào — bấm \"Tạo hộp cấp cứu\" để khai báo hộp đầu tiên."
                        : "Không có hộp nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " hộp cấp cứu");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void perform(KitAction action, String maHop) {
        switch (action) {
            case DETAIL -> {
                EmergencyKitDetailDialog dlg = new EmergencyKitDetailDialog(owner(), service, itemService, session,
                        maHop, allLocations(), this::openLog);
                dlg.setVisible(true);
                if (dlg.changed()) {
                    reload();
                }
            }
            case EDIT -> openEditor(maHop);
            case CHECK, RESTOCK, REPLACE -> runOnDetail(maHop, action);
        }
    }

    /** Tải chi tiết hộp rồi mở hộp thoại kiểm tra / bổ sung / thay thế. */
    private void runOnDetail(String maHop, KitAction action) {
        Set<Role> roles = session.get().roles();
        new SwingWorker<com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse, Void>() {
            @Override
            protected com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse doInBackground() {
                return service.getDetail(roles, maHop);
            }

            @Override
            protected void done() {
                try {
                    var detail = get();
                    boolean changed = switch (action) {
                        case CHECK -> EmergencyKitCheckDialog.open(owner(), service, session, detail);
                        case RESTOCK -> EmergencyKitRestockDialog.open(owner(), service, session, detail,
                                KitLogType.BO_SUNG);
                        default -> EmergencyKitRestockDialog.open(owner(), service, session, detail,
                                KitLogType.THAY_THE);
                    };
                    if (changed) {
                        reload();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblPageInfo.setForeground(RED);
                    lblPageInfo.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private List<String> allLocations() {
        return allRows.stream().map(EmergencyKitRowResponse::viTri).distinct().sorted().toList();
    }

    private void openEditor(String maHop) {
        EmergencyKitEditorDialog dlg = new EmergencyKitEditorDialog(owner(), service, itemService, session, maHop,
                allLocations());
        dlg.setVisible(true);
        dlg.saved().ifPresent(saved -> {
            reload();
            lblPageInfo.setForeground(GREEN);
            lblPageInfo.setText((maHop == null ? "Đã tạo hộp " : "Đã lưu hộp ") + saved.maHop()
                    + (maHop == null ? " — hãy \"Bổ sung\" từ kho hoặc \"Kiểm tra\" để ghi nhận số đang có." : "."));
        });
    }

    void openLog(String maHop) {
        new EmergencyKitLogDialog(owner(), service, session, allRows, maHop).setVisible(true);
    }

    // ------------------------------------------------------------------- table

    /** Màu viên trạng thái: {chấm, nền, chữ}. */
    static Color[] statusColors(EmergencyKitStatus s) {
        return switch (s) {
            case HET_HAN, CAN_HAN -> new Color[] {RED, new Color(0xFDE2E2), new Color(0xB42323)};
            case THIEU -> new Color[] {ORANGE, new Color(0xFFE6CC), new Color(0x8F4A00)};
            case CHO_BO_SUNG -> new Color[] {new Color(0xE5B300), new Color(0xFFF3C4), new Color(0x7A5B00)};
            case CHUA_CAU_HINH -> new Color[] {TEXT_MUTED, HEADER_ROW, TEXT_MUTED};
            case AN_TOAN -> new Color[] {GREEN, new Color(0xDDF3E4), new Color(0x1E6B34)};
        };
    }

    static String statusTooltip(EmergencyKitStatus s) {
        return switch (s) {
            case CHO_BO_SUNG -> "Thiếu hàng và kho không đủ hàng còn hạn để bổ sung — chờ nhập kho.";
            case THIEU -> "Thiếu so với số tối thiểu; kho đủ hàng để bổ sung ngay.";
            case CAN_HAN -> "Có mặt hàng còn hạn không quá ngưỡng cận hạn của hộp.";
            case HET_HAN -> "Có mặt hàng đã quá hạn dùng — cần thay thế ngay.";
            case CHUA_CAU_HINH -> "Hộp chưa khai báo mặt hàng.";
            case AN_TOAN -> "Mọi mặt hàng đủ số lượng và còn hạn trên ngưỡng.";
        };
    }

    private static final class KitStatusRenderer implements TableCellRenderer {
        private final StatusPill lblStatus = new StatusPill();
        private final JPanel pnlCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 21));

        KitStatusRenderer() {
            pnlCell.setBackground(SURFACE);
            pnlCell.add(lblStatus);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            EmergencyKitStatus s = (EmergencyKitStatus) value;
            Color[] c = statusColors(s);
            lblStatus.apply(s.label(), c[0], c[1], c[2]);
            pnlCell.setToolTipText(statusTooltip(s));
            return pnlCell;
        }
    }

    /** Chữ xuống dòng trong ô (tên hộp, vị trí dài). */
    static final class WrapRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lbl.setFont(tbl.getFont());
            int width = tbl.getColumnModel().getColumn(column).getWidth() - 28;
            lbl.setText(wrapHtml(value == null ? "" : value.toString(), lbl.getFontMetrics(lbl.getFont()), width));
            lbl.setForeground(TEXT);
            lbl.setBackground(SURFACE);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            lbl.setVerticalAlignment(JLabel.CENTER);
            return lbl;
        }
    }

    /** Tiêu đề cột hai dòng ("Kiểm tra gần / nhất"). */
    static final class WrappingHeaderRenderer extends HeaderRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, focused, row, column);
            int width = tbl.getColumnModel().getColumn(column).getWidth() - 24;
            lbl.setText(wrapHtml(String.valueOf(value), lbl.getFontMetrics(lbl.getFont()), width));
            return lbl;
        }
    }

    /** Ngày kiểm tra gần nhất; đỏ khi đã đến hạn kiểm tra lại. */
    private static final class LastCheckRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            EmergencyKitRowResponse r = (EmergencyKitRowResponse) value;
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, "", false, false, row, column);
            lbl.setFont(tbl.getFont());
            lbl.setBackground(SURFACE);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            if (r.kiemTraGanNhat() == null) {
                lbl.setText("Chưa kiểm tra");
                lbl.setForeground(RED);
                lbl.setToolTipText("Hộp chưa được kiểm tra lần nào.");
            } else {
                lbl.setText(InventoryFormat.date(r.kiemTraGanNhat().toLocalDate()));
                lbl.setForeground(r.denHanKiemTra() ? RED : TEXT);
                lbl.setToolTipText(r.denHanKiemTra()
                        ? "Đã đến hạn kiểm tra lại (hạn " + InventoryFormat.date(r.hanKiemTraTiep()) + ")."
                        : "Kiểm tra lại trước " + InventoryFormat.date(r.hanKiemTraTiep()) + ".");
            }
            return lbl;
        }
    }

    /** Các liên kết dạng "[Chi tiết] [Kiểm tra]" theo tình trạng hộp. */
    private static final class ActionLinksCell extends JComponent implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final int PADDING = 14;
        private static final int GAP = 10;
        private transient List<KitAction> actions = List.of();

        ActionLinksCell() {
            setFont(font(Font.PLAIN, 15));
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            actions = KitAction.forStatus(((EmergencyKitRowResponse) value).trangThai());
            return this;
        }

        private static String text(KitAction a) {
            return "[" + a.label + "]";
        }

        KitAction hitTest(List<KitAction> rowActions, int x, FontMetrics fm) {
            int cursor = PADDING;
            for (KitAction a : rowActions) {
                int w = fm.stringWidth(text(a));
                if (x >= cursor - 2 && x <= cursor + w + 2) {
                    return a;
                }
                cursor += w + GAP;
            }
            return null;
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
            for (KitAction a : actions) {
                g2.setColor(a == KitAction.REPLACE || a == KitAction.RESTOCK ? PRIMARY : TEXT);
                g2.drawString(text(a), cursor, baseline);
                cursor += fm.stringWidth(text(a)) + GAP;
            }
            g2.dispose();
        }
    }

    private static final class KitTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<EmergencyKitRowResponse> rows = List.of();
        private int offset;

        void setPage(List<EmergencyKitRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        EmergencyKitRowResponse rowAt(int row) {
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
            EmergencyKitRowResponse r = rows.get(row);
            LocalDate next = r.hanDungKeTiep();
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maHop();
                case COL_NAME -> r.tenHop();
                case COL_LOCATION -> r.viTri();
                case 4 -> r.soMatHangDat() + " / " + r.soMatHang();
                case 6 -> next == null ? "—" : InventoryFormat.date(next);
                case COL_STATUS -> r.trangThai();
                default -> r;
            };
        }
    }

    /** Đổi nội dung combo mà không bắn sự kiện lọc giữa chừng. */
    static final class ActionGuard {
        private ActionGuard() {
        }

        static void run(JComboBox<?> cmb, Runnable change) {
            var listeners = cmb.getActionListeners();
            for (var l : listeners) {
                cmb.removeActionListener(l);
            }
            try {
                change.run();
            } finally {
                for (var l : listeners) {
                    cmb.addActionListener(l);
                }
            }
        }
    }
}
