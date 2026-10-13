package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.controller.ErrorHandler;
import vn.n08.nhakhoa.dto.DashboardData;
import vn.n08.nhakhoa.model.KetQuaKiemTra;
import vn.n08.nhakhoa.model.Tone;
import vn.n08.nhakhoa.service.DashboardService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.StatCard;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tổng quan trong ngày (Figma QL-01): số liệu theo quyền + danh sách việc cần xử lý dẫn tới màn hình tương ứng. */
public class DashboardPage extends Page {
    private final JPanel stats = new JPanel();
    private final JPanel todo = new JPanel();
    private final JPanel side = new JPanel();
    private final JLabel status = new JLabel(" ");

    public DashboardPage() {
        super("Tổng quan", Fmt.longDate(Now.today()) + " · số liệu tính tại thời điểm tải");
        header.action(Btn.secondary("Tải lại", e -> reload()));
        stats.setOpaque(false);
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
        todo.setOpaque(false);
        todo.setLayout(new BoxLayout(todo, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        JPanel bottom = new JPanel(new java.awt.GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.add(Card.titled("Việc cần xử lý", "Bấm để mở màn hình tương ứng", todo));
        bottom.add(Card.titled("Trong ngày", null, side));
        JPanel all = new JPanel(new BorderLayout(0, 14));
        all.setOpaque(false);
        all.add(stats, BorderLayout.NORTH);
        all.add(bottom, BorderLayout.CENTER);
        status.setForeground(Theme.RED);
        all.add(status, BorderLayout.SOUTH);
        body(all);
    }

    @Override public void reload() {
        Async.run(this, () -> new DashboardService().tai(), this::show, e -> status.setText(ErrorHandler.message(e)));
    }

    private void show(DashboardData d) {
        status.setText(" ");
        stats.removeAll();
        todo.removeAll();
        side.removeAll();
        List<JComponent> cards = new ArrayList<>();
        if (d.lichHenHomNay() != null) {
            long tong = d.lichHenHomNay().values().stream().mapToLong(Long::longValue).sum();
            cards.add(stat("Lịch hẹn hôm nay", Tone.BLUE, String.valueOf(tong), "Chờ xác nhận " + d.lichHenHomNay().get("Chờ xác nhận")
                    + " · đã xác nhận " + d.lichHenHomNay().get("Đã xác nhận")));
            cards.add(stat("Bệnh nhân chờ khám", Tone.AMBER, String.valueOf(d.choKham()), "Đang khám " + d.dangKham() + " · đã kết luận " + d.daKetLuan()));
        }
        if (d.doanhThuHomNay() != null) {
            BigDecimal tong = d.doanhThuHomNay().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            cards.add(stat("Đã thu hôm nay", Tone.GREEN, Fmt.money(tong), "Chỉ tính lần thu thành công"));
            cards.add(stat("Công nợ", Tone.RED, Fmt.money(d.tongCongNo()), d.soHoaDonConNo() + " hóa đơn còn nợ"));
        }
        if (d.duoiDinhMuc() != null) {
            cards.add(stat("Dưới định mức", Tone.ORANGE, String.valueOf(d.duoiDinhMuc()), "mặt hàng cần bổ sung"));
            cards.add(stat("Lô sắp / đã hết hạn", Tone.RED, d.sapHetHan() + " / " + d.daHetHanConTon(), "còn tồn trong kho"));
        }
        if (d.taiKhoanBiKhoa() != null) cards.add(stat("Tài khoản bị khóa", Tone.GRAY, String.valueOf(d.taiKhoanBiKhoa()), "BI_KHOA, chờ mở"));
        for (int i = 0; i < cards.size(); i += 4) {
            List<JComponent> rowCards = new ArrayList<>(cards.subList(i, Math.min(i + 4, cards.size())));
            while (rowCards.size() < 4) { JPanel f = new JPanel(); f.setOpaque(false); rowCards.add(f); }
            stats.add(Layouts.row(14, rowCards.toArray(new JComponent[0])));
            stats.add(javax.swing.Box.createVerticalStrut(14));
        }
        if (d.yeuCauHuyCho() != null && d.yeuCauHuyCho() > 0) todo("duyet-huy", d.yeuCauHuyCho() + " yêu cầu hủy hóa đơn chờ duyệt", Tone.AMBER);
        if (d.dieuChinhCho() != null && d.dieuChinhCho() > 0) todo("kiem-ke", d.dieuChinhCho() + " phiếu kiểm kê / hao hụt chờ duyệt", Tone.AMBER);
        if (d.banGiaoCho() != null && d.banGiaoCho() > 0) todo("ban-giao", d.banGiaoCho() + " biên bản bàn giao ca chờ xác nhận", Tone.AMBER);
        if (d.taiKhamDenHan() != null && d.taiKhamDenHan() > 0) todo("tai-kham", d.taiKhamDenHan() + " hạng mục điều trị đến hạn / quá hạn tái khám", Tone.BLUE);
        if (d.choKham() != null && d.choKham() > 0) todo("phieu-kham", d.choKham() + " bệnh nhân đang chờ khám", Tone.BLUE);
        if (d.duoiDinhMuc() != null && d.duoiDinhMuc() > 0) todo("ton-kho", d.duoiDinhMuc() + " mặt hàng dưới định mức tồn", Tone.ORANGE);
        if (d.daHetHanConTon() != null && d.daHetHanConTon() > 0) todo("kiem-ke", d.daHetHanConTon() + " lô đã hết hạn còn tồn — lập phiếu hủy", Tone.RED);
        if (d.capCuuQuaHan() != null && d.capCuuQuaHan()) todo("cap-cuu", "Hộp cấp cứu đã quá chu kỳ kiểm tra", Tone.RED);
        if (d.capCuuKetQua() == KetQuaKiemTra.CAN_BO_SUNG) todo("cap-cuu", "Lần kiểm tra hộp cấp cứu gần nhất: cần bổ sung", Tone.RED);
        if (d.taiKhoanBiKhoa() != null && d.taiKhoanBiKhoa() > 0) todo("tai-khoan", d.taiKhoanBiKhoa() + " tài khoản đang bị khóa", Tone.GRAY);
        if (todo.getComponentCount() == 0) todo.add(muted("Không có việc tồn đọng."));
        if (d.doanhThuHomNay() != null) {
            side.add(h("Thu theo phương thức"));
            for (Map.Entry<String, BigDecimal> e : d.doanhThuHomNay().entrySet()) side.add(muted(e.getKey() + ": " + Fmt.money(e.getValue())));
        }
        if (d.lichHenHomNay() != null) {
            side.add(h("Lịch hẹn theo trạng thái"));
            d.lichHenHomNay().forEach((k, v) -> { if (v > 0) side.add(muted(k + ": " + v)); });
        }
        if (d.caThieuNguoi() != null) {
            side.add(h("Ca hôm nay so với định mức"));
            if (d.caThieuNguoi().isEmpty()) side.add(muted("Các ca đủ người."));
            d.caThieuNguoi().forEach(s -> side.add(link("phan-cong", s, Tone.RED)));
        }
        if (d.capCuuQuaHan() != null) {
            side.add(h("Hộp cấp cứu"));
            side.add(muted(d.capCuuLanCuoi() == null ? "Chưa kiểm tra lần nào" : "Kiểm tra gần nhất " + Fmt.dateTime(d.capCuuLanCuoi()) + " · " + d.capCuuKetQua().label()));
        }
        stats.revalidate();
        revalidate();
        repaint();
    }

    private StatCard stat(String label, Tone t, String v, String hint) {
        StatCard c = new StatCard(label, t);
        c.set(v, hint);
        return c;
    }

    private void todo(String page, String text, Tone t) { todo.add(link(page, text, t)); }

    private JComponent link(String page, String text, Tone t) {
        JLabel l = new JLabel("<html>● <span>" + text + "</span> <span style='color:#2563EB'>›</span></html>");
        l.setForeground(Theme.fg(t));
        l.setFont(Theme.BODY);
        l.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        if (MainFrame.current() != null && MainFrame.current().has(page)) {
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { MainFrame.current().go(page); } });
        }
        return l;
    }

    private static JLabel h(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.INK2);
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        return l;
    }

    private static JLabel muted(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.BODY);
        l.setForeground(Theme.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }
}
