package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.config.Database;
import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.controller.ErrorHandler;
import vn.n08.nhakhoa.service.AuthService;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

/** UC38 Màn hình đăng nhập: kiểm tra kết nối CSDL, báo lỗi cụ thể (sai, bị khóa, vô hiệu). */
public class LoginFrame extends JFrame {
    private final HintField user = Fields.text("tên đăng nhập");
    private final JPasswordField pass = new JPasswordField(18);
    private final JLabel err = new JLabel(" ");
    private final JLabel db = new JLabel("Đang kiểm tra kết nối cơ sở dữ liệu…");
    private final Btn ok;

    public LoginFrame(AuthService auth, Consumer<LoginFrame> onSuccess) {
        super("N08 Nha Khoa — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 560);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(brand(), BorderLayout.WEST);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BG);
        Card card = new Card(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        card.setPreferredSize(new Dimension(380, 390));
        JLabel t = new JLabel("Đăng nhập");
        t.setFont(Theme.H1);
        JLabel s = new JLabel("Dùng tài khoản được quản lý phòng khám cấp.");
        s.setFont(Theme.SMALL);
        s.setForeground(Theme.MUTED);
        card.add(left(t));
        card.add(left(s));
        card.add(Box.createVerticalStrut(18));
        card.add(left(label("Tên đăng nhập")));
        card.add(left(user));
        card.add(Box.createVerticalStrut(10));
        card.add(left(label("Mật khẩu")));
        card.add(left(pass));
        card.add(Box.createVerticalStrut(8));
        err.setForeground(Theme.RED);
        err.setFont(Theme.SMALL);
        card.add(left(err));
        card.add(Box.createVerticalStrut(10));
        ok = Btn.primary("Đăng nhập", e -> {
            err.setText(" ");
            String u = user.getText();
            String p = new String(pass.getPassword());
            setBusy(true);
            Async.run(this, () -> auth.login(u, p), r -> onSuccess.accept(this), ex -> {
                setBusy(false);
                err.setText("<html><div style='width:300px'>" + ErrorHandler.message(ex) + "</div></html>");
                pass.setText("");
                pass.requestFocusInWindow();
            });
        });
        ok.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(left(ok));
        card.add(Box.createVerticalGlue());
        db.setFont(Theme.SMALL);
        db.setForeground(Theme.MUTED);
        card.add(left(db));
        right.add(card);
        add(right, BorderLayout.CENTER);
        getRootPane().setDefaultButton(ok);
        for (JComponent c : new JComponent[]{user, pass}) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        checkDb();
    }

    private void setBusy(boolean b) {
        ok.setEnabled(!b);
        ok.setText(b ? "Đang đăng nhập…" : "Đăng nhập");
    }

    private void checkDb() {
        Async.run(this, Database::ping, v -> {
            db.setText("● Đã kết nối: " + v.split("\\s+-")[0]);
            db.setForeground(Theme.GREEN);
        }, e -> {
            db.setText("<html><div style='width:310px'>● " + ErrorHandler.message(e) + "</div></html>");
            db.setForeground(Theme.RED);
        });
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.INK2);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private static Component left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel brand() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Theme.LOGO, 0, getHeight(), Theme.ADMIN));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setPreferredSize(new Dimension(360, 10));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(48, 36, 36, 36));
        JLabel a = new JLabel("N08 NHA KHOA");
        a.setFont(Theme.H2);
        a.setForeground(new Color(0xC7D2FE));
        JLabel b = new JLabel("<html>Quản lý<br>phòng khám nha khoa</html>");
        b.setFont(Theme.H1.deriveFont(26f));
        b.setForeground(Color.WHITE);
        JLabel c = new JLabel("<html><div style='width:220px'>Tiếp nhận · Lịch hẹn · Khám & điều trị · Thu ngân · Nhân sự · Kho</div></html>");
        c.setFont(Theme.BODY);
        c.setForeground(new Color(0xCBD5E1));
        p.add(a);
        p.add(Box.createVerticalStrut(12));
        p.add(b);
        p.add(Box.createVerticalStrut(16));
        p.add(c);
        p.add(Box.createVerticalGlue());
        JLabel f = new JLabel("Đồ án nhóm N08 · Java Swing · JDBC · SQL Server");
        f.setFont(Theme.SMALL);
        f.setForeground(new Color(0x94A3B8));
        p.add(f);
        return p;
    }
}
