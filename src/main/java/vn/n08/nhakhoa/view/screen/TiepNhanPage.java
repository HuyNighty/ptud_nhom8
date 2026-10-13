package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.BenhNhanService;
import vn.n08.nhakhoa.service.TiepNhanService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.BenhNhanDialog;
import vn.n08.nhakhoa.view.dialog.ChonNhanVienDialog;
import vn.n08.nhakhoa.view.dialog.TiepNhanDialog;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridLayout;

/** UC01 Tìm kiếm bệnh nhân + UC02 Tiếp nhận (trực tiếp hoặc check-in lịch hẹn hôm nay) + hàng chờ. */
public class TiepNhanPage extends Page {
    private final TiepNhanService svc = new TiepNhanService();
    private final HintField q = Fields.text("SĐT (10 số), mã hoặc họ tên");
    private final DataTable<BenhNhanView> kq = new DataTable<BenhNhanView>()
            .col("Mã BN", 80, v -> v.bn().maBenhNhan()).col("Họ tên", 170, v -> v.bn().hoTen()).col("Ngày sinh", 90, v -> v.bn().ngaySinh())
            .col("SĐT", 100, v -> v.bn().soDienThoai()).col("Lần khám cuối", 120, BenhNhanView::lanKhamCuoi).col("Còn nợ", 100, BenhNhanView::congNo)
            .emptyText("Nhập SĐT, mã hoặc họ tên rồi bấm Tìm. Không thấy → Tạo hồ sơ mới.");
    private final DataTable<LichHenView> lh = new DataTable<LichHenView>()
            .col("Giờ", 55, v -> v.lh().thoiGianHen().toLocalTime()).col("Bệnh nhân", 150, LichHenView::tenBenhNhan).col("Bác sĩ", 130, LichHenView::tenBacSi)
            .col("Lý do", 140, v -> v.lh().lyDoHen()).col("Trạng thái", 110, v -> v.lh().trangThai()).emptyText("Không còn lịch hẹn chờ check-in hôm nay.");
    private final DataTable<PhieuKhamView> pk = new DataTable<PhieuKhamView>()
            .col("Phiếu", 80, v -> v.pk().maPhieuKham()).col("Tiếp nhận", 60, v -> v.pk().thoiGianTiepNhan().toLocalTime())
            .col("Bệnh nhân", 150, PhieuKhamView::tenBenhNhan).col("Bác sĩ", 130, PhieuKhamView::tenBacSi).col("Nguồn", 70, v -> v.pk().maLichHen() == null ? "Trực tiếp" : "Hẹn")
            .col("Trạng thái", 110, v -> v.pk().trangThai()).emptyText("Chưa có bệnh nhân nào được tiếp nhận hôm nay.");
    private final Btn tiepNhan = Btn.primary("Tiếp nhận trực tiếp", e -> tiepNhan());
    private final Btn checkIn = Btn.primary("Check-in → tạo phiếu khám", e -> checkIn());
    private final Btn doiBs = Btn.secondary("Đổi bác sĩ", e -> doiBs());
    private final Btn huy = Btn.danger("Hủy phiếu", e -> huy());

