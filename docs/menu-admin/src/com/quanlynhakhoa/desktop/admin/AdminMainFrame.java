package com.quanlynhakhoa.desktop.admin;

import com.quanlynhakhoa.desktop.menu.AdminMenu;
import com.quanlynhakhoa.desktop.menu.SidebarMenu;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/** Khung chính của Admin: menu trái + thanh trên + vùng màn hình (CardLayout). */
public class AdminMainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel screenTitle = Ui.label("Tổng quan", Font.BOLD, 14, Ui.STRONG);
    private final Map<String, String> titles = new HashMap<>();
    private final SidebarMenu menu;

    public AdminMainFrame() {
        super("N08 Nha Khoa — Quản lý phòng khám");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 760));
        setLayout(new BorderLayout());

        List<AdminMenu.Entry> all = new ArrayList<>();
        all.add(AdminMenu.home());
        AdminMenu.groups().forEach(g -> all.addAll(g.entries()));
        all.forEach(e -> titles.put(e.key(), e.title()));

        menu = new SidebarMenu("N08 Nha Khoa", "Quản lý phòng khám", null, AdminMenu.home(), AdminMenu.groups());

        content.setBackground(Ui.BG);
        content.add(new TongQuanPanel(menu::select), AdminMenu.TONG_QUAN);
        for (AdminMenu.Entry e : all) {
            if (!e.key().equals(AdminMenu.TONG_QUAN)) {
                content.add(placeholder(e), e.key()); // TODO thay bằng màn hình thật
            }
        }

        menu.addSelectionListener(key -> {
            cards.show(content, key);
            screenTitle.setText(titles.getOrDefault(key, ""));
        });
        menu.setBadge(AdminMenu.TAI_KHAM, 6);
        menu.setBadge(AdminMenu.DUYET_HUY, 2);
        menu.setBadge(AdminMenu.BAN_GIAO, 2);
        menu.setBadge(AdminMenu.KIEM_KE, 1);
        menu.setBadge(AdminMenu.TAI_KHOAN, 1);
        menu.select(AdminMenu.TONG_QUAN);
        menu.collapseAll();

        JPanel right = new JPanel(new BorderLayout());
        right.add(buildTopBar(), BorderLayout.NORTH);
        right.add(content, BorderLayout.CENTER);

        add(menu, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        setSize(1382, 860);
        setLocationRelativeTo(null);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.BORDER),
                BorderFactory.createEmptyBorder(0, 20, 0, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 16));
        left.setOpaque(false);
        left.add(Ui.label("Quản trị  /  ", Font.PLAIN, 14, Ui.MUTED));
        left.add(screenTitle);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        right.setOpaque(false);
        right.add(Ui.label("Quản trị viên", Font.BOLD, 14, Ui.STRONG));
        right.add(Ui.label("(admin)", Font.PLAIN, 13, Ui.MUTED));
        right.add(Ui.badge("Quản trị", Ui.VIOLET, new Color(0xF3EEFF)));
        JButton pwd = Ui.link("Đổi mật khẩu");
        JButton logout = Ui.button("Đăng xuất", false);
        right.add(pwd);
        right.add(logout);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private static JPanel placeholder(AdminMenu.Entry e) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Ui.BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Ui.label(e.title(), Font.BOLD, 22, Ui.STRONG), BorderLayout.NORTH);
        head.add(Ui.label(e.hint(), Font.PLAIN, 13, Ui.MUTED), BorderLayout.SOUTH);
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminMainFrame().setVisible(true));
    }
}
