package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

/** Hộp thoại chỉ đọc (chi tiết, lịch sử) với nút Đóng. */
public class XemDialog extends JDialog {
    public XemDialog(Component p, String title, JComponent content, int w, int h) {
        super(SwingUtilities.getWindowAncestor(p), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.SURFACE);
        JLabel t = new JLabel(title);
        t.setFont(Theme.H2);
        t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), BorderFactory.createEmptyBorder(14, 20, 12, 20)));
        add(t, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.SURFACE);
        body.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        body.add(content);
        add(body, BorderLayout.CENTER);
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        f.setBackground(Theme.HEAD);
        f.add(Btn.primary("Đóng", e -> dispose()));
        add(f, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(new Dimension(w, h));
        setLocationRelativeTo(getOwner());
    }

    public static void show(Component p, String title, JComponent content, int w, int h) {
        new XemDialog(p, title, content, w, h).setVisible(true);
    }
}
