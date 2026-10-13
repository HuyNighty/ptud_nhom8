package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.util.Fmt;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

/** Dòng thời gian lịch sử xử lý (từ NhatKyThaoTac): thời điểm · tài khoản · hành động · người chịu trách nhiệm. */
public class Timeline extends JPanel {
    public Timeline() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public void set(List<NhatKyView> items) {
        removeAll();
        if (items.isEmpty()) {
            JLabel l = new JLabel("Chưa có lịch sử xử lý.");
            l.setForeground(Theme.MUTED);
            add(l);
        }
        for (NhatKyView n : items) {
            String html = "<html><div style='width:420px'><b>" + DataTable.esc(n.hanhDong()) + "</b>"
                    + (n.noiDung() == null ? "" : " — " + DataTable.esc(n.noiDung()))
                    + "<br><span style='color:#64748B'>" + Fmt.dateTime(n.thoiGian()) + " · " + DataTable.esc(n.tenDangNhap() == null ? "hệ thống" : n.tenDangNhap())
                    + (n.tenNguoiChiuTrachNhiem() == null ? "" : " · chịu trách nhiệm: " + DataTable.esc(n.tenNguoiChiuTrachNhiem())) + "</span></div></html>";
            JLabel l = new JLabel(html);
            l.setFont(Theme.SMALL);
            l.setForeground(Theme.INK2);
            l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.PRI_SOFT), BorderFactory.createEmptyBorder(4, 10, 6, 0)));
            add(l);
        }
        revalidate();
        repaint();
    }
}
