package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalSummaryResponse;
import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.DisposalService;
import com.quanlynhakhoa.features.inventory.ui.VoucherTable.ButtonActionCell;
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
import javax.swing.ButtonGroup;
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
 * Màn hình "Hủy / hao hụt" (UC35): thống kê, lọc, danh sách phiếu hủy; lập phiếu (ưu tiên lô hết hạn),
 * duyệt bởi người khác người lập (trừ tồn), in phiếu, hủy phiếu.
 */
public final class DisposalPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int PAGE_SIZE = 8;
    static final String PENDING_LABEL = "Chờ duyệt";
    private static final String[] COLUMNS = {"STT", "Mã phiếu", "Ngày tạo", "Lý do", "Người yêu cầu", "Người duyệt",
            "Giá trị", "Trạng thái", "Thao tác"};
    private static final int COL_VALUE = 6;
    private static final int COL_STATUS = 7;
    private static final int COL_ACTIONS = 8;
    private static final String ALL_REASONS = "Lý do hủy";

    private final transient DisposalService service;
    private final transient Supplier<InventoryActor> session;

    private final JLabel lblTotalValue = statValue();
    private final JLabel lblAmountValue = statValue();
    private final JLabel lblPendingValue = statValue();
    private final JLabel lblDoneValue = statValue();

    private final HintTextField txtSearch = new HintTextField("Tìm kiếm mã phiếu, người yêu cầu, hàng hóa...", 24);
    private final JButton btnExport = createButton("Xuất Excel", ButtonKind.OUTLINE);
    private final JButton btnCreate = createButton("Tạo phiếu hủy", ButtonKind.PRIMARY);
    private final JToggleButton tglAll = createPill("Tất cả");
    private final JToggleButton tglPending = createPill(PENDING_LABEL);
    private final JToggleButton tglDone = createPill("Đã hoàn thành");
    private final JComboBox<Object> cmbReason = new JComboBox<>();
    private final JComboBox<TimeRange> cmbTimeRange = new JComboBox<>(TimeRange.values());
    private final Timer searchDelay = new Timer(300, e -> applyFilters());

    private final DisposalTableModel tableModel = new DisposalTableModel();
    private final JTable tblDisposals = new JTable(tableModel);
    private final ButtonActionCell actionCell = new ButtonActionCell("Duyệt", COL_STATUS, RowAction.PRINT);
    private final JLabel lblPageInfo = new JLabel(" ");
    private final Pager pnlPager = new Pager();

    private transient List<DisposalRowResponse> allRows = List.of();
    private transient List<DisposalRowResponse> filteredRows = List.of();
    private int currentPage;
    private transient SwingWorker<LoadResult, Void> runningLoad;

    private record LoadResult(DisposalSummaryResponse summary, List<DisposalRowResponse> rows) {
    }

    public DisposalPanel(DisposalService service, Supplier<InventoryActor> session) {
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
        addFullWidth(pnlPage, buildToolbar());
        pnlPage.add(Box.createVerticalStrut(14));
        addFullWidth(pnlPage, buildFilters());
        pnlPage.add(Box.createVerticalStrut(20));
        addFullWidth(pnlPage, sectionTitle("DANH SÁCH PHIẾU HỦY / HAO HỤT"));
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

    /** Tải lại thống kê và danh sách; giữ nguyên bộ lọc đang chọn. */
    public void reload() {
        if (runningLoad != null && !runningLoad.isDone()) {
            runningLoad.cancel(true);
        }
        Set<Role> roles = session.get().roles();
        lblPageInfo.setForeground(TEXT_MUTED);
        lblPageInfo.setText("Đang tải phiếu hủy…");
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

    private static JComponent sectionTitle(String text) {
        JLabel lblSection = new JLabel(text);
        lblSection.setFont(font(Font.BOLD, 16));
        lblSection.setForeground(TEXT);
        JPanel pnlSection = new JPanel(new BorderLayout());
        pnlSection.setOpaque(false);
        pnlSection.add(lblSection, BorderLayout.WEST);
        pnlSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblSection.getPreferredSize().height));
        return pnlSection;
    }

    private static JComponent buildHeader() {
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Hủy / hao hụt");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Quản lý các phiếu hủy và ghi nhận hao hụt vật tư, thuốc quá hạn, hỏng hóc "
                + "hoặc mất mát trong quá trình vận hành.");
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
        pnlStats.add(statCard("Tổng phiếu hủy", null, lblTotalValue, "Phiếu hủy"));
        pnlStats.add(statCard("Giá trị hủy tháng này", null, lblAmountValue, "Tổng giá trị đã duyệt"));
        pnlStats.add(statCard("Chờ duyệt", InventoryFormat.statusDot(VoucherStatus.CHO_XU_LY), lblPendingValue,
                "Cần duyệt hủy"));
        pnlStats.add(statCard("Đã hoàn thành", InventoryFormat.statusDot(VoucherStatus.DA_HOAN_THANH), lblDoneValue,
                "Đã xử lý hủy"));
        return pnlStats;
    }

    private static JLabel statValue() {
        JLabel lblValue = new JLabel("—");
        lblValue.setFont(font(Font.BOLD, 36));
        lblValue.setForeground(TEXT);
        return lblValue;
    }

    private static Card statCard(String title, Color dot, JLabel lblValue, String caption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.PLAIN, 17));
        lblStatTitle.setForeground(TEXT);
        if (dot != null) {
            lblStatTitle.setIcon(new DotIcon(dot, 11));
            lblStatTitle.setIconTextGap(8);
        }
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

    private JComponent buildToolbar() {
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
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeft.setOpaque(false);
        pnlLeft.add(pnlSearch);

        btnExport.setIcon(new GlyphIcon(MenuGlyph.DOWNLOAD, 18, TEXT));
        btnExport.setIconTextGap(8);
        btnExport.setFont(font(Font.PLAIN, 15));
        btnCreate.setIcon(new GlyphIcon(MenuGlyph.PLUS, 18, Color.WHITE));
        btnCreate.setIconTextGap(8);
        btnCreate.setFont(font(Font.BOLD, 15));
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlRight.setOpaque(false);
        pnlRight.add(btnExport);
        pnlRight.add(btnCreate);

        JPanel pnlToolbar = new JPanel(new BorderLayout(12, 0));
        pnlToolbar.setOpaque(false);
        pnlToolbar.add(pnlLeft, BorderLayout.CENTER);
        pnlToolbar.add(pnlRight, BorderLayout.EAST);
        return pnlToolbar;
    }

    private JComponent buildFilters() {
        ButtonGroup grpStatus = new ButtonGroup();
        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlFilters.setOpaque(false);
        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglPending, tglDone}) {
            grpStatus.add(tgl);
            pnlFilters.add(tgl);
            pnlFilters.add(Box.createHorizontalStrut(12));
        }
        tglAll.setSelected(true);
        cmbReason.addItem(ALL_REASONS);
        Arrays.stream(DisposalReason.values()).forEach(cmbReason::addItem);
        styleCombo(cmbReason);
        cmbReason.setPreferredSize(new Dimension(200, 42));
        styleCombo(cmbTimeRange);
        cmbTimeRange.setPreferredSize(new Dimension(170, 42));
        pnlFilters.add(cmbReason);
        pnlFilters.add(Box.createHorizontalStrut(12));
        pnlFilters.add(cmbTimeRange);
        return pnlFilters;
    }

    private JComponent buildTableCard() {
        Card pnlCard = new Card(new BorderLayout(), 0);
        tblDisposals.setFont(font(Font.PLAIN, 15));
        tblDisposals.setForeground(TEXT);
        tblDisposals.setRowHeight(56);
        tblDisposals.setShowVerticalLines(false);
        tblDisposals.setGridColor(BORDER);
        tblDisposals.setRowSelectionAllowed(false);
        tblDisposals.setFocusable(false);
        tblDisposals.setBackground(SURFACE);
        tblDisposals.setDefaultRenderer(Object.class, new CellRenderer());
        tblDisposals.getColumnModel().getColumn(COL_VALUE).setCellRenderer(new MoneyRenderer());
        tblDisposals.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer(PENDING_LABEL));
        tblDisposals.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(actionCell);
        int[] widths = {62, 90, 110, 200, 145, 130, 120, 160, 200};
        for (int i = 0; i < widths.length; i++) {
            tblDisposals.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JTableHeader tbhDisposals = tblDisposals.getTableHeader();
        tbhDisposals.setReorderingAllowed(false);
        tbhDisposals.setResizingAllowed(false);
        tbhDisposals.setPreferredSize(new Dimension(10, 52));
        tbhDisposals.setDefaultRenderer(new MoneyHeaderRenderer());
        JPanel pnlGrid = new JPanel(new BorderLayout());
        pnlGrid.setOpaque(false);
        pnlGrid.setBorder(BorderFactory.createEmptyBorder(1, 1, 6, 1));
        pnlGrid.add(tbhDisposals, BorderLayout.NORTH);
        pnlGrid.add(tblDisposals, BorderLayout.CENTER);
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
        cmbReason.addActionListener(e -> applyFilters());
        cmbTimeRange.addActionListener(e -> applyFilters());
        for (JToggleButton tgl : new JToggleButton[] {tglAll, tglPending, tglDone}) {
            tgl.addActionListener(e -> applyFilters());
        }
        btnCreate.addActionListener(e -> openEditor(null));
        btnExport.addActionListener(e -> exportExcel());

        MouseAdapter clicks = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblDisposals.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                DisposalRowResponse r = tableModel.rowAt(row);
                if (tblDisposals.columnAtPoint(e.getPoint()) == COL_ACTIONS) {
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
                int row = tblDisposals.rowAtPoint(e.getPoint());
                boolean hand = row >= 0 && tblDisposals.columnAtPoint(e.getPoint()) == COL_ACTIONS
                        && hit(row, e.getX(), tableModel.rowAt(row).trangThai()) != null;
                tblDisposals.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        tblDisposals.addMouseListener(clicks);
        tblDisposals.addMouseMotionListener(clicks);
    }

    private RowAction hit(int row, int x, VoucherStatus status) {
        Rectangle cell = tblDisposals.getCellRect(row, COL_ACTIONS, false);
        return actionCell.hitTest(status, x - cell.x, tblDisposals.getFontMetrics(actionCell.getFont()));
    }

    // ------------------------------------------------------------ data → view

    private void applySummary(DisposalSummaryResponse s) {
        lblTotalValue.setText(String.valueOf(s.tongPhieu()));
        lblAmountValue.setText(InventoryFormat.money(s.giaTriThangNay()));
        lblPendingValue.setText(String.valueOf(s.soChoDuyet()));
        lblDoneValue.setText(String.valueOf(s.soHoanThanh()));
    }

    static String statusLabel(VoucherStatus status) {
        return status == VoucherStatus.CHO_XU_LY ? PENDING_LABEL : status.label();
    }

    private void applyFilters() {
        searchDelay.stop();
        LocalDate today = LocalDate.now();
        String keyword = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        DisposalReason reason = cmbReason.getSelectedItem() instanceof DisposalReason r ? r : null;
        TimeRange range = (TimeRange) cmbTimeRange.getSelectedItem();
        List<DisposalRowResponse> matching = new ArrayList<>();
        int pending = 0;
        int done = 0;
        for (DisposalRowResponse r : allRows) {
            boolean matchesKeyword = keyword.isEmpty()
                    || r.maPhieu().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.nguoiYeuCau().toLowerCase(Locale.ROOT).contains(keyword)
                    || r.lyDoHienThi().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesReason = reason == null || r.lyDo() == reason;
            boolean matchesTime = range == null || range.contains(r.ngayHuy(), today);
            if (matchesKeyword && matchesReason && matchesTime) {
                matching.add(r);
                pending += r.trangThai() == VoucherStatus.CHO_XU_LY ? 1 : 0;
                done += r.trangThai() == VoucherStatus.DA_HOAN_THANH ? 1 : 0;
            }
        }
        tglAll.setText("Tất cả (" + matching.size() + ")");
        tglPending.setText(PENDING_LABEL + " (" + pending + ")");
        tglDone.setText("Đã hoàn thành (" + done + ")");
        VoucherStatus status = tglPending.isSelected() ? VoucherStatus.CHO_XU_LY
                : tglDone.isSelected() ? VoucherStatus.DA_HOAN_THANH : null;
        filteredRows = status == null ? List.copyOf(matching)
                : matching.stream().filter(r -> r.trangThai() == status).toList();
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
                ? (allRows.isEmpty() ? "Chưa có phiếu hủy nào — bấm \"Tạo phiếu hủy\" để bắt đầu."
                        : "Không có phiếu hủy nào khớp bộ lọc.")
                : "Hiển thị " + (from + 1) + " - " + to + " trong tổng số " + total + " phiếu hủy");
        pnlPager.update(currentPage, total == 0 ? 0 : pageCount);
    }

    // ----------------------------------------------------------------- actions

    private void perform(RowAction action, DisposalRowResponse row) {
        switch (action) {
            case VIEW -> openDetail(row.maPhieu());
            case PRINT -> runDetail(row.maPhieu(), d -> DisposalPrinter.print(this, d));
            case EDIT -> openEditor(row.maPhieu());
            case PROCESS -> runDetail(row.maPhieu(), this::approve);
        }
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    /** @param maPhieu null = lập phiếu mới */
    void openEditor(String maPhieu) {
        if (maPhieu == null) {
            showEditor(null);
        } else {
            runDetail(maPhieu, this::showEditor);
        }
    }

    private void showEditor(DisposalDetailResponse existing) {
        DisposalReceiptDialog dlgDisposal = new DisposalReceiptDialog(owner(), service, session, existing);
        dlgDisposal.setVisible(true);
        dlgDisposal.saved().ifPresent(saved -> {
            reload();
            showInfo("Đã lưu phiếu " + saved.maPhieu() + " — chờ một quản lý kho khác duyệt.");
        });
    }

    private void openDetail(String maPhieu) {
        runDetail(maPhieu, detail -> {
            DisposalDetailDialog dlgDetail = new DisposalDetailDialog(owner(), service, session, detail, this::openEditor);
            dlgDetail.setVisible(true);
            if (dlgDetail.changed()) {
                reload();
            }
        });
    }

    private void approve(DisposalDetailResponse detail) {
        if (Objects.equals(session.get().maNhanVien(), detail.maNhanVienLap())) {
            JOptionPane.showMessageDialog(this, "Phiếu " + detail.maPhieu() + " do chính người dùng hiện tại lập.\n"
                    + "Người duyệt phải khác người lập — hãy nhờ một quản lý kho khác duyệt.", "Duyệt phiếu hủy",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Duyệt phiếu hủy " + detail.maPhieu() + "?\n"
                        + detail.dong().size() + " lô · giá trị " + InventoryFormat.money(detail.giaTri())
                        + "\nTồn kho sẽ bị trừ ngay, không thể hoàn tác.",
                "Duyệt phiếu hủy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        InventoryActor actor = session.get();
        new SwingWorker<DisposalDetailResponse, Void>() {
            @Override
            protected DisposalDetailResponse doInBackground() {
                return service.approve(actor, detail.maPhieu());
            }

            @Override
            protected void done() {
                try {
                    get();
                    reload();
                    showInfo("Đã duyệt phiếu " + detail.maPhieu() + " — tồn kho đã được trừ.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void runDetail(String maPhieu, Consumer<DisposalDetailResponse> onLoaded) {
        Set<Role> roles = session.get().roles();
        new SwingWorker<DisposalDetailResponse, Void>() {
            @Override
            protected DisposalDetailResponse doInBackground() {
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

    private void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            lblPageInfo.setForeground(GREEN);
            lblPageInfo.setText(message);
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Hủy / hao hụt", JOptionPane.WARNING_MESSAGE);
    }

    private void exportExcel() {
        if (filteredRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Xuất Excel",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fchExport = new JFileChooser();
        fchExport.setDialogTitle("Xuất danh sách phiếu hủy");
        fchExport.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        fchExport.setSelectedFile(new File("phieu-huy-hao-hut.xlsx"));
        if (fchExport.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File chosen = fchExport.getSelectedFile();
        File target = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".xlsx");
        List<DisposalRowResponse> snapshot = filteredRows;
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
                    showInfo("Đã xuất " + snapshot.size() + " phiếu hủy ra " + target.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError("Không ghi được file: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    static void writeExcel(File target, List<DisposalRowResponse> rows) throws IOException {
        List<XlsxExport.Column> columns = List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Mã phiếu", 11, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày tạo", 13, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Lý do", 18, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Mô tả", 28, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người yêu cầu", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Người duyệt", 22, XlsxExport.Kind.TEXT),
                new XlsxExport.Column("Giá trị (đ)", 15, XlsxExport.Kind.MONEY),
                new XlsxExport.Column("Trạng thái", 16, XlsxExport.Kind.CENTER));
        List<List<Object>> data = new ArrayList<>();
        int stt = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (DisposalRowResponse r : rows) {
            data.add(Arrays.asList(stt++, r.maPhieu(), r.ngayHuy(), r.lyDo().label(), r.moTa(), r.nguoiYeuCau(),
                    r.nguoiDuyet(), r.giaTri(), statusLabel(r.trangThai())));
            if (r.trangThai() == VoucherStatus.DA_HOAN_THANH) {
                total = total.add(r.giaTri());
            }
        }
        XlsxExport.write(target, "Phieu huy", "DANH SÁCH PHIẾU HỦY / HAO HỤT",
                "N08 Dental · Xuất lúc " + InventoryFormat.dateTime(LocalDateTime.now()) + " · " + rows.size()
                        + " phiếu · Tổng giá trị đã duyệt hủy: " + InventoryFormat.money(total),
                columns, data);
    }

    // ------------------------------------------------------------------- table

    /** Cột "Giá trị" căn phải. */
    private static final class MoneyRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, false, false, row, column);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 18));
            lbl.setForeground(TEXT);
            return lbl;
        }
    }

    /** Tiêu đề bảng; cột "Giá trị" căn phải cho thẳng hàng với số. */
    private static final class MoneyHeaderRenderer extends HeaderRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, focused, row, column);
            lbl.setHorizontalAlignment(column == COL_VALUE ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (column == COL_VALUE) {
                lbl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 8, 0, 18)));
            }
            return lbl;
        }
    }

    private static final class DisposalTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<DisposalRowResponse> rows = List.of();
        private int offset;

        void setPage(List<DisposalRowResponse> pageRows, int startIndex) {
            rows = List.copyOf(pageRows);
            offset = startIndex;
            fireTableDataChanged();
        }

        DisposalRowResponse rowAt(int row) {
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
            DisposalRowResponse r = rows.get(row);
            return switch (column) {
                case 0 -> offset + row + 1;
                case 1 -> r.maPhieu();
                case 2 -> InventoryFormat.date(r.ngayHuy());
                case 3 -> r.lyDoHienThi();
                case 4 -> r.nguoiYeuCau();
                case 5 -> r.nguoiDuyet() == null ? "—" : r.nguoiDuyet();
                case 6 -> InventoryFormat.money(r.giaTri());
                default -> r.trangThai();
            };
        }
    }
}
