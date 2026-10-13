package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.controller.Navigation;
import vn.n08.nhakhoa.dto.DashboardData;
import vn.n08.nhakhoa.service.DashboardService;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Badge;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.dialog.DoiMatKhauDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khung chính: thanh điều hướng trái (lọc theo quyền), thanh trên (người dùng, vai trò, đổi mật khẩu, đăng xuất),
 * vùng nội dung CardLayout. Màn hình được tạo khi mở lần đầu.
 */
public class MainFrame extends JFrame {
    private static MainFrame current;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Page> pages = new HashMap<>();
    private final Map<String, Navigation.Item> items = new LinkedHashMap<>();
    private final Map<String, NavLink> links = new HashMap<>();
    private String currentId;

    public MainFrame(Runnable onLogout) {
        super("N08 Nha Khoa — " + Session.user().hoTen());
        current = this;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (Session.isLoggedIn()) System.exit(0);
            }
        });
        setSize(1360, 840);
        setMinimumSize(new Dimension(1120, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(sidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(topbar(onLogout), BorderLayout.NORTH);
        content.setBackground(Theme.BG);
        main.add(content, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        go(items.containsKey("dashboard") ? "dashboard" : items.keySet().iterator().next());
    }

    /** Màn hình đang mở (để màn hình khác điều hướng tới). */
    public static MainFrame current() { return current; }

    public void go(String id) {
        Navigation.Item it = items.get(id);
        if (it == null) return;
        Page p = pages.get(id);
        if (p == null) {
            p = it.factory().get();
            pages.put(id, p);
            JScrollPane sp = new JScrollPane(p);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(18);
            content.add(sp, id);
        }
        if (currentId != null && links.get(currentId) != null) links.get(currentId).setActive(false);
        currentId = id;
        links.get(id).setActive(true);
        cards.show(content, id);
        p.reload();
        refreshCounts();
    }

    /** Mở màn hình và trả về Page để thao tác tiếp (VD: chọn sẵn bản ghi). */
    @SuppressWarnings("unchecked")
    public <P extends Page> P open(String id) {
        go(id);
        return (P) pages.get(id);
    }

    public boolean has(String id) { return items.containsKey(id); }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SURFACE);
        side.setPreferredSize(new Dimension(250, 10));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.LINE));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 16));
        brand.setBackground(Theme.SURFACE);
        JLabel logo = new JLabel("<html><span style='color:#3B5594;font-size:15px'><b>N08 Nha Khoa</b></span><br>"
                + "<span style='color:#64748B'>Quản lý phòng khám</span></html>");
        brand.add(logo);
        side.add(brand, BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.SURFACE);
        list.setBorder(BorderFactory.createEmptyBorder(0, 10, 16, 10));
        for (Navigation.Group g : Navigation.groups()) {
            var vis = g.items().stream().filter(Navigation.Item::visible).toList();
            if (vis.isEmpty()) continue;
            if (!g.name().isEmpty()) {
                JLabel h = new JLabel(g.name().toUpperCase());
                h.setFont(Theme.SMALL_B.deriveFont(11f));
                h.setForeground(Theme.FAINT);
                h.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 0));
                h.setAlignmentX(LEFT_ALIGNMENT);
                list.add(h);
            }
            for (Navigation.Item it : vis) {
                items.put(it.id(), it);
                NavLink l = new NavLink(it);
                links.put(it.id(), l);
                list.add(l);
            }
        }
        list.add(Box.createVerticalGlue());
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        side.add(sp, BorderLayout.CENTER);
        return side;
    }

    private JPanel topbar(Runnable onLogout) {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.SURFACE);
        top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), BorderFactory.createEmptyBorder(8, 20, 8, 16)));
        Session.User u = Session.user();
        JLabel note = new JLabel(u.vaiTro() == vn.n08.nhakhoa.model.VaiTro.AD
                ? "Quản trị: thao tác lâm sàng phải chọn bác sĩ chịu trách nhiệm; nhật ký luôn ghi tài khoản " + u.tenDangNhap() + "."
                : "Phiên làm việc của " + u.hoTen());
        note.setFont(Theme.SMALL);
        note.setForeground(Theme.MUTED);
        top.add(note, BorderLayout.WEST);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        r.setOpaque(false);
        JLabel who = new JLabel("<html><b>" + u.hoTen() + "</b> <span style='color:#64748B'>(" + u.tenDangNhap() + ")</span></html>");
        r.add(who);
        r.add(new Badge(u.vaiTro()));
        r.add(Btn.ghost("Đổi mật khẩu", e -> new DoiMatKhauDialog(this).open()));
        r.add(Btn.secondary("Đăng xuất", e -> {
            onLogout.run();
            dispose();
        }));
        top.add(r, BorderLayout.EAST);
        return top;
    }

    /** Đếm việc chờ xử lý cạnh mục điều hướng. */
    public void refreshCounts() {
        if (!Session.can(Quyen.TONG_QUAN)) return;
        Async.run(this, () -> new DashboardService().tai(), (DashboardData d) -> {
            setCount("duyet-huy", d.yeuCauHuyCho());
            setCount("kiem-ke", d.dieuChinhCho());
            setCount("ban-giao", d.banGiaoCho());
            setCount("tai-kham", d.taiKhamDenHan());
        }, e -> { /* đếm phụ trợ: bỏ qua lỗi, màn hình chính sẽ báo lỗi */ });
    }

    private void setCount(String id, Long n) {
        NavLink l = links.get(id);
        if (l != null) l.count(n == null ? 0 : n);
    }

    private class NavLink extends JPanel {
        private final JLabel text;
        private final JLabel cnt = new JLabel();
        private boolean active;

        NavLink(Navigation.Item it) {
            super(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(it.uc());
            text = new JLabel(it.label());
            text.setFont(Theme.BODY);
            text.setForeground(Theme.INK2);
            cnt.setFont(Theme.SMALL_B);
            cnt.setForeground(Color.WHITE);
            cnt.setOpaque(true);
            cnt.setBackground(Theme.RED);
            cnt.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            cnt.setVisible(false);
            add(text, BorderLayout.CENTER);
            add(cnt, BorderLayout.EAST);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { go(it.id()); }
                @Override public void mouseEntered(MouseEvent e) { if (!active) { text.setForeground(Theme.PRI_INK); } }
                @Override public void mouseExited(MouseEvent e) { if (!active) text.setForeground(Theme.INK2); }
            });
        }

        void setActive(boolean a) {
            active = a;
            text.setForeground(a ? Theme.PRI_INK : Theme.INK2);
            text.setFont(a ? Theme.BODY_B : Theme.BODY);
            repaint();
        }

        void count(long n) {
            cnt.setText(String.valueOf(n));
            cnt.setVisible(n > 0);
        }

        @Override protected void paintComponent(Graphics g) {
            if (active) {
                g.setColor(Theme.NAV_ACT);
                ((java.awt.Graphics2D) g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            super.paintComponent(g);
        }
    }
}
