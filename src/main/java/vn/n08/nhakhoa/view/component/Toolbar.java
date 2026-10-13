package vn.n08.nhakhoa.view.component;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** Hàng bộ lọc / thao tác phía trên bảng. */
public class Toolbar extends JPanel {
    public Toolbar() {
        super(new WrapLayout(FlowLayout.LEFT, 8, 3));
        setOpaque(false);
    }

    public Toolbar label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.MUTED);
        add(l);
        return this;
    }

    public Toolbar with(JComponent... cs) {
        for (JComponent c : cs) super.add(c);
        return this;
    }
}
