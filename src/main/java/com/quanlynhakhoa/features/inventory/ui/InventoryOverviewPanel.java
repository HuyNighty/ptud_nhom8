package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.exception.PermissionDeniedException;
import com.quanlynhakhoa.features.inventory.dto.response.AttentionNoticeResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ExpiringLotResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryActivityResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryOverviewResponse;
import com.quanlynhakhoa.features.inventory.dto.response.LowStockItemResponse;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Màn hình "Quản lý kho — Tổng quan". Là vùng nội dung bên trong {@link InventoryWorkspacePanel}.
 *
 * <p>Gọi {@link InventoryOverviewService} qua {@link SwingWorker} để không chặn EDT.
 * Role lấy từ {@code sessionRoles} do shell cung cấp từ phiên đăng nhập.</p>
 */
public final class InventoryOverviewPanel extends JPanel {

    private static final String CARD_LOADING = "loading";
    private static final String CARD_ERROR = "error";
    private static final String CARD_CONTENT = "content";
    private static final int SIDE_COLUMN_WIDTH = 360;
    private static final String NO_DATA = "—";
    private static final int MAX_LOW_STOCK_ROWS = 6;
    private static final String NO_STOCK_CAPTION = "Chưa có dữ liệu lô hàng";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private enum ExpiryFilter {
        ALL("Tất cả", l -> true),
        WITHIN_7("≤ 7 ngày", l -> l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_7_NGAY),
        WITHIN_30("≤ 30 ngày", l -> l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_7_NGAY
                || l.mucCanhBao() == ExpiryLevel.CON_TOI_DA_30_NGAY),
        EXPIRED("Đã hết hạn", l -> l.mucCanhBao() == ExpiryLevel.DA_HET_HAN);

        final String label;
        final Predicate<ExpiringLotResponse> test;

        ExpiryFilter(String label, Predicate<ExpiringLotResponse> test) {
            this.label = label;
            this.test = test;
        }
    }

    private final InventoryOverviewService service;
    private final Supplier<Set<Role>> sessionRoles;
    private final CardLayout states = new CardLayout();
    private final JLabel lblErrorMessage = new JLabel("", SwingConstants.CENTER);
    private final JPanel pnlContentHost = new JPanel(new BorderLayout());
    private Consumer<InventoryQuickAction> quickActionHandler = this::showNotReady;
    private SwingWorker<InventoryOverviewResponse, Void> runningWorker;

    // Phần hạn sử dụng cần vẽ lại khi đổi bộ lọc.
    private List<ExpiringLotResponse> loadedLots = List.of();
    private boolean stockDataAvailable;
    private ExpiryFilter expiryFilter = ExpiryFilter.WITHIN_7;
    private final JPanel pnlExpiryTableHost = new JPanel(new BorderLayout());

