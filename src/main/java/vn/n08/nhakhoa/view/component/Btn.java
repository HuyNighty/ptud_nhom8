package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

/** Nút bo góc theo kiểu prototype: primary (xanh), secondary (viền), danger (đỏ), ghost (chữ). */
public class Btn extends JButton {
    public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST, SUCCESS }

    private final Kind kind;

    public Btn(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.BODY_B);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        setForeground(switch (kind) {
            case PRIMARY, DANGER, SUCCESS -> Color.WHITE;
            case GHOST -> Theme.PRI_INK;
            default -> Theme.INK2;
        });
    }

    public static Btn primary(String t, ActionListener a) { return with(new Btn(t, Kind.PRIMARY), a); }
    public static Btn secondary(String t, ActionListener a) { return with(new Btn(t, Kind.SECONDARY), a); }
    public static Btn danger(String t, ActionListener a) { return with(new Btn(t, Kind.DANGER), a); }
    public static Btn ghost(String t, ActionListener a) { return with(new Btn(t, Kind.GHOST), a); }
    public static Btn success(String t, ActionListener a) { return with(new Btn(t, Kind.SUCCESS), a); }

    private static Btn with(Btn b, ActionListener a) {
        if (a != null) b.addActionListener(a);
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hover = getModel().isRollover() && isEnabled();
        Color fill, line = null;
        switch (kind) {
            case PRIMARY -> fill = hover ? Theme.PRI_INK : Theme.PRI;
            case DANGER -> fill = hover ? Theme.RED.darker() : Theme.RED;
            case SUCCESS -> fill = hover ? Theme.GREEN.darker() : Theme.GREEN;
            case GHOST -> fill = hover ? Theme.PRI_SOFT : null;
            default -> { fill = hover ? Theme.HEAD : Theme.SURFACE; line = Theme.LINE; }
        }
        if (!isEnabled()) { fill = kind == Kind.GHOST ? null : Theme.LINE2; line = null; }
        int w = getWidth(), h = getHeight();
        if (fill != null) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
        }
        if (line != null) {
            g2.setColor(line);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
        }
        g2.dispose();
        if (!isEnabled()) setForeground(Theme.FAINT);
        super.paintComponent(g);
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setForeground(!b ? Theme.FAINT : switch (kind) {
            case PRIMARY, DANGER, SUCCESS -> Color.WHITE;
            case GHOST -> Theme.PRI_INK;
            default -> Theme.INK2;
        });
    }
}
