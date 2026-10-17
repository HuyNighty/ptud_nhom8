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
import javax.swing.ButtonGroup;
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
 * Màn hình "Quản lý kho — Tổng quan" (Figma QL kho). Chỉ là vùng nội dung;
 * sidebar/topbar thuộc MainFrame dùng chung.
 *
 * <p>Gọi {@link InventoryOverviewService} qua {@link SwingWorker} để không chặn EDT.
 * Role lấy từ {@code sessionRoles} do shell cung cấp từ phiên đăng nhập.</p>
 */
public final class InventoryOverviewPanel extends JPanel {

    private static final String CARD_LOADING = "loading";
    private static final String CARD_ERROR = "error";
    private static final String CARD_CONTENT = "content";
    private static final int SIDE_COLUMN_WIDTH = 360;
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
    private final JLabel errorMessage = new JLabel("", SwingConstants.CENTER);
    private final JPanel contentHost = new JPanel(new BorderLayout());
    private Consumer<InventoryQuickAction> quickActionHandler = this::showNotReady;
    private SwingWorker<InventoryOverviewResponse, Void> runningWorker;

    // Phần hạn sử dụng cần vẽ lại khi đổi bộ lọc.
    private List<ExpiringLotResponse> loadedLots = List.of();
    private ExpiryFilter expiryFilter = ExpiryFilter.WITHIN_7;
    private final JPanel expiryTableHost = new JPanel(new BorderLayout());

