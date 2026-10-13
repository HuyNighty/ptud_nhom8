package vn.n08.nhakhoa.view.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;

/** Thông báo ngắn góc dưới phải cửa sổ, tự ẩn sau 3 giây. */
public final class Toast {
    private Toast() {}

    public static void show(Component anyInWindow, String msg) {
        Window w = anyInWindow instanceof Window x ? x : SwingUtilities.getWindowAncestor(anyInWindow);
        while (w != null && !(w instanceof javax.swing.JFrame) && w.getOwner() != null) w = w.getOwner();
        if (!(w instanceof RootPaneContainer rpc)) return;
        JRootPane root = rpc.getRootPane();
        JLabel l = new JLabel("<html><div style='width:300px'>✓ " + DataTable.esc(msg) + "</div></html>") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ADMIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setForeground(java.awt.Color.WHITE);
        l.setFont(Theme.BODY);
        l.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        Dimension d = l.getPreferredSize();
        JLayeredPane lp = root.getLayeredPane();
        l.setBounds(lp.getWidth() - d.width - 24, lp.getHeight() - d.height - 24, d.width, d.height);
        lp.add(l, JLayeredPane.POPUP_LAYER);
        lp.repaint();
        Timer t = new Timer(3200, e -> {
            lp.remove(l);
            lp.repaint();
        });
        t.setRepeats(false);
        t.start();
    }
}
