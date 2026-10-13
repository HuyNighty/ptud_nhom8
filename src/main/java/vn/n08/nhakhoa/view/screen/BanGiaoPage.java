package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.BanGiaoView;
import vn.n08.nhakhoa.model.TrangThaiBanGiao;
import vn.n08.nhakhoa.service.BanGiaoService;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Timeline;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.BanGiaoDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.time.LocalDate;
import java.util.List;

/** UC19 Giao nhận ca: lập, xác nhận (khác người lập), hủy có lý do; lịch sử xử lý. */
public class BanGiaoPage extends Page {
    private final BanGiaoService svc = new BanGiaoService();
    private final HintField tu = Fields.date(Now.today().minusDays(7)), den = Fields.date(Now.today());
    private final JComboBox<TrangThaiBanGiao> tt = Fields.combo(List.of(TrangThaiBanGiao.values()), TrangThaiBanGiao::label, "Tất cả");
    private final DataTable<BanGiaoView> table = new DataTable<BanGiaoView>()
            .col("Mã", 80, v -> v.bg().maBanGiao()).col("Ngày", 85, v -> v.bg().ngay()).col("Giao", 150, v -> v.tenNguoiGiao() + " · " + v.tenCaGiao())
            .col("Nhận", 150, v -> v.tenNguoiNhan() + " · " + v.tenCaNhan()).col("Chênh lệch", 75, v -> v.bg().coChenhLech() ? "Có" : "")
            .col("Người lập", 90, BanGiaoView::tkTao).col("Trạng thái", 110, v -> v.bg().trangThai()).emptyText("Không có biên bản.");
    private final DetailGrid info = new DetailGrid(1);
    private final JPanel note = new JPanel(new java.awt.BorderLayout());
    private final Timeline tl = new Timeline();
    private final Btn xacNhan = Btn.success("Xác nhận nhận ca", e -> xacNhan());
    private final Btn huy = Btn.danger("Hủy biên bản", e -> huy());

    public BanGiaoPage() {
        super("Bàn giao ca", "UC19 Giao nhận ca — tài khoản lập biên bản không được tự xác nhận");
        header.action(Btn.primary("Lập biên bản", e -> { if (new BanGiaoDialog(this).open() != null) reload(); }));
        Card list = Card.titled("Biên bản", null, Layouts.stack(new Toolbar().label("Từ").with(tu).label("đến").with(den).with(tt, Btn.secondary("Lọc", e -> reload())), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        note.setOpaque(false);
        JPanel b = Layouts.stack(info, note);
        b.add(new Toolbar().with(xacNhan, huy), java.awt.BorderLayout.SOUTH);
        d.add(Card.titled("Chi tiết", null, b));
        d.add(javax.swing.Box.createVerticalStrut(10));
        d.add(Card.titled("Lịch sử xử lý", null, tl));
        body(Layouts.masterDetail(list, d, 460));
        table.autoSelect();
        table.onSelect(this::show);
        show(null);
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
        TrangThaiBanGiao t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSach(a, b, t), r -> show(table.selected()));
    }

    private void show(BanGiaoView v) {
        info.clear();
        note.removeAll();
        Session.User u = Session.user();
        boolean cho = v != null && v.bg().trangThai() == TrangThaiBanGiao.CHO_BAN_GIAO;
        boolean tuLap = v != null && v.bg().maTaiKhoanTao().equals(u.maTaiKhoan());
        if (v == null) {
            info.row("Biên bản", "Chọn một biên bản.");
            tl.set(List.of());
        } else {
            info.row("Mã", v.bg().maBanGiao()).row("Ngày", v.bg().ngay()).row("Người giao", v.tenNguoiGiao() + " · " + v.tenCaGiao())
                    .row("Người nhận", v.tenNguoiNhan() + " · " + v.tenCaNhan()).row("Nội dung", v.bg().noiDung())
                    .row("Chênh lệch", v.bg().coChenhLech() ? "Có — cần ghi chú khi xác nhận" : "Không").row("Lập bởi", v.tkTao() + " · " + Fmt.dateTime(v.bg().thoiGian()))
                    .row("Trạng thái", v.bg().trangThai());
            if (v.tkXacNhan() != null) info.row("Xác nhận bởi", v.tkXacNhan() + " · " + Fmt.dateTime(v.bg().thoiGianXacNhan())).row("Ghi chú xác nhận", v.bg().ghiChuXacNhan());
            if (v.tkHuy() != null) info.row("Hủy bởi", v.tkHuy() + " · " + Fmt.dateTime(v.bg().thoiGianHuy())).row("Lý do hủy", v.bg().lyDoHuy());
            if (cho && tuLap) note.add(Layouts.note("Biên bản do chính tài khoản <b>" + u.tenDangNhap() + "</b> lập nên không được tự xác nhận.", true));
            Async.run(this, () -> new NhatKyService().lichSu(v.bg().maBanGiao()), tl::set);
        }
        boolean coQuyenXn = v != null && (u.maNhanVien().equals(v.bg().maNguoiNhan()) || Session.can(Quyen.PHAN_CONG));
        xacNhan.setEnabled(cho && !tuLap && coQuyenXn);
        huy.setEnabled(cho && (tuLap || Session.can(Quyen.PHAN_CONG)));
        info.revalidate();
        note.revalidate();
    }

    private void xacNhan() {
        BanGiaoView v = table.selected();
        String gc;
        if (v.bg().coChenhLech()) gc = Dialogs.reason(this, "Xác nhận " + v.bg().maBanGiao(), "Biên bản có chênh lệch. Ghi chú đã kiểm tra / cách xử lý (bắt buộc).", "Xác nhận", false);
        else gc = Dialogs.confirm(this, "Xác nhận " + v.bg().maBanGiao(), "Xác nhận đã nhận ca theo nội dung biên bản?", "Xác nhận", false) ? "" : null;
        if (gc == null) return;
        Async.run(this, () -> { svc.xacNhan(v.bg().maBanGiao(), gc); return true; }, r -> { Toast.show(this, "Đã xác nhận bàn giao."); reload(); });
    }

    private void huy() {
        BanGiaoView v = table.selected();
        String ly = Dialogs.reason(this, "Hủy biên bản " + v.bg().maBanGiao(), "Nhập lý do hủy.", "Hủy biên bản", true);
        if (ly != null) Async.run(this, () -> { svc.huy(v.bg().maBanGiao(), ly); return true; }, r -> { Toast.show(this, "Đã hủy biên bản."); reload(); });
    }
}
