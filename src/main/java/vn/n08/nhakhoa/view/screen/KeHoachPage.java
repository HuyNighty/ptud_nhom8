package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.ChiTietDieuTriView;
import vn.n08.nhakhoa.dto.KeHoachChiTiet;
import vn.n08.nhakhoa.dto.KeHoachView;
import vn.n08.nhakhoa.model.TrangThaiDieuTri;
import vn.n08.nhakhoa.model.TrangThaiKeHoach;
import vn.n08.nhakhoa.service.KeHoachService;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.util.Fmt;
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
import vn.n08.nhakhoa.view.dialog.HangMucDialog;
import vn.n08.nhakhoa.view.dialog.KeHoachDialog;
import vn.n08.nhakhoa.view.dialog.KetQuaDieuTriDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.List;

/** UC12 Lập kế hoạch, UC13 Cập nhật quá trình điều trị, UC15 Ghi nhận kết quả điều trị. */
public class KeHoachPage extends Page {
    private final KeHoachService svc = new KeHoachService();
    private final JComboBox<TrangThaiKeHoach> tt = Fields.combo(List.of(TrangThaiKeHoach.values()), TrangThaiKeHoach::label, "Tất cả trạng thái");
    private final HintField q = Fields.text("Tên / mã BN, mã kế hoạch, mục tiêu");
    private final DataTable<KeHoachView> table = new DataTable<KeHoachView>()
            .col("Mã", 80, v -> v.kh().maKeHoach()).col("Ngày lập", 90, v -> v.kh().ngayLap()).col("Bệnh nhân", 150, KeHoachView::tenBenhNhan)
            .col("Mục tiêu", 200, v -> v.kh().mucTieu()).col("Tiến độ", 60, v -> v.soDaThucHien() + "/" + v.soHangMuc())
            .col("Chi phí dự kiến", 110, KeHoachView::chiPhiDuKien).col("Trạng thái", 110, v -> v.kh().trangThai()).emptyText("Không có kế hoạch.");
    private final DetailGrid info = new DetailGrid(1);
    private final DataTable<ChiTietDieuTriView> items = new DataTable<ChiTietDieuTriView>()
            .col("#", 40, v -> v.ct().maChiTiet()).col("Dịch vụ", 150, ChiTietDieuTriView::tenDichVu).col("Răng", 50, v -> v.ct().viTriRang())
            .col("Dự kiến", 85, v -> v.ct().ngayDuKien()).col("Thực hiện", 85, v -> v.ct().ngayThucHien()).col("TT", 110, v -> v.ct().trangThai())
            .col("Kết quả / ghi chú", 170, v -> v.ct().ketQua() != null ? v.ct().ketQua() + " (" + v.tenBacSiThucHien() + ")" : v.ct().ghiChu())
            .emptyText("Chưa có hạng mục.");
    private final Timeline tl = new Timeline();
    private final Btn them = Btn.secondary("Thêm hạng mục", e -> them());
    private final Btn doiNgay = Btn.secondary("Đổi ngày", e -> doiNgay());
    private final Btn ghiKq = Btn.primary("Ghi kết quả", e -> ghiKq());
    private final Btn huyHm = Btn.danger("Hủy hạng mục", e -> huyHm());
    private final Btn hoanThanh = Btn.success("Hoàn thành kế hoạch", e -> hoanThanh());
    private final Btn huyKh = Btn.danger("Hủy kế hoạch", e -> huyKh());
    private KeHoachChiTiet cur;

