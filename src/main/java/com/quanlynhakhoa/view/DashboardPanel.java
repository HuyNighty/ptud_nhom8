package com.quanlynhakhoa.view;

import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** HOME-UI-01: read-only dashboard shell. No fake patient or revenue data. */
public final class DashboardPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);

    public DashboardPanel(DesktopSession session) {
        setLayout(new BorderLayout(0, 22));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        add(introduction(session), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(metrics());
        body.add(Box.createVerticalStrut(18));
        body.add(panels());
        body.add(Box.createVerticalStrut(18));
        body.add(footnote());
        add(body, BorderLayout.CENTER);
    }

    private JPanel introduction(DesktopSession session) {
        JPanel panel = card(new BorderLayout(16, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 24, 23, 24)));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = label("Chào mừng đến hệ thống quản lý nha khoa", 23, NAVY, true);
        JLabel sub = label("Tổng quan hoạt động phòng khám · Giao diện dành cho Nhóm 8", 13, MUTED, false);
        copy.add(title);
        copy.add(Box.createVerticalStrut(9));
        copy.add(sub);
        panel.add(copy, BorderLayout.CENTER);
        JLabel account = label("Tài khoản: " + session.accountId(), 12, NAVY, true);
        account.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(account, BorderLayout.EAST);
        return panel;
    }

    private JPanel metrics() {
        JPanel row = new JPanel(new GridLayout(1, 4, 13, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 123));
        row.add(metric("Bệnh nhân", "Dữ liệu sẽ do module Bệnh nhân cung cấp"));
        row.add(metric("Lịch hẹn hôm nay", "Dữ liệu sẽ do module Lịch hẹn cung cấp"));
        row.add(metric("Đang điều trị", "Dữ liệu sẽ do module Khám cung cấp"));
        row.add(metric("Doanh thu", "Dữ liệu sẽ do module Hóa đơn cung cấp"));
        return row;
    }

    private JPanel metric(String title, String description) {
        JPanel panel = card(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 17, 17, 17)));
        panel.add(label(title, 13, MUTED, true), BorderLayout.NORTH);
        panel.add(label("—", 28, NAVY, true), BorderLayout.CENTER);
        JLabel note = label("Chưa kết nối dữ liệu", 11, MUTED, false);
        note.setToolTipText(description);
        panel.add(note, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panels() {
        JPanel row = new JPanel(new GridLayout(1, 2, 15, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        row.add(emptySection("Lịch hẹn hôm nay", "Các lịch hẹn được hiển thị tại đây sau khi module Lịch hẹn được tích hợp."));
        row.add(emptySection("Hoạt động gần đây", "Hoạt động khám, tiếp nhận và thanh toán sẽ xuất hiện tại đây khi có dữ liệu."));
        return row;
    }

    private JPanel emptySection(String title, String description) {
        JPanel panel = card(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(22, 22, 22, 22)));
        panel.add(label(title, 17, NAVY, true), BorderLayout.NORTH);
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        JLabel marker = label("Chưa có dữ liệu để hiển thị", 16, NAVY, false);
        marker.setAlignmentX(CENTER_ALIGNMENT);
        JLabel detail = label("Chờ module nghiệp vụ được tích hợp", 12, MUTED, false);
        detail.setAlignmentX(CENTER_ALIGNMENT);
        empty.add(Box.createVerticalGlue());
        empty.add(marker);
        empty.add(Box.createVerticalStrut(10));
        empty.add(detail);
        empty.add(Box.createVerticalGlue());
        panel.add(empty, BorderLayout.CENTER);
        JLabel footer = label(description, 11, MUTED, false);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel footnote() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panel.setOpaque(false);
        panel.add(label("HOME-UI-01 · Trang chủ hiển thị cấu trúc, chưa có dữ liệu nghiệp vụ thực.",
                12, MUTED, false));
        return panel;
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }
}