    public InventoryOverviewPanel(InventoryOverviewService service, Supplier<Set<Role>> sessionRoles) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.sessionRoles = Objects.requireNonNull(sessionRoles, "sessionRoles must not be null");
        setLayout(states);
        setBackground(BACKGROUND);
        add(buildLoadingView(), CARD_LOADING);
        add(buildErrorView(), CARD_ERROR);
        contentHost.setOpaque(false);
        add(contentHost, CARD_CONTENT);
        expiryTableHost.setOpaque(false);
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        JLabel label = new JLabel("Đang tải dữ liệu kho…");
        label.setFont(font(Font.PLAIN, 15));
        label.setForeground(TEXT_MUTED);
        panel.add(label);
        return panel;
    }

    private JComponent buildErrorView() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        Card card = new Card(new BorderLayout(0, 14), 24);
        errorMessage.setFont(font(Font.PLAIN, 15));
        errorMessage.setForeground(RED);
        card.add(errorMessage, BorderLayout.CENTER);
        JButton retry = button("Thử lại", ButtonKind.OUTLINE);
        retry.addActionListener(e -> reload());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        south.setOpaque(false);
        south.add(retry);
        card.add(south, BorderLayout.SOUTH);
        panel.add(card);
        return panel;
    }

    private void showError(Throwable cause) {
        String message = cause instanceof PermissionDeniedException
                ? "Tài khoản hiện tại không có quyền xem kho (INVENTORY_READ)."
                : "Không tải được dữ liệu kho. Vui lòng thử lại.";
        errorMessage.setText(message);
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

        ScrollableColumn page = new ScrollableColumn();
        page.setLayout(new BorderLayout(0, 22));
        page.setBackground(BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        JPanel top = new JPanel(new BorderLayout(0, 22));
        top.setOpaque(false);
        top.add(buildHeader(data), BorderLayout.NORTH);
        top.add(buildStats(data), BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(22, 0));
        body.setOpaque(false);
        body.add(buildMainColumn(data), BorderLayout.CENTER);
        body.add(buildSideColumn(data), BorderLayout.EAST);
        page.add(body, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        contentHost.removeAll();
        contentHost.add(scroll, BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
    }

    private JComponent buildHeader(InventoryOverviewResponse data) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý kho — Tổng quan");
        title.setFont(font(Font.BOLD, 30));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Không gian làm việc của bạn · " + DATE.format(data.ngayXem()));
        subtitle.setFont(font(Font.PLAIN, 15));
        subtitle.setForeground(TEXT_MUTED);
        titles.add(title);
        titles.add(Box.createVerticalStrut(6));
        titles.add(subtitle);
        header.add(titles, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        if (data.prototypeData()) {
            JLabel badge = new JLabel("PROTOTYPE · dữ liệu mẫu, chưa đọc SQL Server");
            badge.setOpaque(true);
            badge.setBackground(PROTOTYPE_FILL);
            badge.setForeground(PROTOTYPE_TEXT);
            badge.setFont(font(Font.BOLD, 12));
            badge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            right.add(badge);
        }
        JButton refresh = button("Làm mới", ButtonKind.OUTLINE);
        refresh.addActionListener(e -> reload());
        right.add(refresh);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildStats(InventoryOverviewResponse data) {
        JPanel row = new JPanel(new GridLayout(1, 4, 18, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng mặt hàng", data.tongMatHang(), NUMBER_BLUE, "Vật tư & thuốc"));
        row.add(statCard("Sắp hết tồn", data.soMatHangSapHetTon(), ORANGE, "Cần bổ sung"));
        row.add(statCard("Sắp hết hạn", data.soLoCanXuLyGap(), RED, "Cần xử lý gấp"));
        row.add(statCard("Phiếu cần xử lý", data.soPhieuChoXuLy(), GREEN, "Chờ nhập / xuất / kiểm kê"));
        return row;
    }

    private JComponent statCard(String title, int value, Color color, String caption) {
        Card card = new Card(null, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(font(Font.BOLD, 16));
        t.setForeground(TEXT);
        JLabel v = new JLabel(String.valueOf(value));
        v.setFont(font(Font.BOLD, 46));
        v.setForeground(color);
        JLabel c = new JLabel(caption);
        c.setFont(font(Font.PLAIN, 15));
        c.setForeground(TEXT);
        card.add(t);
        card.add(Box.createVerticalStrut(4));
        card.add(v);
        card.add(c);
        return card;
    }

    private JComponent buildMainColumn(InventoryOverviewResponse data) {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(buildLowStockCard(data.matHangSapHetTon()));
        column.add(Box.createVerticalStrut(22));
        column.add(buildExpiryCard());
        // Giữ chiều cao tự nhiên, không kéo giãn theo cột phải.
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(column, BorderLayout.NORTH);
        return wrapper;
    }

    private JComponent buildLowStockCard(List<LowStockItemResponse> items) {
        Card card = new Card(new BorderLayout(), 0);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        head.add(sectionTitle("VẬT TƯ SẮP HẾT TỒN"), BorderLayout.WEST);
        JButton viewStock = button(InventoryQuickAction.XEM_TON_KHO.label(), ButtonKind.OUTLINE);
        viewStock.addActionListener(e -> quickActionHandler.accept(InventoryQuickAction.XEM_TON_KHO));
        head.add(viewStock, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        if (items.isEmpty()) {
            card.add(emptyState("Không có mặt hàng dưới hoặc sát định mức."), BorderLayout.CENTER);
            return card;
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
        JTable table = styledTable(model, new int[] {90, 260, 90, 110, 140});
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        card.add(tableBlock(table, 0), BorderLayout.CENTER);
        return card;
    }

    private JComponent buildExpiryCard() {
        Card card = new Card(new BorderLayout(), 0);
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));
        JLabel title = sectionTitle("THEO DÕI HẠN SỬ DỤNG");
        title.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title);
        head.add(Box.createVerticalStrut(14));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup group = new ButtonGroup();
        for (ExpiryFilter filter : ExpiryFilter.values()) {
            long count = loadedLots.stream().filter(filter.test).count();
            JToggleButton chip = chip(filter.label + " (" + count + ")");
            chip.setSelected(filter == expiryFilter);
            chip.addActionListener(e -> {
                expiryFilter = filter;
                refreshExpiryTable();
            });
            group.add(chip);
            chips.add(chip);
        }
        head.add(chips);
        card.add(head, BorderLayout.NORTH);
        card.add(expiryTableHost, BorderLayout.CENTER);
        refreshExpiryTable();
        return card;
    }

    private void refreshExpiryTable() {
        List<ExpiringLotResponse> rows = loadedLots.stream().filter(expiryFilter.test).toList();
        expiryTableHost.removeAll();
        if (rows.isEmpty()) {
            expiryTableHost.add(emptyState("Không có lô nào thuộc bộ lọc này."), BorderLayout.CENTER);
        } else {
            String[] columns = {"Mã lô", "Sản phẩm", "Hạn sử dụng", "SL", "Cảnh báo"};
            AbstractTableModel model = new ReadOnlyModel(columns, rows.size()) {
                @Override
                public Object getValueAt(int row, int col) {
                    ExpiringLotResponse l = rows.get(row);
                    return switch (col) {
                        case 0 -> l.maLo();
                        case 1 -> l.tenSanPham();
                        case 2 -> DATE.format(l.hanSuDung());
                        case 3 -> l.soLuong() + " " + l.donViTinh();
                        default -> l.mucCanhBao();
                    };
                }
            };
            JTable table = styledTable(model, new int[] {90, 240, 130, 100, 140});
            table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
            expiryTableHost.add(tableBlock(table, 22), BorderLayout.CENTER);
        }
        expiryTableHost.revalidate();
        expiryTableHost.repaint();
    }

    private JComponent buildSideColumn(InventoryOverviewResponse data) {
        JPanel column = new JPanel(new GridBagLayout());
        column.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 18, 0);
        column.add(buildQuickActions(), gc);
        column.add(buildActivity(data.hoatDongHomNay()), gc);
        gc.insets = new Insets(0, 0, 0, 0);
        column.add(buildNotices(data.canChuY()), gc);

        // Cột phải rộng cố định, cao theo nội dung (không kéo giãn theo cột trái).
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(SIDE_COLUMN_WIDTH, column.getPreferredSize().height);
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(column, BorderLayout.NORTH);
        return wrapper;
    }

    private JComponent buildQuickActions() {
        Card card = new Card(new BorderLayout(0, 14), 22);
        JPanel stack = new JPanel(new GridLayout(0, 1, 0, 10));
        stack.setOpaque(false);
        card.add(sectionTitle("THAO TÁC NHANH"), BorderLayout.NORTH);
        stack.add(quickButton("+  " + InventoryQuickAction.NHAP_KHO.label(), InventoryQuickAction.NHAP_KHO, ButtonKind.PRIMARY));
        stack.add(quickButton("−  " + InventoryQuickAction.XUAT_KHO.label(), InventoryQuickAction.XUAT_KHO, ButtonKind.PRIMARY));
        stack.add(quickButton(InventoryQuickAction.HOAN_TRA_KHO.label(), InventoryQuickAction.HOAN_TRA_KHO, ButtonKind.OUTLINE));
        stack.add(quickButton(InventoryQuickAction.KIEM_KE_KHO.label(), InventoryQuickAction.KIEM_KE_KHO, ButtonKind.OUTLINE));
        stack.add(quickButton(InventoryQuickAction.HUY_HAO_HUT.label(), InventoryQuickAction.HUY_HAO_HUT, ButtonKind.OUTLINE));
        card.add(stack, BorderLayout.CENTER);
        return card;
    }

    private JButton quickButton(String text, InventoryQuickAction action, ButtonKind kind) {
        JButton b = button(text, kind);
        b.setFont(font(kind == ButtonKind.PRIMARY ? Font.BOLD : Font.PLAIN, 15));
        b.addActionListener(e -> quickActionHandler.accept(action));
        return b;
    }

    private JComponent buildActivity(InventoryActivityResponse a) {
        Card card = new Card(new BorderLayout(0, 12), 22);
        card.add(sectionTitle("HOẠT ĐỘNG KHO HÔM NAY"), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 8));
        grid.setOpaque(false);
        grid.add(bodyLabel("Nhập kho: " + a.nhapKho()));
        grid.add(bodyLabel("Xuất kho: " + a.xuatKho()));
        grid.add(bodyLabel("Hoàn trả: " + a.hoanTra()));
        grid.add(bodyLabel("Hủy / hao hụt: " + a.huyHaoHut()));
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildNotices(List<AttentionNoticeResponse> notices) {
        Card card = new Card(new BorderLayout(0, 12), 22);
        card.add(sectionTitle("CẦN CHÚ Ý"), BorderLayout.NORTH);
        JPanel list = new JPanel(new GridLayout(0, 1, 0, 8));
        list.setOpaque(false);
        if (notices.isEmpty()) {
            list.add(bodyLabel("Không có cảnh báo."));
        }
        for (AttentionNoticeResponse n : notices) {
            Color color = n.mucDo() == NoticeSeverity.CRITICAL ? RED : ORANGE;
            JLabel label = bodyLabel("<html>" + escape(n.noiDung()) + "</html>");
            label.setIcon(new DotIcon(color, 11));
            label.setIconTextGap(10);
            label.setVerticalTextPosition(SwingConstants.CENTER);
            list.add(label);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------- helpers

    private static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 15));
        l.setForeground(TEXT);
        return l;
    }

    private static JLabel bodyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 15));
        l.setForeground(TEXT);
        return l;
    }

    private static JComponent emptyState(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font(Font.PLAIN, 14));
        l.setForeground(TEXT_MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(24, 22, 28, 22));
        return l;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JTable styledTable(AbstractTableModel model, int[] widths) {
        JTable table = new JTable(model);
        table.setFont(font(Font.PLAIN, 15));
        table.setForeground(TEXT);
        table.setRowHeight(46);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setBackground(SURFACE);
        table.setDefaultRenderer(Object.class, paddedRenderer());
        table.setDefaultRenderer(Integer.class, paddedRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setFont(font(Font.PLAIN, 15));
                l.setForeground(TEXT);
                l.setBackground(HEADER_ROW);
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 22, 0, 8)));
                return l;
            }
        });
        header.setPreferredSize(new Dimension(10, 46));

        TableColumnModel cols = table.getColumnModel();
        for (int i = 0; i < widths.length; i++) {
            cols.getColumn(i).setPreferredWidth(widths[i]);
        }
        return table;
    }

    private static DefaultTableCellRenderer paddedRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, c);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 8));
                return l;
            }
        };
        return r;
    }

    /** Header + bảng không bọc JScrollPane để bảng cao đúng số dòng như thiết kế. */
    private static JComponent tableBlock(JTable table, int sideInset) {
        JPanel block = new JPanel(new BorderLayout());
        block.setOpaque(false);
        block.setBorder(BorderFactory.createEmptyBorder(0, sideInset, sideInset > 0 ? 18 : 12, sideInset));
        block.add(table.getTableHeader(), BorderLayout.NORTH);
        block.add(table, BorderLayout.CENTER);
        return block;
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
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 8));
            l.setIconTextGap(10);
            if (v instanceof StockLevel level) {
                l.setText(level.label());
                l.setIcon(new DotIcon(level == StockLevel.THIEU ? RED : ORANGE, 12));
            } else if (v instanceof ExpiryLevel level) {
                l.setText(level.label());
                Color color = switch (level) {
                    case DA_HET_HAN -> RED;
                    case CON_TOI_DA_7_NGAY -> ORANGE;
                    case CON_TOI_DA_30_NGAY -> AMBER;
                    case CON_HAN -> GREEN;
                };
                l.setIcon(new DotIcon(color, 12));
            }
            return l;
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
