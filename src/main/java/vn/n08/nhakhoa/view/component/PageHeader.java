package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Tiêu đề trang: tên chức năng, mô tả ngắn (UC liên quan) và nút thao tác chính. */
public class PageHeader extends JPanel {
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    public PageHeader(String title, String sub) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(title);
        h.setFont(Theme.H1);
        h.setForeground(Theme.INK);
        t.add(h);
        if (sub != null) {
            JLabel s = new JLabel(sub);
            s.setFont(Theme.SMALL);
            s.setForeground(Theme.MUTED);
            s.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            t.add(s);
        }
        add(t, BorderLayout.WEST);
        actions.setOpaque(false);
        add(actions, BorderLayout.EAST);
    }

    public PageHeader action(JComponent c) {
        actions.add(c);
        return this;
    }
}
