package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Tone;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Ô số liệu: nhãn, giá trị lớn, dòng gợi ý; vạch màu theo sắc thái. */
public class StatCard extends Card {
    private final JLabel value = new JLabel("—");
    private final JLabel hint = new JLabel(" ");
    private Color accent;

    public StatCard(String label, Tone tone) {
        super(null);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        accent = Theme.fg(tone);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 16));
        JLabel l = new JLabel(label);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.MUTED);
        value.setFont(Theme.BIG);
        value.setForeground(Theme.INK);
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.MUTED);
        add(l);
        add(value);
        add(hint);
    }

    public void set(String v, String h) {
        value.setText(v);
        hint.setText(h == null || h.isEmpty() ? " " : h);
    }

    public void tone(Tone t) {
        accent = Theme.fg(t);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(accent);
        g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
        g2.dispose();
    }
}
