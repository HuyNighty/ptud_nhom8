package com.quanlynhakhoa.view;

import com.quanlynhakhoa.features.FeatureBootstrap;
import com.quanlynhakhoa.features.FeatureRegistry;
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
import java.util.EnumSet;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public final class MainFrame extends JFrame {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color INK = new Color(28, 48, 75);
    private static final Color LINE = new Color(221, 231, 242);
    private final MainNavigationModel model;
    private final DesktopSession session;
    private final FeatureRegistry registry;
    private final Set<Module> loaded = EnumSet.noneOf(Module.class);
    private final Map<Module, JPanel> slots = new EnumMap<>(Module.class);
    private final Runnable returnToLogin;
    private final CardLayout cards = new CardLayout();
    private final JPanel pnlContent = new JPanel(cards);
    private final Set<Module> allowed;
    private final Map<Module, JMenuItem> links = new EnumMap<>(Module.class);
    private JButton btnOverview;

    public MainFrame(DesktopSession session, Runnable returnToLogin) {
        this(session, returnToLogin, FeatureBootstrap.createRegistry());
    }

    public MainFrame(DesktopSession session, Runnable returnToLogin, FeatureRegistry registry) {
        super("Nhóm 8 - Quản lý nha khoa");
        this.model = new MainNavigationModel(session);
        this.session = session;
        this.registry = Objects.requireNonNull(registry, "registry");
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

        JPanel pnlRoot = new JPanel(new BorderLayout());
        pnlRoot.setBackground(BACKGROUND);
        JPanel pnlNorth = new JPanel(new BorderLayout());
        pnlNorth.add(buildHeader(session), BorderLayout.NORTH);
        pnlNorth.add(buildNavigation(), BorderLayout.SOUTH);
        pnlRoot.add(pnlNorth, BorderLayout.NORTH);

        pnlContent.setBackground(BACKGROUND);
        for (Module module : model.availableModules()) {
            JPanel pnlPage = module == Module.HOME ? new DashboardPanel(session) : buildPlaceholder(module);
            JPanel pnlSlot = new JPanel(new BorderLayout());
            pnlSlot.add(scroll(pnlPage), BorderLayout.CENTER);
            slots.put(module, pnlSlot);
            pnlContent.add(pnlSlot, module.name());
        }
        pnlRoot.add(pnlContent, BorderLayout.CENTER);
        setContentPane(pnlRoot);
        navigate(Module.HOME);
    }

    private JPanel buildHeader(DesktopSession session) {
        JPanel pnlHeader = new JPanel(new BorderLayout(20, 0));
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(new EmptyBorder(17, 26, 16, 26));

        JPanel pnlBrand = new JPanel(new BorderLayout(0, 4));
        pnlBrand.setOpaque(false);
        JLabel lblTitle = new JLabel("QUẢN LÝ NHA KHOA  |  NHÓM 8");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitle.setForeground(NAVY);
        JLabel lblCaption = new JLabel("Ứng dụng quản lý phòng khám · Java Swing");
        lblCaption.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblCaption.setForeground(new Color(104, 119, 136));
        pnlBrand.add(lblTitle, BorderLayout.NORTH);
        pnlBrand.add(lblCaption, BorderLayout.SOUTH);
        pnlHeader.add(pnlBrand, BorderLayout.WEST);

        JPanel pnlRight = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 2));
        pnlRight.setOpaque(false);
        JLabel lblIdentity = new JLabel(session.accountId() + "   ·   " + roleLabel(session));
        lblIdentity.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblIdentity.setForeground(INK);
        pnlRight.add(lblIdentity);
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(event -> logout());
        btnLogout.setFocusPainted(false);
        pnlRight.add(btnLogout);
        pnlHeader.add(pnlRight, BorderLayout.EAST);
        return pnlHeader;
    }

    /** Menu grouped by five stages of clinic operation; module authorization is unchanged. */
    private JMenuBar buildNavigation() {
        JMenuBar mnuBar = new JMenuBar();
        mnuBar.setBackground(NAVY);
        mnuBar.setOpaque(true);
        mnuBar.setBorder(new EmptyBorder(3, 16, 3, 16));

        btnOverview = new JButton("Tổng quan");
        btnOverview.setForeground(Color.WHITE);
        btnOverview.setBackground(NAVY);
        btnOverview.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnOverview.setBorder(new EmptyBorder(14, 13, 14, 13));
        btnOverview.setFocusPainted(false);
        btnOverview.addActionListener(event -> navigate(Module.HOME));
        mnuBar.add(btnOverview);

        // 1. Tổ chức: cơ cấu nhân sự, tài khoản và danh mục dịch vụ.
        JMenu mnuOrganization = menu("Tổ chức");
        item(mnuOrganization, "Danh sách nhân viên", Module.WORKFORCE);
        item(mnuOrganization, "Tài khoản", Module.ACCOUNTS);
        item(mnuOrganization, "Vai trò", Module.ROLES);
        item(mnuOrganization, "Bảng giá dịch vụ", Module.SERVICE_CATALOG);
        addIfNotEmpty(mnuBar, mnuOrganization);

        // 2. Kế hoạch: lịch hẹn và thời gian làm việc.
        JMenu mnuPlanning = menu("Kế hoạch");
        item(mnuPlanning, "Danh sách lịch hẹn", Module.APPOINTMENTS);
        item(mnuPlanning, "Lịch làm việc của tôi", Module.MY_SCHEDULE);
        if (allowed.contains(Module.APPOINTMENTS)) {
            pending(mnuPlanning, "Đặt lịch hẹn");
        }
        if (allowed.contains(Module.WORKFORCE)) {
            pending(mnuPlanning, "Phân công ca");
        }
        addIfNotEmpty(mnuBar, mnuPlanning);

        // 3. Tiếp nhận: thông tin ban đầu của bệnh nhân.
        JMenu mnuReception = menu("Tiếp nhận");
        item(mnuReception, "Danh sách bệnh nhân", Module.PATIENTS);
        if (allowed.contains(Module.PATIENTS)) {
            pending(mnuReception, "Tiếp nhận mới");
        }
        addIfNotEmpty(mnuBar, mnuReception);

        // 4. Hoạt động: hồ sơ, khám chữa bệnh, thuốc và vật tư.
        JMenu mnuOperations = menu("Hoạt động");
        item(mnuOperations, "Hồ sơ bệnh án", Module.MEDICAL_RECORDS);
        item(mnuOperations, "Phiếu khám", Module.EXAMINATIONS);
        item(mnuOperations, "Danh mục thuốc & vật tư", Module.INVENTORY);
        item(mnuOperations, "Yêu cầu cấp phát", Module.SUPPLY_REQUESTS);
        item(mnuOperations, "Kiểm tra hộp cấp cứu", Module.EMERGENCY_KIT);
        if (allowed.contains(Module.EXAMINATIONS)) {
            pending(mnuOperations, "Kế hoạch điều trị");
        }
        if (allowed.contains(Module.INVENTORY)) {
            pending(mnuOperations, "Nhập / xuất kho");
        }
        addIfNotEmpty(mnuBar, mnuOperations);

        // 5. Tổng kết: thanh toán, công nợ và lịch sử thao tác.
        JMenu mnuSummary = menu("Tổng kết");
        item(mnuSummary, "Danh sách hóa đơn", Module.BILLING);
        item(mnuSummary, "Nhật ký thao tác", Module.AUDIT);
        if (allowed.contains(Module.BILLING)) {
            pending(mnuSummary, "Thanh toán");
            pending(mnuSummary, "Công nợ");
        }
        addIfNotEmpty(mnuBar, mnuSummary);
        return mnuBar;
    }

    private JMenu menu(String title) {
        JMenu mnuMenu = new JMenu(title + "  ▾");
        mnuMenu.setForeground(Color.WHITE);
        mnuMenu.setFont(new Font("SansSerif", Font.BOLD, 13));
        mnuMenu.setBorder(new EmptyBorder(13, 13, 13, 13));
        return mnuMenu;
    }

    private void item(JMenu mnuMenu, String text, Module module) {
        if (!allowed.contains(module)) return;
        JMenuItem mniAction = new JMenuItem(text);
        mniAction.setFont(new Font("SansSerif", Font.PLAIN, 13));
        mniAction.setBorder(new EmptyBorder(11, 14, 11, 30));
        mniAction.addActionListener(event -> navigate(module));
        mnuMenu.add(mniAction);
        links.put(module, mniAction);
    }

    private void pending(JMenu mnuMenu, String text) {
        JMenuItem mniAction = new JMenuItem(text + "  ·  Sắp triển khai");
        mniAction.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mniAction.setEnabled(false);
        mnuMenu.add(mniAction);
    }

    private void addIfNotEmpty(JMenuBar mnuBar, JMenu mnuMenu) {
        boolean hasEnabledItem = false;
        for (int i = 0; i < mnuMenu.getItemCount(); i++) {
            JMenuItem mniItem = mnuMenu.getItem(i);
            if (mniItem != null && mniItem.isEnabled()) hasEnabledItem = true;
        }
        if (hasEnabledItem) mnuBar.add(mnuMenu);
    }

    private JPanel buildPlaceholder(Module module) {
        JPanel pnlPanel = new JPanel(new BorderLayout());
        pnlPanel.setBackground(BACKGROUND);
        pnlPanel.setBorder(new EmptyBorder(38, 40, 38, 40));
        JPanel pnlCard = new JPanel(new BorderLayout(0, 20));
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(34, 34, 34, 34)));
        JLabel lblTitle = new JLabel(labelFor(module));
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(NAVY);
        pnlCard.add(lblTitle, BorderLayout.NORTH);
        JLabel lblMessage = new JLabel("Giao diện nghiệp vụ này sẽ do thành viên phụ trách phát triển.");
        lblMessage.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblMessage.setForeground(INK);
        pnlCard.add(lblMessage, BorderLayout.CENTER);
        JLabel lblNote = new JLabel("Chưa tích hợp dữ liệu. Không có thao tác SQL từ màn hình này.", SwingConstants.LEFT);
        lblNote.setForeground(new Color(94, 109, 127));
        pnlCard.add(lblNote, BorderLayout.SOUTH);
        pnlPanel.add(pnlCard, BorderLayout.NORTH);
        return pnlPanel;
    }

    private JScrollPane scroll(JPanel pnlPage) {
        JScrollPane scrPage = new JScrollPane(pnlPage);
        scrPage.setBorder(BorderFactory.createEmptyBorder());
        scrPage.getVerticalScrollBar().setUnitIncrement(16);
        return scrPage;
    }

    private void loadFeature(Module module) {
        if (module == Module.HOME || loaded.contains(module) || !registry.contains(module)) return;
        try {
            JPanel pnlPage = registry.create(session, module);
            JPanel pnlSlot = slots.get(module);
            pnlSlot.removeAll();
            pnlSlot.add(scroll(pnlPage), BorderLayout.CENTER);
            pnlSlot.revalidate();
            pnlSlot.repaint();
            loaded.add(module);
        } catch (RuntimeException failure) {
            JOptionPane.showMessageDialog(this, "Không thể mở màn hình nghiệp vụ.",
                    "Lỗi tải giao diện", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void navigate(Module module) {
        model.select(module);
        loadFeature(module);
        cards.show(pnlContent, module.name());
        if (btnOverview != null) {
            btnOverview.setBackground(module == Module.HOME ? new Color(43, 110, 190) : NAVY);
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
            case EXAMINATIONS -> "Phiếu khám";
            case SERVICE_CATALOG -> "Bảng giá dịch vụ";
            case SUPPLY_REQUESTS -> "Yêu cầu cấp phát";
            case MY_SCHEDULE -> "Lịch làm việc của tôi";
            case EMERGENCY_KIT -> "Kiểm tra hộp cấp cứu";
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