    public TiepNhanPage() {
        super("Tiếp nhận bệnh nhân", "UC01 Tìm kiếm · UC02 Tiếp nhận — bệnh nhân đến trực tiếp hoặc theo lịch hẹn hôm nay");
        header.action(Btn.secondary("Tạo hồ sơ mới", e -> {
            String ma = new BenhNhanDialog(this, null).open();
            if (ma != null) { q.setText(ma); tim(); }
        }));
        q.addActionListener(e -> tim());
        q.setPreferredSize(new Dimension(300, 32));
        kq.setPreferredSize(new Dimension(10, 200));
        Card tim = Card.titled("Tìm bệnh nhân", null, Layouts.stack(new Toolbar().with(q, Btn.secondary("Tìm", e -> tim()), tiepNhan), kq));
        Card cho = Card.titled("Lịch hẹn hôm nay chờ check-in", "Check-in chuyển lịch hẹn sang \"Đã đến\" và tạo phiếu khám cho bác sĩ của lịch", lh, checkIn);
        Card hang = Card.titled("Hàng chờ hôm nay", "Phiếu chờ khám có thể đổi bác sĩ hoặc hủy (có lý do)", pk, doiBs, huy);
        JPanel low = new JPanel(new GridLayout(1, 2, 14, 0));
        low.setOpaque(false);
        low.add(cho);
        low.add(hang);
        low.setPreferredSize(new Dimension(10, 330));
        JPanel all = new JPanel(new java.awt.BorderLayout(0, 14));
        all.setOpaque(false);
        all.add(tim, java.awt.BorderLayout.CENTER);
        all.add(low, java.awt.BorderLayout.SOUTH);
        body(all);
        kq.onSelect(v -> tiepNhan.setEnabled(v != null));
        lh.onSelect(v -> checkIn.setEnabled(v != null));
        pk.onSelect(v -> { boolean ok = v != null && v.pk().trangThai() == TrangThaiPhieuKham.CHO_KHAM; doiBs.setEnabled(ok); huy.setEnabled(ok); });
        lh.onOpen(v -> checkIn());
        kq.onOpen(v -> tiepNhan());
        tiepNhan.setEnabled(false);
        checkIn.setEnabled(false);
        doiBs.setEnabled(false);
        huy.setEnabled(false);
        kq.setRows(java.util.List.of());
    }

    private void tim() {
        String s = q.getText();
        Async.load(kq, () -> new BenhNhanService().tim(s));
    }

    @Override public void reload() {
        Async.run(this, () -> svc.trongNgay(Now.today()), d -> {
            lh.setRows(d.lichHenCho());
            pk.setRows(d.phieuKham());
        }, e -> { lh.error(e.getMessage()); pk.error(e.getMessage()); });
    }

    private void tiepNhan() {
        BenhNhanView v = kq.selected();
        if (v == null) return;
        if (new TiepNhanDialog(this, v).open() != null) reload();
    }

    private void checkIn() {
        LichHenView v = lh.selected();
        if (v == null) return;
        if (!Dialogs.confirm(this, "Check-in", v.tenBenhNhan() + " — hẹn " + Fmt.time(v.lh().thoiGianHen()) + " với " + v.tenBacSi()
                + ".\nTạo phiếu khám với triệu chứng ban đầu là lý do hẹn?", "Check-in", false)) return;
        Async.run(this, () -> svc.checkIn(v.lh().maLichHen(), null), ma -> { Toast.show(this, "Đã check-in, phiếu khám " + ma + "."); reload(); });
    }

    private void doiBs() {
        PhieuKhamView v = pk.selected();
        if (v == null) return;
        String r = new ChonNhanVienDialog(this, "Đổi bác sĩ — " + v.pk().maPhieuKham(), v.tenBenhNhan() + " đang chờ khám với " + v.tenBacSi() + ".",
                VaiTro.BS, false, "Đổi bác sĩ", m -> { svc.doiBacSi(v.pk().maPhieuKham(), m); return m; }, "Đã đổi bác sĩ.").preset(v.pk().maNhanVien()).open();
        if (r != null) reload();
    }

    private void huy() {
        PhieuKhamView v = pk.selected();
        if (v == null) return;
        String ly = Dialogs.reason(this, "Hủy phiếu " + v.pk().maPhieuKham(), "Lý do hủy (bắt buộc). Lịch hẹn gắn kèm (nếu có) cũng chuyển sang Đã hủy.", "Hủy phiếu", true);
        if (ly == null) return;
        Async.run(this, () -> { svc.huyPhieu(v.pk().maPhieuKham(), ly); return true; }, r -> { Toast.show(this, "Đã hủy phiếu."); reload(); });
    }
}
