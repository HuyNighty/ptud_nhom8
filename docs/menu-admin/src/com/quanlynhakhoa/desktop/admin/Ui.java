package com.quanlynhakhoa.desktop.admin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/** Màu, font và các hàm tạo component dùng chung cho giao diện Admin (Swing thuần, không thư viện ngoài). */
public final class Ui {

    private Ui() {
    }

    public static final Color BG = new Color(0xF5F6F8);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color GRID = new Color(0xEEF0F3);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color STRONG = new Color(0x111827);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color SELECTED = new Color(0xE8EFFD);
    public static final Color HEADER_BG = new Color(0xF9FAFB);

    public static final Color BLUE = new Color(0x2563EB);
    public static final Color GREEN = new Color(0x15803D);
    public static final Color RED = new Color(0xDC2626);
    public static final Color AMBER = new Color(0xC2410C);
    public static final Color VIOLET = new Color(0x7C3AED);
    public static final Color SLATE = new Color(0x475569);

    public static final String FAMILY = pickFamily();

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Nút phẳng: primary = nền xanh chữ trắng; còn lại nền trắng viền xám. */
    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(primary ? ACCENT : Color.WHITE);
        b.setForeground(primary ? Color.WHITE : TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Nút dạng liên kết (chữ xanh, không viền). */
    public static JButton link(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 13));
        b.setForeground(ACCENT);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Nhãn có nền màu nhạt (badge). */
    public static JLabel badge(String text, Color fg, Color bg) {
        JLabel l = label(text, Font.BOLD, 12, fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return l;
    }

    public static Border cardBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    public static Color tint(Color c) {
        return new Color(
                255 - (255 - c.getRed()) / 10,
                255 - (255 - c.getGreen()) / 10,
                255 - (255 - c.getBlue()) / 10);
    }

    /** Định dạng JTable: dòng cao, chỉ kẻ ngang, header phẳng. */
    public static void styleTable(JTable t) {
        t.setFont(font(Font.PLAIN, 13));
        t.setForeground(TEXT);
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(GRID);
        t.setIntercellSpacing(new java.awt.Dimension(0, 1));
        t.setSelectionBackground(SELECTED);
        t.setSelectionForeground(STRONG);
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setDefaultEditor(Object.class, null);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setResizingAllowed(false);

        JTableHeader h = t.getTableHeader();
        h.setPreferredSize(new java.awt.Dimension(0, 32));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, false, false, r, c);
                l.setFont(font(Font.BOLD, 12));
                l.setForeground(MUTED);
                l.setBackground(HEADER_BG);
                l.setOpaque(true);
                l.setHorizontalAlignment(table.getColumnModel().getColumn(c).getCellRenderer() instanceof RightRenderer
                        ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                return l;
            }
        });
        DefaultTableCellRenderer padded = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, s, false, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return l;
            }
        };
        t.setDefaultRenderer(Object.class, padded);
    }

    /** Renderer căn phải cho cột số. */
    public static final class RightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, s, false, r, c);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            l.setFont(font(Font.BOLD, 13));
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            return l;
        }
    }

    public static JScrollPane flatScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    private static String pickFamily() {
        try {
            Set<String> av = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String f : new String[]{"Segoe UI", "Inter", "Noto Sans", "Arial"}) {
                if (av.contains(f)) {
                    return f;
                }
            }
        } catch (Exception ignored) {
            // headless
        }
        return Font.DIALOG;
    }
}