    public KeHoachPage() {
        super("Kế hoạch điều trị", "UC12 Lập kế hoạch · UC13 Cập nhật quá trình điều trị · UC15 Ghi nhận kết quả điều trị");
        header.action(Btn.primary("Lập kế hoạch", e -> { if (new KeHoachDialog(this, null, null, null).open() != null) reload(); }));
        q.addActionListener(e -> reload());
        Card list = Card.titled("Kế hoạch", null, Layouts.stack(new Toolbar().with(tt, q, Btn.secondary("Lọc", e -> reload())), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        items.setPreferredSize(new Dimension(10, 220));
        JPanel ib = Layouts.stack(info, items);
        ib.add(new Toolbar().with(them, doiNgay, ghiKq, huyHm), java.awt.BorderLayout.SOUTH);
        d.add(Card.titled("Chi tiết kế hoạch", null, ib));
        d.add(javax.swing.Box.createVerticalStrut(8));
        d.add(new Toolbar().with(hoanThanh, huyKh));
        d.add(javax.swing.Box.createVerticalStrut(8));
        d.add(Card.titled("Lịch sử xử lý", null, tl));
        body(Layouts.masterDetail(list, d, 620));
        table.autoSelect();
        table.onSelect(v -> load());
        items.onSelect(v -> buttons());
        items.onOpen(v -> ghiKq());
        show(null);
    }

    @Override public void reload() {
        TrangThaiKeHoach t = Fields.selected(tt);
        String s = q.getText();
        Async.load(table, () -> svc.danhSach(t, s), r -> load());
    }

    private void load() {
        KeHoachView v = table.selected();
        if (v == null) { show(null); return; }
        String m = v.kh().maKeHoach();
        Async.run(this, () -> svc.chiTiet(m), this::show);
        Async.run(this, () -> new NhatKyService().lichSu(m), tl::set);
    }

    private void show(KeHoachChiTiet c) {
        cur = c;
        info.clear();
        if (c == null) {
            info.row("Kế hoạch", "Chọn một kế hoạch.");
            items.setRows(List.of());
            tl.set(List.of());
        } else {
            KeHoachView k = c.keHoach();
            info.row("Mã / ngày lập", k.kh().maKeHoach() + " · " + Fmt.date(k.kh().ngayLap()) + (k.kh().maPhieuKham() == null ? "" : " · từ lượt khám " + k.kh().maPhieuKham()))
                    .row("Bệnh nhân", k.tenBenhNhan() + " (" + k.maBenhNhan() + ")").row("Bác sĩ lập", k.tenBacSi()).row("Mục tiêu", k.kh().mucTieu())
                    .row("Trạng thái", k.kh().trangThai()).row("Chi phí dự kiến", k.chiPhiDuKien()).row("Tiến độ", k.soDaThucHien() + "/" + k.soHangMuc() + " hạng mục");
            if (k.kh().lyDoHuy() != null) info.row("Lý do hủy", k.kh().lyDoHuy());
            items.setRows(c.hangMuc());
        }
        info.revalidate();
        buttons();
    }

    private void buttons() {
        boolean dang = cur != null && cur.keHoach().kh().trangThai() == TrangThaiKeHoach.DANG_THUC_HIEN;
        ChiTietDieuTriView i = items.selected();
        boolean chua = dang && i != null && i.ct().trangThai() == TrangThaiDieuTri.CHUA_THUC_HIEN;
        them.setEnabled(dang);
        hoanThanh.setEnabled(dang);
        huyKh.setEnabled(dang);
        doiNgay.setEnabled(chua);
        ghiKq.setEnabled(chua);
        huyHm.setEnabled(chua);
    }

    private void after(String msg) {
        Toast.show(this, msg);
        reload();
    }

    private void them() {
        if (new HangMucDialog(this, cur.keHoach().kh().maKeHoach()).open() != null) after("Đã thêm hạng mục.");
    }

    private void doiNgay() {
        ChiTietDieuTriView i = items.selected();
        String s = Dialogs.reason(this, "Đổi ngày dự kiến", i.tenDichVu() + " răng " + i.ct().viTriRang() + " — hiện " + Fmt.date(i.ct().ngayDuKien())
                + "\nNhập ngày mới (dd/mm/yyyy).", "Đổi ngày", false);
        if (s == null) return;
        try {
            var d = Fmt.parseDate(s, "ngay", "Ngày");
            Async.run(this, () -> { svc.doiNgayDuKien(i.ct().maChiTiet(), d); return true; }, r -> after("Đã đổi ngày dự kiến."));
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    private void ghiKq() {
        ChiTietDieuTriView i = items.selected();
        if (i != null && ghiKq.isEnabled() && new KetQuaDieuTriDialog(this, i).open() != null) reload();
    }

    private void huyHm() {
        ChiTietDieuTriView i = items.selected();
        String ly = Dialogs.reason(this, "Hủy hạng mục #" + i.ct().maChiTiet(), "Lý do hủy (bắt buộc).", "Hủy hạng mục", true);
        if (ly != null) Async.run(this, () -> { svc.huyHangMuc(i.ct().maChiTiet(), ly); return true; }, r -> after("Đã hủy hạng mục."));
    }

    private void hoanThanh() {
        String m = cur.keHoach().kh().maKeHoach();
        if (Dialogs.confirm(this, "Hoàn thành kế hoạch " + m, "Chỉ hoàn thành khi mọi hạng mục đã thực hiện hoặc đã hủy.", "Hoàn thành", false)) {
            Async.run(this, () -> { svc.hoanThanh(m); return true; }, r -> after("Đã hoàn thành kế hoạch."));
        }
    }

    private void huyKh() {
        String m = cur.keHoach().kh().maKeHoach();
        String ly = Dialogs.reason(this, "Hủy kế hoạch " + m, "Các hạng mục chưa thực hiện sẽ chuyển Đã hủy; hạng mục đã làm giữ nguyên. Nhập lý do.", "Hủy kế hoạch", true);
        if (ly != null) Async.run(this, () -> { svc.huyKeHoach(m, ly); return true; }, r -> after("Đã hủy kế hoạch."));
    }
}
