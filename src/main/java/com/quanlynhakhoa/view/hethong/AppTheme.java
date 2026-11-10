package com.quanlynhakhoa.view.hethong;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/** Shared visual language for business pages and their dialogs. */
public final class AppTheme {
    public static final Color PRIMARY = new Color(29, 61, 102);
    public static final Color BACKGROUND = new Color(245, 248, 252);
    public static final Color INK = new Color(28, 48, 75);
    public static final Color MUTED = new Color(97, 115, 137);
    public static final Color LINE = new Color(220, 229, 239);
    public static final Color SELECTION = new Color(225, 237, 252);
    private static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    private static boolean installed;

    private AppTheme() { }

    public static void install() {
        if (installed) return;
        installed = true;
        String[] controls = {"Label", "Button", "TextField", "PasswordField", "TextArea",
                "ComboBox", "CheckBox", "RadioButton", "Table", "TableHeader", "TabbedPane",
                "List", "Spinner", "OptionPane", "TitledBorder"};
        for (String control : controls) UIManager.put(control + ".font", BODY);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", SELECTION);
        UIManager.put("ComboBox.selectionForeground", INK);
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override public void eventDispatched(AWTEvent event) {
                if (event instanceof WindowEvent windowEvent
                        && windowEvent.getID() == WindowEvent.WINDOW_OPENED
                        && windowEvent.getWindow() instanceof JDialog dialog
                        && belongsToApplication(dialog)) {
                    apply(dialog.getContentPane());
                    Dimension previous = dialog.getSize();
                    Dimension preferred = dialog.getPreferredSize();
                    Rectangle available = dialog.getGraphicsConfiguration().getBounds();
                    dialog.setSize(Math.min(available.width - 48, Math.max(previous.width, preferred.width)),
                            Math.min(available.height - 80, Math.max(previous.height, preferred.height)));
                    dialog.validate();
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private static boolean belongsToApplication(Window window) {
        for (Window owner = window.getOwner(); owner != null; owner = owner.getOwner()) {
            if (owner instanceof MainFrame) return true;
        }
        return false;
    }

    public static void applyPage(JPanel page) {
        page.setBackground(BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));
        apply(page);
    }

    public static void apply(Component component) {
        if (component instanceof JLabel label) {
            Font old = label.getFont();
            boolean value = Boolean.TRUE.equals(label.getClientProperty("billing.value"));
            if (value || old != null && old.getSize() >= 20) {
                label.setFont(BODY.deriveFont(Font.BOLD, value ? 24f : 23f));
                label.setForeground(PRIMARY);
            } else {
                label.setFont(BODY.deriveFont(old == null ? Font.PLAIN : old.getStyle(),
                        old != null && old.getSize() >= 16 ? 17f : 13f));
            }
        } else if (component instanceof JButton button) {
            styleButton(button);
        } else if (component instanceof javax.swing.text.JTextComponent text) {
            text.setFont(BODY);
            if (text.isEditable()) {
                text.setBackground(Color.WHITE);
                text.setForeground(INK);
                text.setCaretColor(PRIMARY);
                text.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }
        } else if (component instanceof JTable table) {
            table.setFont(BODY);
            table.setRowHeight(34);
            table.setForeground(INK);
            table.setBackground(Color.WHITE);
            table.setSelectionBackground(SELECTION);
            table.setSelectionForeground(INK);
            table.setGridColor(LINE);
            table.setShowHorizontalLines(true);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(8, 1));
            table.getTableHeader().setFont(BODY.deriveFont(Font.BOLD));
            table.getTableHeader().setForeground(PRIMARY);
            table.getTableHeader().setBackground(new Color(237, 242, 249));
            table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        } else if (component instanceof JComboBox<?> combo) {
            combo.setFont(BODY);
            combo.setBackground(Color.WHITE);
            combo.setForeground(INK);
            Dimension size = combo.getPreferredSize();
            combo.setPreferredSize(new Dimension(size.width, Math.max(34, size.height)));
            // Keep the look-and-feel arrow button intact.
            return;
        } else if (component instanceof JSpinner spinner) {
            spinner.setFont(BODY);
            return;
        } else if (component instanceof JTabbedPane tabs) {
            tabs.setFont(BODY.deriveFont(Font.BOLD));
            tabs.setForeground(PRIMARY);
            tabs.setBackground(BACKGROUND);
        } else if (component instanceof JCheckBox || component instanceof JRadioButton) {
            component.setFont(BODY);
        }
        if (component instanceof JComponent control && control.getBorder() instanceof TitledBorder title) {
            title.setTitleFont(BODY.deriveFont(Font.BOLD));
            title.setTitleColor(PRIMARY);
            title.setBorder(BorderFactory.createLineBorder(LINE));
        }
        if (component instanceof JPanel panel && panel.isOpaque()) {
            Color fill = panel.getBackground();
            if (fill != null && !Color.WHITE.equals(fill)
                    && fill.getRed() >= 230 && fill.getGreen() >= 230 && fill.getBlue() >= 230) {
                panel.setBackground(BACKGROUND);
            }
        }
        if (component instanceof JScrollPane scroll) {
            scroll.getViewport().setBackground(Color.WHITE);
            if (!(scroll.getBorder() instanceof TitledBorder)) {
                scroll.setBorder(BorderFactory.createLineBorder(LINE));
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) apply(child);
        }
    }

    public static void applyButtons(Component component) {
        if (component instanceof JButton button) styleButton(button);
        else if (component instanceof Container container && !(component instanceof JComboBox<?>)
                && !(component instanceof JSpinner)) {
            for (Component child : container.getComponents()) applyButtons(child);
        }
    }

    private static void styleButton(JButton button) {
        if (button.getText() == null || button.getText().isBlank()) return;
        button.setFont(BODY.deriveFont(Font.BOLD));
        button.setUI(new ActionButtonUI());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        boolean compact = Boolean.TRUE.equals(button.getClientProperty("theme.compact"));
        button.setMargin(compact ? new Insets(2, 2, 2, 2) : new Insets(8, 14, 8, 14));
        button.setBorder(compact ? BorderFactory.createEmptyBorder(2, 2, 2, 2)
                : BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(isPrimary(button) ? Color.WHITE : PRIMARY);
        Dimension size = button.getPreferredSize();
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText()) + (compact ? 8 : 32);
        if (button.getIcon() != null) textWidth += button.getIcon().getIconWidth() + button.getIconTextGap();
        button.setPreferredSize(new Dimension(Math.max(size.width, textWidth), Math.max(compact ? 26 : 36, size.height)));
    }

    private static boolean isPrimary(JButton button) {
        String text = button.getText();
        return Boolean.TRUE.equals(button.getClientProperty("theme.primary"))
                || Boolean.TRUE.equals(button.getClientProperty("theme.selected"))
                || Boolean.TRUE.equals(button.getClientProperty("billing.primary"))
                || text.startsWith("Thêm") || text.startsWith("+ Thêm") || text.startsWith("Lưu")
                || text.startsWith("Tạo ") || text.startsWith("+ Đặt")
                || text.startsWith("Xác nhận") || text.equals("Thu tiền");
    }

    private static final class ActionButtonUI extends BasicButtonUI {
        @Override public void paint(Graphics graphics, JComponent component) {
            JButton button = (JButton) component;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean primary = isPrimary(button);
                Color fill = primary ? PRIMARY : Color.WHITE;
                if (!button.isEnabled()) fill = new Color(237, 241, 246);
                else if (button.getModel().isPressed()) fill = primary ? new Color(20, 44, 75) : SELECTION;
                else if (button.getModel().isRollover()) fill = primary ? new Color(40, 79, 128) : new Color(240, 245, 252);
                g.setColor(fill);
                g.fillRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 8, 8);
                g.setColor(button.hasFocus() ? PRIMARY : LINE);
                g.setStroke(new BasicStroke(button.hasFocus() ? 2f : 1f));
                g.drawRoundRect(1, 1, button.getWidth() - 3, button.getHeight() - 3, 8, 8);
                super.paint(g, component);
            } finally {
                g.dispose();
            }
        }

        @Override protected void paintText(Graphics graphics, AbstractButton button, Rectangle bounds, String text) {
            // BasicButtonUI embosses disabled text with two offset draws. Keep one clear text layer.
            graphics.setColor(!button.isEnabled() ? MUTED : isPrimary((JButton) button) ? Color.WHITE : PRIMARY);
            FontMetrics metrics = graphics.getFontMetrics();
            BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, text, button.getDisplayedMnemonicIndex(),
                    bounds.x + getTextShiftOffset(), bounds.y + metrics.getAscent() + getTextShiftOffset());
        }
    }
}
