package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import com.quanlynhakhoa.features.inventory.service.StockInService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;

/**
 * Không gian làm việc của Quản lý kho: sidebar menu + thanh trên + vùng nội dung.
 * MainFrame (shell chung) chỉ cần đặt panel này làm nội dung khi Role có quyền kho.
 */
public final class InventoryWorkspacePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIDEBAR_WIDTH = 280;

    private final transient InventoryOverviewService overviewService;
    private final transient InventoryItemService itemService;
    private final transient StockInService stockInService;
    private final transient StockOutService stockOutService;
    private final transient Supplier<InventoryActor> session;
    private final transient Supplier<Set<Role>> sessionRoles;

    private final CardLayout contentCards = new CardLayout();
    private final JPanel pnlContent = new JPanel(contentCards);
    private final Map<InventoryScreen, JToggleButton> menuItems = new EnumMap<>(InventoryScreen.class);
    private final Map<InventoryScreen, JComponent> screens = new EnumMap<>(InventoryScreen.class);
    private final ButtonGroup grpMenu = new ButtonGroup();

    /**
     * @param session người dùng của phiên đăng nhập hiện tại (do shell/Login cung cấp)
     */
    public InventoryWorkspacePanel(InventoryOverviewService overviewService, InventoryItemService itemService,
                                   StockInService stockInService, StockOutService stockOutService,
                                   Supplier<InventoryActor> session) {
        super(new BorderLayout());
        this.overviewService = Objects.requireNonNull(overviewService);
        this.itemService = Objects.requireNonNull(itemService);
        this.stockInService = Objects.requireNonNull(stockInService);
        this.stockOutService = Objects.requireNonNull(stockOutService);
        this.session = Objects.requireNonNull(session);
        this.sessionRoles = () -> session.get().roles();
        String displayName = session.get().hoTen();
        setBackground(BACKGROUND);

        pnlContent.setBackground(BACKGROUND);
        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.setBackground(BACKGROUND);
        pnlMain.add(buildTopbar(displayName == null || displayName.isBlank() ? "Người dùng" : displayName.strip()),
                BorderLayout.NORTH);
        pnlMain.add(pnlContent, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.WEST);
        add(pnlMain, BorderLayout.CENTER);
        showScreen(InventoryScreen.OVERVIEW);
    }

    /** Chuyển màn hình; màn chưa triển khai hiển thị thông báo "đang phát triển". */
    public void showScreen(InventoryScreen screen) {
        Objects.requireNonNull(screen, "screen must not be null");
        menuItems.get(screen).setSelected(true);
        JComponent view = screens.get(screen);
        if (view == null) {
            view = createScreen(screen);
            screens.put(screen, view);
            pnlContent.add(view, screen.name());
        } else if (view instanceof InventoryOverviewPanel pnlOverview) {
            pnlOverview.reload();
        } else if (view instanceof InventoryItemPanel pnlItems) {
            pnlItems.reload();
        } else if (view instanceof StockInPanel pnlStockIn) {
            pnlStockIn.reload();
        } else if (view instanceof StockOutPanel pnlStockOut) {
            pnlStockOut.reload();
        }
        contentCards.show(pnlContent, screen.name());
    }

    private JComponent createScreen(InventoryScreen screen) {
        return switch (screen) {
            case OVERVIEW -> {
                InventoryOverviewPanel pnlOverview = new InventoryOverviewPanel(overviewService, sessionRoles);
                pnlOverview.setQuickActionHandler(action -> showScreen(action.target()));
                yield pnlOverview;
            }
            case ITEMS -> new InventoryItemPanel(itemService, sessionRoles);
            case STOCK_IN -> new StockInPanel(stockInService, itemService, session);
            case STOCK_OUT -> new StockOutPanel(stockOutService, itemService, session);
            default -> new InventoryPlaceholderPanel(screen);
        };
    }

    // ----------------------------------------------------------------- sidebar

    private JComponent buildSidebar() {
        JPanel pnlSidebar = new JPanel(new BorderLayout());
        pnlSidebar.setBackground(SIDEBAR);
        pnlSidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 10));
        pnlSidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        pnlSidebar.add(buildBrand(), BorderLayout.NORTH);

        JPanel pnlMenu = new JPanel();
        pnlMenu.setOpaque(false);
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setBorder(BorderFactory.createEmptyBorder(4, 16, 12, 16));
        String currentSection = null;
        for (InventoryScreen screen : InventoryScreen.values()) {
            if (screen.section() != null && !screen.section().equals(currentSection)) {
                currentSection = screen.section();
                pnlMenu.add(Box.createVerticalStrut(14));
                pnlMenu.add(menuSectionLabel(currentSection));
                pnlMenu.add(Box.createVerticalStrut(4));
            }
            JToggleButton tglMenuItem = createMenuItem(screen.label(), screen.glyph());
            tglMenuItem.setAlignmentX(LEFT_ALIGNMENT);
            tglMenuItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, tglMenuItem.getPreferredSize().height));
            if (!screen.implemented()) {
                tglMenuItem.setToolTipText("Đang phát triển");
            }
            tglMenuItem.addActionListener(e -> showScreen(screen));
            grpMenu.add(tglMenuItem);
            menuItems.put(screen, tglMenuItem);
            pnlMenu.add(tglMenuItem);
            pnlMenu.add(Box.createVerticalStrut(2));
        }
        JScrollPane scrMenu = new JScrollPane(pnlMenu,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrMenu.setBorder(BorderFactory.createEmptyBorder());
        scrMenu.setOpaque(false);
        scrMenu.getViewport().setOpaque(false);
        scrMenu.getVerticalScrollBar().setUnitIncrement(16);
        pnlSidebar.add(scrMenu, BorderLayout.CENTER);

        JLabel lblFooter = new JLabel("N08 · Phòng khám nha khoa");
        lblFooter.setOpaque(true);
        lblFooter.setBackground(ROLE_CHIP);
        lblFooter.setForeground(TEXT_MUTED);
        lblFooter.setFont(font(Font.PLAIN, 13));
        lblFooter.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        pnlFooter.add(lblFooter, BorderLayout.CENTER);
        pnlSidebar.add(pnlFooter, BorderLayout.SOUTH);
        return pnlSidebar;
    }

    private static JComponent buildBrand() {
        JPanel pnlBrand = new JPanel(new BorderLayout(14, 0));
        pnlBrand.setOpaque(false);
        pnlBrand.setBorder(BorderFactory.createEmptyBorder(24, 24, 18, 16));
        JLabel lblLogo = new JLabel(new GlyphIcon(MenuGlyph.CLINIC, 26, Color.WHITE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                antialias(g2);
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblLogo.setPreferredSize(new Dimension(52, 52));
        lblLogo.setHorizontalAlignment(JLabel.CENTER);
        JPanel pnlBrandText = new JPanel();
        pnlBrandText.setOpaque(false);
        pnlBrandText.setLayout(new BoxLayout(pnlBrandText, BoxLayout.Y_AXIS));
        JLabel lblBrandName = new JLabel("N08 Dental");
        lblBrandName.setFont(font(Font.BOLD, 20));
        lblBrandName.setForeground(TEXT);
        JLabel lblBrandSub = new JLabel("Hệ thống phòng khám");
        lblBrandSub.setFont(font(Font.PLAIN, 14));
        lblBrandSub.setForeground(TEXT_MUTED);
        pnlBrandText.add(Box.createVerticalStrut(4));
        pnlBrandText.add(lblBrandName);
        pnlBrandText.add(Box.createVerticalStrut(2));
        pnlBrandText.add(lblBrandSub);
        pnlBrand.add(lblLogo, BorderLayout.WEST);
        pnlBrand.add(pnlBrandText, BorderLayout.CENTER);
        return pnlBrand;
    }

    private static JLabel menuSectionLabel(String text) {
        JLabel lblSection = new JLabel(text);
        lblSection.setFont(font(Font.PLAIN, 13));
        lblSection.setForeground(TEXT_MUTED);
        lblSection.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 0));
        lblSection.setAlignmentX(LEFT_ALIGNMENT);
        return lblSection;
    }

    // ------------------------------------------------------------------ topbar

    private JComponent buildTopbar(String displayName) {
        JPanel pnlTopbar = new JPanel(new BorderLayout());
        pnlTopbar.setBackground(SURFACE);
        pnlTopbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 32, 14, 28)));

        JPanel pnlUser = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        pnlUser.setOpaque(false);
        JLabel lblWorkspace = new JLabel("Không gian làm việc");
        lblWorkspace.setFont(font(Font.PLAIN, 15));
        lblWorkspace.setForeground(TEXT_MUTED);

        JPanel pnlIdentity = new JPanel();
        pnlIdentity.setOpaque(false);
        pnlIdentity.setLayout(new BoxLayout(pnlIdentity, BoxLayout.Y_AXIS));
        JLabel lblUserName = new JLabel(displayName);
        lblUserName.setFont(font(Font.BOLD, 15));
        lblUserName.setForeground(TEXT);
        JLabel lblRole = new JLabel(roleLabel(sessionRoles.get()));
        lblRole.setOpaque(true);
        lblRole.setBackground(ROLE_CHIP);
        lblRole.setForeground(ROLE_CHIP_TEXT);
        lblRole.setFont(font(Font.PLAIN, 12));
        lblRole.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        pnlIdentity.add(lblUserName);
        pnlIdentity.add(Box.createVerticalStrut(4));
        pnlIdentity.add(lblRole);

        pnlUser.add(lblWorkspace);
        pnlUser.add(Box.createHorizontalStrut(10));
        pnlUser.add(new Avatar(initials(displayName)));
        pnlUser.add(pnlIdentity);
        pnlUser.add(new JLabel(new GlyphIcon(MenuGlyph.CHEVRON_DOWN, 16, TEXT_MUTED)));
        pnlTopbar.add(pnlUser, BorderLayout.EAST);
        return pnlTopbar;
    }

    static String roleLabel(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return "Chưa đăng nhập";
        }
        if (roles.contains(Role.INVENTORY_MANAGER)) {
            return "Quản lý kho";
        }
        if (roles.contains(Role.SYSTEM_ADMIN)) {
            return "Quản trị hệ thống";
        }
        if (roles.contains(Role.CLINIC_MANAGER)) {
            return "Quản lý phòng khám";
        }
        if (roles.contains(Role.DOCTOR)) {
            return "Bác sĩ";
        }
        if (roles.contains(Role.NURSE)) {
            return "Y tá";
        }
        return "Lễ tân";
    }

    /** "Nguyễn Văn Trí" → "VT"; "Người dùng" → "ND". */
    static String initials(String name) {
        String[] parts = name.strip().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        String first = parts[parts.length - 2];
        String last = parts[parts.length - 1];
        return (first.substring(0, 1) + last.substring(0, 1)).toUpperCase(Locale.forLanguageTag("vi"));
    }

    /** Hình tròn chứa chữ viết tắt tên người dùng. */
    private static final class Avatar extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String text;

        Avatar(String text) {
            this.text = text;
            setPreferredSize(new Dimension(48, 48));
            setFont(font(Font.PLAIN, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(MENU_SELECTED);
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(PRIMARY);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
