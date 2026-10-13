package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.util.CanhBaoException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/** Hộp thoại thông báo, xác nhận, nhập lý do, xác nhận cảnh báo — cùng một phong cách. */
public final class Dialogs {
    private Dialogs() {}

    public static void info(Component p, String msg) { message(p, "Thông báo", msg, Theme.PRI); }

    public static void error(Component p, String msg) { message(p, "Không thực hiện được", msg, Theme.RED); }

    private static void message(Component p, String title, String msg, Color accent) {
        JDialog d = base(p, title);
        d.add(body(title, msg, accent, null), BorderLayout.CENTER);
        d.add(footer(Btn.primary("Đóng", e -> d.dispose())), BorderLayout.SOUTH);
        show(d);
    }

    public static boolean confirm(Component p, String title, String msg, String okText, boolean danger) {
        JDialog d = base(p, title);
        boolean[] ok = {false};
        d.add(body(title, msg, danger ? Theme.RED : Theme.PRI, null), BorderLayout.CENTER);
        Btn yes = danger ? Btn.danger(okText, e -> { ok[0] = true; d.dispose(); }) : Btn.primary(okText, e -> { ok[0] = true; d.dispose(); });
        d.add(footer(Btn.secondary("Hủy bỏ", e -> d.dispose()), yes), BorderLayout.SOUTH);
        show(d);
        return ok[0];
    }

    /** Hỏi lý do bắt buộc; trả null nếu người dùng bỏ qua. */
    public static String reason(Component p, String title, String prompt, String okText, boolean danger) {
        JDialog d = base(p, title);
        JTextArea a = Fields.area(4);
        JLabel err = new JLabel(" ");
        err.setForeground(Theme.RED);
        err.setFont(Theme.SMALL);
        JPanel extra = new JPanel(new BorderLayout(0, 4));
        extra.setOpaque(false);
        extra.add(Fields.scroll(a), BorderLayout.CENTER);
        extra.add(err, BorderLayout.SOUTH);
        String[] out = {null};
        d.add(body(title, prompt, danger ? Theme.RED : Theme.PRI, extra), BorderLayout.CENTER);
        Runnable act = () -> {
            if (a.getText().isBlank()) { err.setText("Nội dung này là bắt buộc."); return; }
            out[0] = a.getText().trim();
            d.dispose();
        };
        Btn yes = danger ? Btn.danger(okText, e -> act.run()) : Btn.primary(okText, e -> act.run());
        d.add(footer(Btn.secondary("Hủy bỏ", e -> d.dispose()), yes), BorderLayout.SOUTH);
        show(d);
        return out[0];
    }

    /** Hiển thị danh sách cảnh báo; true nếu người dùng xác nhận vẫn tiếp tục. */
    public static boolean warnings(Component p, CanhBaoException e, String okText) {
        StringBuilder sb = new StringBuilder("<html><div style='width:430px'>").append(DataTable.esc(e.getMessage())).append("<ul>");
        for (String w : e.canhBao()) sb.append("<li style='margin-bottom:4px'>").append(DataTable.esc(w)).append("</li>");
        sb.append("</ul></div></html>");
        return confirm(p, "Cần xác nhận", sb.toString(), okText, true);
    }

    private static JDialog base(Component p, String title) {
        Window w = p == null ? null : (p instanceof Window pw ? pw : SwingUtilities.getWindowAncestor(p));
        JDialog d = new JDialog(w, title, JDialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());
        d.getContentPane().setBackground(Theme.SURFACE);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    private static JPanel body(String title, String msg, Color accent, JComponent extra) {
        JPanel b = new JPanel(new BorderLayout(0, 8));
        b.setBackground(Theme.SURFACE);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(4, 0, 0, 0, accent), BorderFactory.createEmptyBorder(16, 20, 8, 20)));
        JLabel t = new JLabel(title);
        t.setFont(Theme.H2);
        t.setForeground(Theme.INK);
        b.add(t, BorderLayout.NORTH);
        String html = msg.startsWith("<html>") ? msg : "<html><div style='width:400px'>" + DataTable.esc(msg).replace("\n", "<br>") + "</div></html>";
        JLabel m = new JLabel(html);
        m.setFont(Theme.BODY);
        m.setForeground(Theme.INK2);
        b.add(m, BorderLayout.CENTER);
        if (extra != null) b.add(extra, BorderLayout.SOUTH);
        return b;
    }

    static JPanel footer(JComponent... buttons) {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        f.setBackground(Theme.HEAD);
        f.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));
        for (JComponent b : buttons) f.add(b);
        return f;
    }

    private static void show(JDialog d) {
        d.pack();
        d.setMinimumSize(new Dimension(420, d.getHeight()));
        d.setLocationRelativeTo(d.getOwner());
        d.setVisible(true);
    }
}
