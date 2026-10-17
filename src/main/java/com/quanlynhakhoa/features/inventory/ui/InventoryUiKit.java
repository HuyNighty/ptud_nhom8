package com.quanlynhakhoa.features.inventory.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Màu, font và component vẽ tay dùng chung cho các màn hình kho (không thêm dependency). */
final class InventoryUiKit {

    static final Color BACKGROUND = new Color(0xEEF0F5);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xDCE0E8);
    static final Color HEADER_ROW = new Color(0xF3F5F9);
    static final Color TEXT = new Color(0x1B2233);
    static final Color TEXT_MUTED = new Color(0x5E6678);
    static final Color PRIMARY = new Color(0x26366B);
    static final Color PRIMARY_HOVER = new Color(0x31458A);
    static final Color SELECTED_FILL = new Color(0xE3E8F6);
    static final Color NUMBER_BLUE = new Color(0x1E3E9C);
    static final Color ORANGE = new Color(0xE07B12);
    static final Color RED = new Color(0xD32F2F);
    static final Color GREEN = new Color(0x2E7D32);
    static final Color AMBER = new Color(0xC99A06);
    static final Color PROTOTYPE_FILL = new Color(0xFFF4E0);
    static final Color PROTOTYPE_TEXT = new Color(0x8A5300);

    private static final String FAMILY = pickFamily();

    private InventoryUiKit() {
    }

    private static String pickFamily() {
        Set<String> available = Set.copyOf(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[] {"Segoe UI", "Inter", "Roboto", "Noto Sans", "DejaVu Sans"}) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Thẻ nền trắng bo góc có viền mảnh. */
    static final class Card extends JPanel {
        private final int arc;

        Card(LayoutManager layout, int padding) {
            this(layout, padding, 14);
        }

        Card(LayoutManager layout, int padding, int arc) {
            super(layout);
            this.arc = arc;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    enum ButtonKind { PRIMARY, OUTLINE, CHIP }

    static JButton button(String text, ButtonKind kind) {
        JButton button = new JButton(text);
        styleButton(button, kind);
        return button;
    }

    static JToggleButton chip(String text) {
        JToggleButton chip = new JToggleButton(text);
        styleButton(chip, ButtonKind.CHIP);
        return chip;
    }

    private static void styleButton(AbstractButton button, ButtonKind kind) {
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(font(kind == ButtonKind.PRIMARY ? Font.BOLD : Font.PLAIN, kind == ButtonKind.CHIP ? 13 : 14));
        button.setForeground(kind == ButtonKind.PRIMARY ? Color.WHITE : TEXT);
        int vertical = kind == ButtonKind.CHIP ? 6 : 10;
        button.setBorder(BorderFactory.createEmptyBorder(vertical, 14, vertical, 14));
        button.putClientProperty("n08.kind", kind);
        button.setUI(new RoundedButtonUI());
    }

    private static final class RoundedButtonUI extends javax.swing.plaf.basic.BasicButtonUI {
        @Override
        public void paint(Graphics g, javax.swing.JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonKind kind = (ButtonKind) b.getClientProperty("n08.kind");
            boolean hover = b.getModel().isRollover();
            boolean selected = b.isSelected();
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            int w = c.getWidth() - 1;
            int h = c.getHeight() - 1;
            switch (kind) {
                case PRIMARY -> {
                    g2.setColor(hover ? PRIMARY_HOVER : PRIMARY);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                case OUTLINE -> {
                    g2.setColor(hover ? HEADER_ROW : SURFACE);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(BORDER);
                    g2.drawRoundRect(0, 0, w, h, 10, 10);
                }
                case CHIP -> {
                    g2.setColor(selected ? SELECTED_FILL : (hover ? HEADER_ROW : SURFACE));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(selected ? PRIMARY : BORDER);
                    g2.setStroke(new BasicStroke(selected ? 1.4f : 1f));
                    g2.drawRoundRect(0, 0, w, h, 10, 10);
                }
            }
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Chấm tròn màu trạng thái (dùng trong bảng và danh sách cần chú ý). */
    record DotIcon(Color color, int size) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
