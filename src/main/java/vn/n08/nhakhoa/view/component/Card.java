package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Khung nền trắng bo góc, có thể kèm tiêu đề và nút ở phải. */
public class Card extends JPanel {
    public Card(LayoutManager lm) {
        super(lm);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    public Card() {
        this(new BorderLayout(0, 10));
    }

    /** Card có tiêu đề (và phụ đề) + thân. */
    public static Card titled(String title, String sub, JComponent body, JComponent... actions) {
        Card c = new Card(new BorderLayout(0, 10));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new javax.swing.BoxLayout(t, javax.swing.BoxLayout.Y_AXIS));
        JLabel l = new JLabel(title);
        l.setFont(Theme.H2);
        l.setForeground(Theme.INK);
        t.add(l);
        if (sub != null) {
            JLabel s = new JLabel(sub);
            s.setFont(Theme.SMALL);
            s.setForeground(Theme.MUTED);
            t.add(s);
        }
        head.add(t, BorderLayout.WEST);
        if (actions.length > 0) {
            JPanel a = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            a.setOpaque(false);
            for (JComponent x : actions) a.add(x);
            head.add(a, BorderLayout.EAST);
        }
        c.add(head, BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(Theme.LINE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}
