package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.LichHenService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.NhanVienCombo;
import vn.n08.nhakhoa.view.component.Timeline;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.DoiLichDialog;
import vn.n08.nhakhoa.view.dialog.LichHenDialog;
import vn.n08.nhakhoa.view.dialog.XemDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/** UC03 Quản lý lịch hẹn, UC06 Hủy (bắt buộc lý do), UC07 Đổi lịch (giữ lịch cũ), đánh dấu vắng, lịch sử. */
public class LichHenPage extends Page {
    private final LichHenService svc = new LichHenService();
    private final HintField tu = Fields.date(Now.today()), den = Fields.date(Now.today().plusDays(7));
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "Tất cả bác sĩ", false);
    private final JComboBox<TrangThaiLichHen> tt = Fields.combo(List.of(TrangThaiLichHen.values()), TrangThaiLichHen::label, "Tất cả trạng thái");
    private final HintField q = Fields.text("Tên, SĐT, mã BN / lịch hẹn");
    private final DataTable<LichHenView> table = new DataTable<LichHenView>()
            .col("Mã", 80, v -> v.lh().maLichHen()).col("Thời gian", 120, v -> v.lh().thoiGianHen()).col("Thứ", 60, v -> Fmt.weekdayShort(v.lh().thoiGianHen().toLocalDate()))
            .col("Bệnh nhân", 160, LichHenView::tenBenhNhan).col("SĐT", 100, LichHenView::soDienThoai).col("Bác sĩ", 140, LichHenView::tenBacSi)
            .col("Lý do", 170, v -> v.lh().lyDoHen()).col("Trạng thái", 110, v -> v.lh().trangThai())
            .col("Gốc / kế hoạch", 120, v -> (v.lh().maLichHenGoc() == null ? "" : "đổi từ " + v.lh().maLichHenGoc()) + (v.lh().maKeHoach() == null ? "" : " " + v.lh().maKeHoach()))
            .emptyText("Không có lịch hẹn trong khoảng đã chọn.");
    private final Btn xacNhan = Btn.success("Xác nhận", e -> act("Xác nhận lịch hẹn", m -> () -> { svc.xacNhan(m); return true; }));
    private final Btn doi = Btn.secondary("Đổi lịch", e -> doiLich());
    private final Btn vang = Btn.secondary("Đánh dấu vắng", e -> act("Đánh dấu vắng mặt", m -> () -> { svc.danhDauVang(m); return true; }));
    private final Btn huy = Btn.danger("Hủy lịch", e -> huyLich());
    private final Btn lichSu = Btn.ghost("Lịch sử", e -> lichSu());

    public LichHenPage() {
        super("Lịch hẹn", "UC03 Quản lý lịch hẹn · UC06 Hủy lịch hẹn · UC07 Thay đổi lịch hẹn");
        header.action(Btn.primary("Tạo lịch hẹn", e -> { if (new LichHenDialog(this, null, null, null, null).open() != null) reload(); }));
        Toolbar f = new Toolbar().label("Từ").with(tu).label("đến").with(den).with(bs, tt, q, Btn.secondary("Lọc", e -> reload()));
        q.addActionListener(e -> reload());
        Toolbar a = new Toolbar().with(xacNhan, doi, vang, huy, lichSu);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(f);
        top.add(javax.swing.Box.createVerticalStrut(8));
        top.add(a);
        body(Card.titled("Danh sách lịch hẹn", "Chọn một lịch để thao tác; đổi lịch tạo bản ghi mới và giữ bản cũ ở trạng thái \"Đã đổi lịch\"",
                Layouts.stack(top, table)));
        table.onSelect(this::buttons);
        table.onOpen(v -> lichSu());
        buttons(null);
    }

    private void buttons(LichHenView v) {
        boolean mo = v != null && v.lh().dangMo();
        xacNhan.setEnabled(v != null && v.lh().trangThai() == TrangThaiLichHen.CHO_XAC_NHAN);
        doi.setEnabled(mo);
        huy.setEnabled(mo);
        vang.setEnabled(mo && v.lh().thoiGianHen().isBefore(Now.now()));
        lichSu.setEnabled(v != null);
    }

    @Override public void reload() {
        LocalDate a, b;
        try {
            a = Fmt.parseDate(tu.getText(), "tu", "Từ ngày");
            b = Fmt.parseDate(den.getText(), "den", "Đến ngày");
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        String m = bs.ma();
        TrangThaiLichHen t = Fields.selected(tt);
        String s = q.getText();
        Async.load(table, () -> svc.danhSach(a, b, m, t, s), r -> buttons(table.selected()));
    }

    private void act(String title, java.util.function.Function<String, Supplier<Boolean>> f) {
        LichHenView v = table.selected();
        if (v == null) return;
        Async.run(this, f.apply(v.lh().maLichHen()), r -> { Toast.show(this, title + " — " + v.lh().maLichHen()); reload(); });
    }

    private void doiLich() {
        LichHenView v = table.selected();
        if (v != null && new DoiLichDialog(this, v).open() != null) reload();
    }

    private void huyLich() {
        LichHenView v = table.selected();
        if (v == null) return;
        String ly = Dialogs.reason(this, "Hủy lịch hẹn " + v.lh().maLichHen(), v.tenBenhNhan() + " · " + Fmt.dateTime(v.lh().thoiGianHen()) + "\nNhập lý do hủy (bắt buộc).",
                "Hủy lịch", true);
        if (ly == null) return;
        Async.run(this, () -> { svc.huy(v.lh().maLichHen(), ly); return true; }, r -> { Toast.show(this, "Đã hủy lịch hẹn."); reload(); });
    }

    private void lichSu() {
        LichHenView v = table.selected();
        if (v == null) return;
        Async.run(this, () -> new Object[]{svc.chuoiDoiLich(v.lh().maLichHen()), svc.nhatKy(v.lh().maLichHen())}, r -> {
            @SuppressWarnings("unchecked") List<LichHenView> chain = (List<LichHenView>) r[0];
            @SuppressWarnings("unchecked") List<vn.n08.nhakhoa.dto.NhatKyView> log = (List<vn.n08.nhakhoa.dto.NhatKyView>) r[1];
            DataTable<LichHenView> t = new DataTable<LichHenView>().col("Mã", 80, x -> x.lh().maLichHen()).col("Thời gian", 120, x -> x.lh().thoiGianHen())
                    .col("Bác sĩ", 140, LichHenView::tenBacSi).col("Trạng thái", 110, x -> x.lh().trangThai()).col("Lý do hủy", 160, x -> x.lh().lyDoHuy());
            t.setRows(chain);
            t.setPreferredSize(new java.awt.Dimension(560, 150));
            Timeline tl = new Timeline();
            tl.set(log);
            JPanel p = new JPanel(new java.awt.BorderLayout(0, 12));
            p.setOpaque(false);
            p.add(t, java.awt.BorderLayout.NORTH);
            p.add(tl, java.awt.BorderLayout.CENTER);
            XemDialog.show(this, "Lịch sử lịch hẹn " + v.lh().maLichHen(), new javax.swing.JScrollPane(p), 640, 520);
        });
    }
}
