package com.quanlynhakhoa.view;

import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * HOME-UI-01: horizontal business navigation + dashboard shell.
 * Permission filtering and the authenticated constructor are preserved.
 * All feature destinations remain placeholders; only feature owners implement them.
 */
public final class MainFrame extends JFrame {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color INK = new Color(28, 48, 75);
    private static final Color LINE = new Color(221, 231, 242);
    private final MainNavigationModel model;
    private final Runnable returnToLogin;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<Module> allowed;
    private final Map<Module, JMenuItem> links = new EnumMap<>(Module.class);
    private JButton overviewButton;

    /** Do not introduce a no-argument constructor: access requires a real session. */
    public MainFrame(DesktopSession session, Runnable returnToLogin) {
        super("Nhóm 8 - Quản lý nha khoa");
        this.model = new MainNavigationModel(session);
        this.returnToLogin = Objects.requireNonNull(returnToLogin, "returnToLogin");
        this.allowed = new HashSet<>(model.availableModules());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1310, 820);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                model.logout();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        JPanel north = new JPanel(new BorderLayout());
        north.add(buildHeader(session), BorderLayout.NORTH);
        north.add(buildNavigation(), BorderLayout.SOUTH);
        root.add(north, BorderLayout.NORTH);

        content.setBackground(BACKGROUND);
        for (Module module : model.availableModules()) {
            JPanel page = module == Module.HOME ? new DashboardPanel(session) : buildPlaceholder(module);
            JScrollPane scroll = new JScrollPane(page);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            content.add(scroll, module.name());
        }
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        navigate(Module.HOME);
    }

    private JPanel buildHeader(DesktopSession session) {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(17, 26, 16, 26));

        JPanel brand = new JPanel(new BorderLayout(0, 4));
        brand.setOpaque(false);
        JLabel title = new JLabel("QUẢN LÝ NHA KHOA  |  NHÓM 8");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(NAVY);
        JLabel caption = new JLabel("Ứng dụng quản lý phòng khám · Java Swing");
        caption.setFont(new Font("SansSerif", Font.PLAIN, 12));
        caption.setForeground(new Color(104, 119, 136));
        brand.add(title, BorderLayout.NORTH);
        brand.add(caption, BorderLayout.SOUTH);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 2));
        right.setOpaque(false);
        JLabel identity = new JLabel(session.accountId() + "   ·   " + roleLabel(session));
        identity.setFont(new Font("SansSerif", Font.PLAIN, 12));
        identity.setForeground(INK);
        right.add(identity);
        JButton logout = new JButton("Đăng xuất");
        logout.addActionListener(event -> logout());
        logout.setFocusPainted(false);
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JMenuBar buildNavigation() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(NAVY);
        bar.setOpaque(true);
        bar.setBorder(new EmptyBorder(3, 16, 3, 16));

        overviewButton = new JButton("Tổng quan");
        overviewButton.setForeground(Color.WHITE);
        overviewButton.setBackground(NAVY);
        overviewButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        overviewButton.setBorder(new EmptyBorder(14, 13, 14, 13));
        overviewButton.setFocusPainted(false);
        overviewButton.addActionListener(event -> navigate(Module.HOME));
        bar.add(overviewButton);

        JMenu patient = menu("Bệnh nhân");
        item(patient, "Danh sách bệnh nhân", Module.PATIENTS);
        item(patient, "Hồ sơ bệnh án", Module.MEDICAL_RECORDS);
        pending(patient, "Tiếp nhận mới");
        addIfNotEmpty(bar, patient);

        JMenu appointments = menu("Lịch hẹn");
        item(appointments, "Danh sách lịch hẹn", Module.APPOINTMENTS);
        pending(appointments, "Đặt lịch hẹn");
        addIfNotEmpty(bar, appointments);

        // Clinical feature destinations do not exist yet; no new permissions are invented.
        if (allowed.contains(Module.MEDICAL_RECORDS)) {
            JMenu clinical = menu("Khám & điều trị");
            pending(clinical, "Phiếu khám");
            pending(clinical, "Kế hoạch điều trị");
            pending(clinical, "Dịch vụ khám");
            bar.add(clinical);
        }

        JMenu billing = menu("Hóa đơn");
        item(billing, "Danh sách hóa đơn", Module.BILLING);
        pending(billing, "Thanh toán");
        pending(billing, "Công nợ");
        addIfNotEmpty(bar, billing);

        JMenu inventory = menu("Kho vật tư");
        item(inventory, "Danh mục thuốc & vật tư", Module.INVENTORY);
        pending(inventory, "Nhập / xuất kho");
        addIfNotEmpty(bar, inventory);

        JMenu workforce = menu("Nhân viên");
        item(workforce, "Danh sách nhân viên", Module.WORKFORCE);
        pending(workforce, "Phân công ca");
        addIfNotEmpty(bar, workforce);

        JMenu system = menu("Hệ thống");
        item(system, "Tài khoản", Module.ACCOUNTS);
        item(system, "Vai trò", Module.ROLES);
        item(system, "Nhật ký thao tác", Module.AUDIT);
        addIfNotEmpty(bar, system);
        return bar;
    }

    private JMenu menu(String title) {
        JMenu menu = new JMenu(title + "  ▾");
        menu.setForeground(Color.WHITE);
        menu.setFont(new Font("SansSerif", Font.BOLD, 13));
        menu.setBorder(new EmptyBorder(13, 13, 13, 13));
        return menu;
    }

    private void item(JMenu menu, String text, Module module) {
        if (!allowed.contains(module)) return;
        JMenuItem action = new JMenuItem(text);
        action.setFont(new Font("SansSerif", Font.PLAIN, 13));
        action.setBorder(new EmptyBorder(11, 14, 11, 30));
        action.addActionListener(event -> navigate(module));
        menu.add(action);
        links.put(module, action);
    }

    private void pending(JMenu menu, String text) {
        // Only show a disabled preview entry if this user can already open a module in the group.
        // Empty groups are hidden below; clinical group has a separate permission check.
        JMenuItem action = new JMenuItem(text + "  ·  Sắp triển khai");
        action.setFont(new Font("SansSerif", Font.PLAIN, 12));
        action.setEnabled(false);
        menu.add(action);
    }

    private void addIfNotEmpty(JMenuBar bar, JMenu menu) {
        boolean hasEnabledItem = false;
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && item.isEnabled()) hasEnabledItem = true;
        }
        if (hasEnabledItem) bar.add(menu);
    }

    private JPanel buildPlaceholder(Module module) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(38, 40, 38, 40));
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(34, 34, 34, 34)));
        JLabel title = new JLabel(labelFor(module));
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(NAVY);
        card.add(title, BorderLayout.NORTH);
        JLabel message = new JLabel("Giao diện nghiệp vụ này sẽ do thành viên phụ trách phát triển.");
        message.setFont(new Font("SansSerif", Font.PLAIN, 14));
        message.setForeground(INK);
        card.add(message, BorderLayout.CENTER);
        JLabel note = new JLabel("Chưa tích hợp dữ liệu. Không có thao tác SQL từ màn hình này.", SwingConstants.LEFT);
        note.setForeground(new Color(94, 109, 127));
        card.add(note, BorderLayout.SOUTH);
        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private void navigate(Module module) {
        model.select(module); // Enforces role and active-session authorization.
        cards.show(content, module.name());
        if (overviewButton != null) {
            overviewButton.setBackground(module == Module.HOME ? new Color(43, 110, 190) : NAVY);
        }
        for (Map.Entry<Module, JMenuItem> entry : links.entrySet()) {
            entry.getValue().setForeground(entry.getKey() == module
                    ? new Color(28, 112, 197) : INK);
        }
    }

    private void logout() {
        if (!model.isActive()) return;
        model.logout();
        dispose();
        returnToLogin.run();
    }

    public static String labelFor(Module module) {
        return switch (module) {
            case HOME -> "Tổng quan";
            case PATIENTS -> "Khách hàng";
            case MEDICAL_RECORDS -> "Hồ sơ bệnh án";
            case APPOINTMENTS -> "Lịch hẹn";
            case BILLING -> "Hóa đơn";
            case INVENTORY -> "Kho vật tư";
            case WORKFORCE -> "Nhân viên";
            case ACCOUNTS -> "Tài khoản";
            case ROLES -> "Vai trò";
            case AUDIT -> "Nhật ký thao tác";
        };
    }

    private static String roleLabel(DesktopSession session) {
        return switch (session.role()) {
            case SYSTEM_ADMIN -> "Quản trị hệ thống";
            case CLINIC_MANAGER -> "Quản lý phòng khám";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case NURSE -> "Y tá";
            case INVENTORY_MANAGER -> "Quản lý kho";
        };
    }
}