    public InventoryOverviewPanel(InventoryOverviewService service, Supplier<Set<Role>> sessionRoles) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.sessionRoles = Objects.requireNonNull(sessionRoles, "sessionRoles must not be null");
        setLayout(states);
        setBackground(BACKGROUND);
        add(buildLoadingView(), CARD_LOADING);
        add(buildErrorView(), CARD_ERROR);
        pnlContentHost.setOpaque(false);
        add(pnlContentHost, CARD_CONTENT);
        pnlExpiryTableHost.setOpaque(false);
        reload();
    }

    /** Shell đăng ký handler để mở Panel Nhập/Xuất/... khi các Panel đó sẵn sàng. */
    public void setQuickActionHandler(Consumer<InventoryQuickAction> handler) {
        this.quickActionHandler = Objects.requireNonNull(handler, "handler must not be null");
    }

    public void reload() {
        if (runningWorker != null && !runningWorker.isDone()) {
            runningWorker.cancel(true);
        }
        states.show(this, CARD_LOADING);
        Set<Role> roles = sessionRoles.get();
        runningWorker = new SwingWorker<>() {
            @Override
            protected InventoryOverviewResponse doInBackground() {
                return service.getOverview(roles);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    render(get());
                    states.show(InventoryOverviewPanel.this, CARD_CONTENT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        };
        runningWorker.execute();
    }

    // ------------------------------------------------------------------ states

    private JComponent buildLoadingView() {
        JPanel pnlLoading = new JPanel(new GridBagLayout());
        pnlLoading.setBackground(BACKGROUND);
        JLabel lblLoading = new JLabel("Đang tải dữ liệu kho…");
        lblLoading.setFont(font(Font.PLAIN, 15));
        lblLoading.setForeground(TEXT_MUTED);
        pnlLoading.add(lblLoading);
        return pnlLoading;
    }

    private JComponent buildErrorView() {
        JPanel pnlError = new JPanel(new GridBagLayout());
        pnlError.setBackground(BACKGROUND);
        Card pnlErrorCard = new Card(new BorderLayout(0, 14), 24);
        lblErrorMessage.setFont(font(Font.PLAIN, 15));
        lblErrorMessage.setForeground(RED);
        pnlErrorCard.add(lblErrorMessage, BorderLayout.CENTER);
        JButton btnRetry = createButton("Thử lại", ButtonKind.OUTLINE);
        btnRetry.addActionListener(e -> reload());
        JPanel pnlErrorActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlErrorActions.setOpaque(false);
        pnlErrorActions.add(btnRetry);
        pnlErrorCard.add(pnlErrorActions, BorderLayout.SOUTH);
        pnlError.add(pnlErrorCard);
        return pnlError;
    }

    private void showError(Throwable cause) {
        String message = cause instanceof PermissionDeniedException
                ? "Tài khoản hiện tại không có quyền xem kho (INVENTORY_READ)."
                : "Không tải được dữ liệu kho. Vui lòng thử lại.";
        lblErrorMessage.setText(message);
        states.show(this, CARD_ERROR);
    }

    private void showNotReady(InventoryQuickAction action) {
        JOptionPane.showMessageDialog(this,
                "Màn hình \"" + action.label() + "\" chưa sẵn sàng.",
                "N08 Dental", JOptionPane.INFORMATION_MESSAGE);
    }

    // ----------------------------------------------------------------- content

    private void render(InventoryOverviewResponse data) {
        loadedLots = data.loTheoDoiHan();
        stockDataAvailable = data.coDuLieuTonKho();

        ScrollableColumn pnlPage = new ScrollableColumn();
        pnlPage.setLayout(new BorderLayout(0, 22));
        pnlPage.setBackground(BACKGROUND);
        pnlPage.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        JPanel pnlTop = new JPanel(new BorderLayout(0, 22));
        pnlTop.setOpaque(false);
        pnlTop.add(buildHeader(data), BorderLayout.NORTH);
        pnlTop.add(buildStats(data), BorderLayout.CENTER);
        pnlPage.add(pnlTop, BorderLayout.NORTH);

        JPanel pnlBody = new JPanel(new BorderLayout(22, 0));
        pnlBody.setOpaque(false);
        pnlBody.add(buildMainColumn(data), BorderLayout.CENTER);
        pnlBody.add(buildSideColumn(data), BorderLayout.EAST);
        pnlPage.add(pnlBody, BorderLayout.CENTER);

        JScrollPane scrPage = new JScrollPane(pnlPage);
        scrPage.setBorder(BorderFactory.createEmptyBorder());
        scrPage.getViewport().setBackground(BACKGROUND);
        scrPage.getVerticalScrollBar().setUnitIncrement(18);

        pnlContentHost.removeAll();
        pnlContentHost.add(scrPage, BorderLayout.CENTER);
        pnlContentHost.revalidate();
        pnlContentHost.repaint();
    }

    private JComponent buildHeader(InventoryOverviewResponse data) {
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JPanel pnlTitles = new JPanel();
        pnlTitles.setOpaque(false);
        pnlTitles.setLayout(new BoxLayout(pnlTitles, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Quản lý kho — Tổng quan");
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        JLabel lblSubtitle = new JLabel("Không gian làm việc của bạn · " + DATE.format(data.ngayXem()));
        lblSubtitle.setFont(font(Font.PLAIN, 15));
        lblSubtitle.setForeground(TEXT_MUTED);
        pnlTitles.add(lblTitle);
        pnlTitles.add(Box.createVerticalStrut(6));
        pnlTitles.add(lblSubtitle);
        pnlHeader.add(pnlTitles, BorderLayout.CENTER);

        JPanel pnlHeaderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlHeaderActions.setOpaque(false);
        JButton btnRefresh = createButton("Làm mới", ButtonKind.OUTLINE);
        btnRefresh.addActionListener(e -> reload());
        pnlHeaderActions.add(btnRefresh);
        pnlHeader.add(pnlHeaderActions, BorderLayout.EAST);
        return pnlHeader;
    }

    private JComponent buildStats(InventoryOverviewResponse data) {
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 18, 0));
        pnlStats.setOpaque(false);
        boolean stock = data.coDuLieuTonKho();
        pnlStats.add(statCard("Tổng mặt hàng", String.valueOf(data.tongMatHang()), NUMBER_BLUE,
                data.soThuoc() + " thuốc · " + data.soVatTu() + " vật tư"));
        pnlStats.add(statCard("Sắp hết tồn", stock ? String.valueOf(data.soMatHangSapHetTon()) : NO_DATA,
                ORANGE, stock ? "Cần bổ sung" : NO_STOCK_CAPTION));
        pnlStats.add(statCard("Sắp hết hạn", stock ? String.valueOf(data.soLoCanXuLyGap()) : NO_DATA,
                RED, stock ? "Cần xử lý gấp" : NO_STOCK_CAPTION));
        pnlStats.add(statCard("Phiếu cần xử lý", stock ? String.valueOf(data.soPhieuChoXuLy()) : NO_DATA,
                GREEN, stock ? "Phiếu nhập / xuất chờ xử lý" : "Chưa có chức năng phiếu kho"));
        return pnlStats;
    }

    private JComponent statCard(String title, String value, Color color, String caption) {
        Card pnlStatCard = new Card(null, 20);
        pnlStatCard.setLayout(new BoxLayout(pnlStatCard, BoxLayout.Y_AXIS));
        JLabel lblStatTitle = new JLabel(title);
        lblStatTitle.setFont(font(Font.BOLD, 16));
        lblStatTitle.setForeground(TEXT);
        JLabel lblStatValue = new JLabel(value);
        lblStatValue.setFont(font(Font.BOLD, 46));
        boolean noData = NO_DATA.equals(value);
        lblStatValue.setForeground(noData ? BORDER_STRONG : color);
        JLabel lblStatCaption = new JLabel(caption);
        lblStatCaption.setFont(font(Font.PLAIN, 15));
        lblStatCaption.setForeground(noData ? TEXT_MUTED : TEXT);
        pnlStatCard.add(lblStatTitle);
        pnlStatCard.add(Box.createVerticalStrut(4));
        pnlStatCard.add(lblStatValue);
        pnlStatCard.add(lblStatCaption);
        return pnlStatCard;
    }

    private JComponent buildMainColumn(InventoryOverviewResponse data) {
        JPanel pnlMainColumn = new JPanel();
        pnlMainColumn.setOpaque(false);
        pnlMainColumn.setLayout(new BoxLayout(pnlMainColumn, BoxLayout.Y_AXIS));
        pnlMainColumn.add(buildLowStockCard(data.matHangSapHetTon()));
        pnlMainColumn.add(Box.createVerticalStrut(22));
        pnlMainColumn.add(buildExpiryCard());
        // Giữ chiều cao tự nhiên, không kéo giãn theo cột phải.
        JPanel pnlMainWrapper = new JPanel(new BorderLayout());
        pnlMainWrapper.setOpaque(false);
        pnlMainWrapper.add(pnlMainColumn, BorderLayout.NORTH);
        return pnlMainWrapper;
    }

    private JComponent buildLowStockCard(List<LowStockItemResponse> allItems) {
        List<LowStockItemResponse> items = allItems.subList(0, Math.min(MAX_LOW_STOCK_ROWS, allItems.size()));
        Card pnlLowStockCard = new Card(new BorderLayout(), 0);
        JPanel pnlLowStockHead = new JPanel(new BorderLayout());
        pnlLowStockHead.setOpaque(false);
        pnlLowStockHead.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        pnlLowStockHead.add(sectionTitle("VẬT TƯ SẮP HẾT TỒN"), BorderLayout.WEST);
        JButton btnViewStock = createButton(InventoryQuickAction.XEM_TON_KHO.label(), ButtonKind.OUTLINE);
        btnViewStock.addActionListener(e -> quickActionHandler.accept(InventoryQuickAction.XEM_TON_KHO));
        pnlLowStockHead.add(btnViewStock, BorderLayout.EAST);
        pnlLowStockCard.add(pnlLowStockHead, BorderLayout.NORTH);

        if (items.isEmpty()) {
            pnlLowStockCard.add(emptyState(stockDataAvailable
                    ? "Không có mặt hàng dưới hoặc sát định mức."
                    : "Chưa có dữ liệu tồn kho — cần chức năng lô hàng / nhập kho."), BorderLayout.CENTER);
            return pnlLowStockCard;
        }
        String[] columns = {"Mã", "Tên", "Tồn", "Định mức", "Trạng thái"};
        AbstractTableModel model = new ReadOnlyModel(columns, items.size()) {
            @Override
            public Object getValueAt(int row, int col) {
                LowStockItemResponse i = items.get(row);
                return switch (col) {
                    case 0 -> i.maMatHang();
                    case 1 -> i.tenMatHang();
                    case 2 -> i.soLuongTon();
                    case 3 -> i.dinhMucToiThieu();
                    default -> i.mucTon();
                };
            }
        };
        JTable tblLowStock = styledTable(model, new int[] {90, 260, 90, 110, 140});
        tblLowStock.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        pnlLowStockCard.add(tableBlock(tblLowStock, 0), BorderLayout.CENTER);
        return pnlLowStockCard;
    }

    private JComponent buildExpiryCard() {
        Card pnlExpiryCard = new Card(new BorderLayout(), 0);
        JPanel pnlExpiryHead = new JPanel();
        pnlExpiryHead.setOpaque(false);
        pnlExpiryHead.setLayout(new BoxLayout(pnlExpiryHead, BoxLayout.Y_AXIS));
        pnlExpiryHead.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        JLabel lblExpiryTitle = sectionTitle("THEO DÕI HẠN SỬ DỤNG");
        lblExpiryTitle.setAlignmentX(LEFT_ALIGNMENT);
        pnlExpiryHead.add(lblExpiryTitle);
        pnlExpiryHead.add(Box.createVerticalStrut(14));

        ExpiryFilter[] filters = ExpiryFilter.values();
        JToggleButton[] tglExpiryFilters = new JToggleButton[filters.length];
        JToggleButton tglSelected = null;
        for (int i = 0; i < filters.length; i++) {
            ExpiryFilter filter = filters[i];
            long count = loadedLots.stream().filter(filter.test).count();
            tglExpiryFilters[i] = createSegment(filter.label + " (" + count + ")");
            tglExpiryFilters[i].addActionListener(e -> {
                expiryFilter = filter;
                refreshExpiryTable();
            });
            if (filter == expiryFilter) {
                tglSelected = tglExpiryFilters[i];
            }
        }
        JPanel pnlExpiryFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlExpiryFilters.setOpaque(false);
        pnlExpiryFilters.setAlignmentX(LEFT_ALIGNMENT);
        pnlExpiryFilters.add(createSegmentBar(tglSelected, tglExpiryFilters));
        pnlExpiryHead.add(pnlExpiryFilters);
        pnlExpiryCard.add(pnlExpiryHead, BorderLayout.NORTH);
        pnlExpiryCard.add(pnlExpiryTableHost, BorderLayout.CENTER);
        refreshExpiryTable();
        return pnlExpiryCard;
    }

    private void refreshExpiryTable() {
        List<ExpiringLotResponse> rows = loadedLots.stream().filter(expiryFilter.test).toList();
        pnlExpiryTableHost.removeAll();
        if (rows.isEmpty()) {
            pnlExpiryTableHost.add(emptyState(stockDataAvailable
                    ? "Không có lô nào thuộc bộ lọc này."
                    : "Chưa có dữ liệu lô hàng để theo dõi hạn sử dụng."), BorderLayout.CENTER);
        } else {
            String[] columns = {"Số lô", "Sản phẩm", "Hạn sử dụng", "SL", "Cảnh báo"};
            AbstractTableModel model = new ReadOnlyModel(columns, rows.size()) {
                @Override
                public Object getValueAt(int row, int col) {
                    ExpiringLotResponse l = rows.get(row);
                    return switch (col) {
                        case 0 -> l.soLo();
                        case 1 -> l.tenSanPham();
                        case 2 -> DATE.format(l.hanSuDung());
                        case 3 -> l.soLuong() + " " + l.donViTinh();
                        default -> l.mucCanhBao();
                    };
                }
            };
            JTable tblExpiry = styledTable(model, new int[] {90, 240, 130, 100, 140});
            tblExpiry.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
            pnlExpiryTableHost.add(tableBlock(tblExpiry, 22), BorderLayout.CENTER);
        }
        pnlExpiryTableHost.revalidate();
        pnlExpiryTableHost.repaint();
    }

    private JComponent buildSideColumn(InventoryOverviewResponse data) {
        JPanel pnlSideColumn = new JPanel(new GridBagLayout());
        pnlSideColumn.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 18, 0);
        pnlSideColumn.add(buildQuickActions(), gc);
        pnlSideColumn.add(buildActivity(data.hoatDongHomNay(), data.coDuLieuTonKho()), gc);
        gc.insets = new Insets(0, 0, 0, 0);
        pnlSideColumn.add(buildNotices(data.canChuY()), gc);

        // Cột phải rộng cố định, cao theo nội dung (không kéo giãn theo cột trái).
        JPanel pnlSideWrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(SIDE_COLUMN_WIDTH, pnlSideColumn.getPreferredSize().height);
            }
        };
        pnlSideWrapper.setOpaque(false);
        pnlSideWrapper.add(pnlSideColumn, BorderLayout.NORTH);
        return pnlSideWrapper;
    }

    private JComponent buildQuickActions() {
        Card pnlQuickActions = new Card(new BorderLayout(0, 14), 22);
        JPanel pnlQuickButtons = new JPanel(new GridLayout(0, 1, 0, 10));
        pnlQuickButtons.setOpaque(false);
        pnlQuickActions.add(sectionTitle("THAO TÁC NHANH"), BorderLayout.NORTH);
        pnlQuickButtons.add(quickButton("+  " + InventoryQuickAction.NHAP_KHO.label(), InventoryQuickAction.NHAP_KHO, ButtonKind.PRIMARY));
        pnlQuickButtons.add(quickButton("−  " + InventoryQuickAction.XUAT_KHO.label(), InventoryQuickAction.XUAT_KHO, ButtonKind.PRIMARY));
        pnlQuickButtons.add(quickButton(InventoryQuickAction.HOAN_TRA_KHO.label(), InventoryQuickAction.HOAN_TRA_KHO, ButtonKind.OUTLINE));
        pnlQuickButtons.add(quickButton(InventoryQuickAction.KIEM_KE_KHO.label(), InventoryQuickAction.KIEM_KE_KHO, ButtonKind.OUTLINE));
        pnlQuickButtons.add(quickButton(InventoryQuickAction.HUY_HAO_HUT.label(), InventoryQuickAction.HUY_HAO_HUT, ButtonKind.OUTLINE));
        pnlQuickActions.add(pnlQuickButtons, BorderLayout.CENTER);
        return pnlQuickActions;
    }

    private JButton quickButton(String text, InventoryQuickAction action, ButtonKind kind) {
        JButton btnQuick = createButton(text, kind);
        btnQuick.setFont(font(kind == ButtonKind.PRIMARY ? Font.BOLD : Font.PLAIN, 15));
        btnQuick.addActionListener(e -> quickActionHandler.accept(action));
        return btnQuick;
    }

    private JComponent buildActivity(InventoryActivityResponse a, boolean available) {
        Card pnlActivity = new Card(new BorderLayout(0, 12), 22);
        pnlActivity.add(sectionTitle("HOẠT ĐỘNG KHO HÔM NAY"), BorderLayout.NORTH);
        JPanel pnlActivityGrid = new JPanel(new GridLayout(2, 2, 12, 8));
        pnlActivityGrid.setOpaque(false);
        pnlActivityGrid.add(bodyLabel("Nhập kho: " + valueOrDash(available ? a.nhapKho() : null)));
        pnlActivityGrid.add(bodyLabel("Xuất kho: " + valueOrDash(available ? a.xuatKho() : null)));
        pnlActivityGrid.add(bodyLabel("Hoàn trả: " + valueOrDash(available ? a.hoanTra() : null)));
        pnlActivityGrid.add(bodyLabel("Hủy / hao hụt: " + valueOrDash(available ? a.huyHaoHut() : null)));
        pnlActivity.add(pnlActivityGrid, BorderLayout.CENTER);
        return pnlActivity;
    }

    private static String valueOrDash(Integer value) {
        return value == null ? NO_DATA : String.valueOf(value);
    }

    private JComponent buildNotices(List<AttentionNoticeResponse> notices) {
        Card pnlNotices = new Card(new BorderLayout(0, 12), 22);
        pnlNotices.add(sectionTitle("CẦN CHÚ Ý"), BorderLayout.NORTH);
        JPanel pnlNoticeList = new JPanel(new GridLayout(0, 1, 0, 8));
        pnlNoticeList.setOpaque(false);
        if (notices.isEmpty()) {
            pnlNoticeList.add(bodyLabel("Không có cảnh báo."));
        }
        for (AttentionNoticeResponse n : notices) {
            Color color = switch (n.mucDo()) {
                case CRITICAL -> RED;
                case WARNING -> ORANGE;
                case INFO -> NUMBER_BLUE;
            };
            JLabel lblNotice = bodyLabel("<html>" + escape(n.noiDung()) + "</html>");
            lblNotice.setIcon(new DotIcon(color, 11));
            lblNotice.setIconTextGap(10);
            lblNotice.setVerticalAlignment(SwingConstants.TOP);
            lblNotice.setVerticalTextPosition(SwingConstants.TOP);
            lblNotice.setVerticalTextPosition(SwingConstants.CENTER);
            pnlNoticeList.add(lblNotice);
        }
        pnlNotices.add(pnlNoticeList, BorderLayout.CENTER);
        return pnlNotices;
    }

    // ----------------------------------------------------------------- helpers

    private static JLabel sectionTitle(String text) {
        JLabel lblSection = new JLabel(text);
        lblSection.setFont(font(Font.BOLD, 15));
        lblSection.setForeground(TEXT);
        return lblSection;
    }

    private static JLabel bodyLabel(String text) {
        JLabel lblBody = new JLabel(text);
        lblBody.setFont(font(Font.PLAIN, 15));
        lblBody.setForeground(TEXT);
        return lblBody;
    }

    private static JComponent emptyState(String text) {
        JLabel lblEmpty = new JLabel(text, SwingConstants.CENTER);
        lblEmpty.setFont(font(Font.PLAIN, 14));
        lblEmpty.setForeground(TEXT_MUTED);
        lblEmpty.setBorder(BorderFactory.createEmptyBorder(24, 22, 28, 22));
        return lblEmpty;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JTable styledTable(AbstractTableModel model, int[] widths) {
        JTable tbl = new JTable(model);
        tbl.setFont(font(Font.PLAIN, 15));
        tbl.setForeground(TEXT);
        tbl.setRowHeight(46);
        tbl.setShowVerticalLines(false);
        tbl.setShowHorizontalLines(true);
        tbl.setGridColor(BORDER);
        tbl.setIntercellSpacing(new Dimension(0, 1));
        tbl.setFocusable(false);
        tbl.setRowSelectionAllowed(false);
        tbl.setBackground(SURFACE);
        tbl.setDefaultRenderer(Object.class, paddedRenderer());
        tbl.setDefaultRenderer(Integer.class, paddedRenderer());

        JTableHeader tbhHeader = tbl.getTableHeader();
        tbhHeader.setReorderingAllowed(false);
        tbhHeader.setResizingAllowed(false);
        tbhHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lblHeaderCell = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                lblHeaderCell.setFont(font(Font.PLAIN, 15));
                lblHeaderCell.setForeground(TEXT);
                lblHeaderCell.setBackground(HEADER_ROW);
                lblHeaderCell.setOpaque(true);
                lblHeaderCell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 22, 0, 8)));
                return lblHeaderCell;
            }
        });
        tbhHeader.setPreferredSize(new Dimension(10, 46));

        TableColumnModel colModel = tbl.getColumnModel();
        for (int i = 0; i < widths.length; i++) {
            colModel.getColumn(i).setPreferredWidth(widths[i]);
        }
        return tbl;
    }

    private static DefaultTableCellRenderer paddedRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int c) {
                JLabel lblCell = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, c);
                lblCell.setHorizontalAlignment(SwingConstants.LEFT);
                lblCell.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 8));
                return lblCell;
            }
        };
        return renderer;
    }

    /** Header + bảng không bọc JScrollPane để bảng cao đúng số dòng như thiết kế. */
    private static JComponent tableBlock(JTable table, int sideInset) {
        JPanel pnlTableBlock = new JPanel(new BorderLayout());
        pnlTableBlock.setOpaque(false);
        pnlTableBlock.setBorder(BorderFactory.createEmptyBorder(0, sideInset, sideInset > 0 ? 18 : 12, sideInset));
        pnlTableBlock.add(table.getTableHeader(), BorderLayout.NORTH);
        pnlTableBlock.add(table, BorderLayout.CENTER);
        return pnlTableBlock;
    }

    private abstract static class ReadOnlyModel extends AbstractTableModel {
        private final String[] columns;
        private final int rows;

        ReadOnlyModel(String[] columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /** Chấm màu + nhãn cho {@link StockLevel} và {@link ExpiryLevel}. */
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object v, boolean s, boolean f, int r, int c) {
            JLabel lblStatus = (JLabel) super.getTableCellRendererComponent(tbl, v, false, false, r, c);
            lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 8));
            lblStatus.setIconTextGap(10);
            if (v instanceof StockLevel level) {
                lblStatus.setText(level.label());
                lblStatus.setIcon(new DotIcon(switch (level) {
                    case HET_HANG, THIEU -> RED;
                    case SAP_HET -> ORANGE;
                    case DU -> GREEN;
                }, 12));
            } else if (v instanceof ExpiryLevel level) {
                lblStatus.setText(level.label());
                Color color = switch (level) {
                    case DA_HET_HAN -> RED;
                    case CON_TOI_DA_7_NGAY -> ORANGE;
                    case CON_TOI_DA_30_NGAY -> AMBER;
                    case CON_HAN -> GREEN;
                };
                lblStatus.setIcon(new DotIcon(color, 12));
            }
            return lblStatus;
        }
    }

    /** Nội dung co theo chiều ngang viewport, chỉ cuộn dọc. */
    private static final class ScrollableColumn extends JPanel implements Scrollable {
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
