package com.quanlynhakhoa.view;

import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/** R4-A2: authenticated, role-filtered shell. All modules are placeholders. */
public final class MainFrame extends JFrame {
    private static final Color NAVY = new Color(24, 43, 68);
    private static final Color LIGHT = new Color(244, 248, 252);
    private static final Color ACCENT = new Color(218, 235, 250);

    private final MainNavigationModel model;
    private final Runnable returnToLogin;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Module, JButton> links = new EnumMap<>(Module.class);

    /** No unauthenticated default constructor is intentionally provided. */
    public MainFrame(DesktopSession session, Runnable returnToLogin) {
        super("Nhóm 8 - Quản lý nha khoa");
        this.model = new MainNavigationModel(session);
        this.returnToLogin = Objects.requireNonNull(returnToLogin, "returnToLogin");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(820, 560));
        setSize(1060, 710);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                model.logout(); // Closing the window invalidates the local principal.
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LIGHT);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(LIGHT);
        right.add(buildHeader(session), BorderLayout.NORTH);
        content.setBackground(LIGHT);
        content.setBorder(new EmptyBorder(24, 26, 24, 26));
        for (Module module : model.availableModules()) {
            content.add(buildPage(module), module.name());
        }
        right.add(content, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        navigate(Module.HOME);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 22, 14));
        JLabel brand = new JLabel("NHA KHOA - NHÓM 8");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(24));

        for (Module module : model.availableModules()) {
            JButton link = new JButton(labelFor(module));
            link.setFont(new Font("SansSerif", Font.PLAIN, 14));
            link.setHorizontalAlignment(SwingConstants.LEFT);
            link.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
            link.setAlignmentX(LEFT_ALIGNMENT);
            link.setFocusPainted(true);
            link.addActionListener(event -> navigate(module));
            links.put(module, link);
            sidebar.add(link);
            sidebar.add(Box.createVerticalStrut(7));
        }
        sidebar.add(Box.createVerticalGlue());
        JButton logout = new JButton("Đăng xuất");
        logout.setFont(new Font("SansSerif", Font.BOLD, 14));
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.addActionListener(event -> logout());
        sidebar.add(logout);
        return sidebar;
    }

    private JPanel buildHeader(DesktopSession session) {
        JPanel header = new JPanel(new BorderLayout(16, 8));
        header.setBorder(new EmptyBorder(21, 28, 18, 28));
        header.setBackground(Color.WHITE);
        JLabel title = new JLabel("Hệ thống quản lý nha khoa");
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        title.setForeground(NAVY);
        header.add(title, BorderLayout.WEST);
        JLabel identity = new JLabel("Mã tài khoản: " + session.accountId()
                + "  |  Vai trò: " + roleLabel(session));
        identity.setFont(new Font("SansSerif", Font.PLAIN, 12));
        identity.setForeground(NAVY);
        header.add(identity, BorderLayout.EAST);
        return header;
    }

    private JPanel buildPage(Module module) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 226, 235)),
                new EmptyBorder(30, 30, 30, 30)));

        JLabel heading = new JLabel(module == Module.HOME
                ? "Chào mừng đến hệ thống" : labelFor(module));
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(NAVY);
        page.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3, 1, 8, 8));
        center.setBackground(Color.WHITE);
        if (module == Module.HOME) {
            center.add(new JLabel("Đăng nhập thành công. Điều hướng được lọc theo vai trò."));
            center.add(new JLabel("Các module nghiệp vụ chưa kết nối cơ sở dữ liệu."));
            center.add(new JLabel("Thử chọn mục bên trái hoặc Đăng xuất."));
        } else {
            center.add(new JLabel("Màn hình " + labelFor(module) + " đang chuẩn bị."));
            center.add(new JLabel("Chưa có chức năng xem, thêm, sửa hoặc xóa dữ liệu."));
            center.add(new JLabel("Quyền xem module không đồng nghĩa quyền thao tác nghiệp vụ."));
        }
        page.add(center, BorderLayout.CENTER);
        JLabel note = new JLabel("R4-A2 · Giao diện thử nghiệm trên cơ sở dữ liệu N08AuthIT_", SwingConstants.LEFT);
        note.setForeground(new Color(80, 96, 112));
        page.add(note, BorderLayout.SOUTH);
        return page;
    }

    private void navigate(Module module) {
        model.select(module); // Throws AccessDeniedException for unauthorized or logged-out access.
        cards.show(content, module.name());
        for (Map.Entry<Module, JButton> entry : links.entrySet()) {
            entry.getValue().setBackground(entry.getKey() == module ? ACCENT : Color.WHITE);
        }
    }

    private void logout() {
        if (!model.isActive()) return;
        model.logout(); // Invalidates in-memory authorization before opening login.
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
