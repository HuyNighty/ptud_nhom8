package com.quanlynhakhoa.desktop.menu;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Chạy thử menu Admin độc lập (không cần Spring / CSDL). Thay các panel tạm bằng màn hình thật. */
public final class AdminMenuDemo {

    public static void main(String[] args) {
        boolean collapsed = args.length > 0 && args[0].equals("--collapsed");
        SwingUtilities.invokeLater(() -> show(collapsed));
    }

    private static void show(boolean collapsed) {
        CardLayout cards = new CardLayout();
        JPanel content = new JPanel(cards);
        content.setBackground(new Color(0xF5F6F8));

        List<AdminMenu.Entry> all = new ArrayList<>();
        all.add(AdminMenu.home());
        AdminMenu.groups().forEach(g -> all.addAll(g.entries()));
        for (AdminMenu.Entry e : all) {
            content.add(placeholder(e), e.key());
        }

        SidebarMenu menu = new SidebarMenu("N08 Nha Khoa", "Quản lý phòng khám", null,
                AdminMenu.home(), AdminMenu.groups());
        menu.addSelectionListener(key -> cards.show(content, key));

        // Số việc chờ xử lý — thực tế lấy từ TongQuanService khi tải Tổng quan
        menu.setBadge(AdminMenu.TAI_KHAM, 6);
        menu.setBadge(AdminMenu.DUYET_HUY, 2);
        menu.setBadge(AdminMenu.BAN_GIAO, 2);
        menu.setBadge(AdminMenu.KIEM_KE, 1);
        menu.setBadge(AdminMenu.TAI_KHOAN, 1);

        menu.select(AdminMenu.TONG_QUAN);
        if (collapsed) {
            menu.collapseAll();
            menu.setGroupExpanded("grp-nhan-vien", true);
        }

        JFrame f = new JFrame("N08 Nha Khoa — Quản trị");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout());
        f.add(menu, BorderLayout.WEST);
        f.add(content, BorderLayout.CENTER);
        f.setSize(new Dimension(1280, 820));
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static JPanel placeholder(AdminMenu.Entry e) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0xF5F6F8));
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JLabel title = new JLabel(e.title());
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        JLabel hint = new JLabel(e.hint());
        hint.setForeground(new Color(0x6B7280));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(hint, BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH);
        return p;
    }
}
