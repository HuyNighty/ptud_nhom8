package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Labeled;
import vn.n08.nhakhoa.model.Tone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Nhãn trạng thái dạng viên thuốc, màu theo Tone. */
public class Badge extends JLabel {
    private Tone tone = Tone.GRAY;

    public Badge() {
        setFont(Theme.SMALL_B);
        setBorder(BorderFactory.createEmptyBorder(2, 9, 2, 9));
        setOpaque(false);
    }

    public Badge(Labeled v) {
        this();
        set(v);
    }

    public Badge(String text, Tone t) {
        this();
        set(text, t);
    }

    public void set(Labeled v) {
        if (v == null) set("—", Tone.GRAY);
        else set(v.label(), v.tone());
    }

    public void set(String text, Tone t) {
        tone = t;
        setText(text);
        setForeground(Theme.fg(t));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getText() != null && !getText().isEmpty() && !"—".equals(getText())) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.bg(tone));
            int w = Math.min(getWidth(), getPreferredSize().width);
            int h = Math.min(getHeight(), 22);
            g2.fillRoundRect(0, (getHeight() - h) / 2, w, h, h, h);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
