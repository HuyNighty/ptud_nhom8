package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.CapCuuView;
import vn.n08.nhakhoa.dto.ChiTietKiemTraView;
import vn.n08.nhakhoa.dto.KiemTraCapCuuView;
import vn.n08.nhakhoa.service.CapCuuService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.BoSungCapCuuDialog;
import vn.n08.nhakhoa.view.dialog.CapCuuMucDialog;
import vn.n08.nhakhoa.view.dialog.KiemTraCapCuuDialog;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/**
 * UC34 Kiểm tra hộp thuốc cấp cứu. Danh mục cấu hình được; dữ liệu cài sẵn là DỮ LIỆU MẪU để chạy thử,
 * không phải danh mục chuyên môn chính thức — phòng khám cần tự rà soát.
 */
public class CapCuuPage extends Page {
    private final CapCuuService svc = new CapCuuService();
    private final DataTable<CapCuuView> dm = new DataTable<CapCuuView>()
            .col("Mã", 50, v -> v.muc().maMuc()).col("Mục", 200, v -> v.muc().tenMuc()).col("ĐVT", 55, v -> v.muc().donViTinh())
            .col("Tối thiểu", 65, v -> v.muc().soLuongToiThieu()).col("Hiện có", 60, v -> v.muc().soLuongHienCo()).col("HSD gần nhất", 95, v -> v.muc().hanSuDungGanNhat())
            .col("Tình trạng", 115, CapCuuView::tinhTrang).col("Liên kết kho", 150, CapCuuView::tenLienKet)
            .col("Nguồn", 90, v -> v.muc().laDuLieuMau() ? "Dữ liệu mẫu" : "Cấu hình").emptyText("Danh mục trống.")
            .rowTint(v -> v.tinhTrang() == CapCuuView.TinhTrang.THIEU || v.tinhTrang() == CapCuuView.TinhTrang.HET_HAN ? Theme.RED_S : null);
    private final DataTable<KiemTraCapCuuView> ls = new DataTable<KiemTraCapCuuView>()
            .col("Mã", 80, v -> v.kt().maKiemTra()).col("Thời gian", 110, v -> v.kt().thoiGian()).col("Người kiểm tra", 140, KiemTraCapCuuView::tenNguoiKiemTra)
            .col("Đạt", 60, v -> v.soDat() + "/" + v.soMuc()).col("Kết quả", 110, v -> v.kt().ketQua()).col("Ghi chú", 260, v -> v.kt().ghiChu()).emptyText("Chưa kiểm tra lần nào.");
    private final DataTable<ChiTietKiemTraView> ct = new DataTable<ChiTietKiemTraView>().col("Mục", 200, ChiTietKiemTraView::tenMuc)
            .col("Thực tế", 60, v -> v.ct().soLuongThucTe()).col("Tối thiểu", 65, ChiTietKiemTraView::soLuongToiThieu).col("HSD", 90, v -> v.ct().hanSuDung())
            .col("Đạt", 50, v -> v.ct().dat() ? "Đạt" : "Không").emptyText("Chọn một lần kiểm tra.");
    private final JLabel cauHinh = new JLabel(" ");
    private final Btn sua = Btn.secondary("Sửa mục", e -> sua());
    private final Btn apDung = Btn.secondary("Ngừng / áp dụng lại", e -> apDung());
    private final Btn boSung = Btn.secondary("Bổ sung từ kho", e -> boSung());
    private CapCuuService.CauHinh ch;

    public CapCuuPage() {
        super("Hộp cấp cứu", "UC34 Kiểm tra hộp thuốc cấp cứu — danh mục cấu hình được");
        header.action(Btn.secondary("Cấu hình ngưỡng", e -> cauHinh()));
        header.action(Btn.secondary("Thêm mục", e -> { if (new CapCuuMucDialog(this, null).open() != null) reload(); }));
        header.action(Btn.primary("Kiểm tra hộp", e -> kiemTra()));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(Layouts.note("<b>Lưu ý:</b> các mục cài sẵn là <b>dữ liệu mẫu để chạy thử</b>, không phải danh mục chuyên môn chính thức. "
                + "Phòng khám cần rà soát và cấu hình theo quy định áp dụng tại cơ sở.", true), BorderLayout.NORTH);
        cauHinh.setFont(Theme.SMALL);
        cauHinh.setForeground(Theme.MUTED);
        top.add(cauHinh, BorderLayout.CENTER);
        top.add(new Toolbar().with(sua, apDung, boSung), BorderLayout.SOUTH);
        Card danhMuc = Card.titled("Danh mục", null, Layouts.stack(top, dm));
        JPanel bot = new JPanel(new GridLayout(1, 2, 14, 0));
        bot.setOpaque(false);
        bot.add(Card.titled("Lịch sử kiểm tra", null, ls));
        bot.add(Card.titled("Chi tiết lần kiểm tra", null, ct));
        bot.setPreferredSize(new Dimension(10, 260));
        JPanel all = new JPanel(new BorderLayout(0, 14));
        all.setOpaque(false);
        all.add(danhMuc, BorderLayout.CENTER);
        all.add(bot, BorderLayout.SOUTH);
        body(all);
        dm.onSelect(v -> {
            sua.setEnabled(v != null);
            apDung.setEnabled(v != null);
            boSung.setEnabled(v != null && v.muc().dangApDung() && (v.muc().maThuoc() != null || v.muc().maVatTu() != null) && Session.can(Quyen.KHO));
        });
        dm.onOpen(v -> sua());
        ls.onSelect(v -> { if (v == null) ct.setRows(List.of()); else Async.load(ct, () -> svc.chiTietKiemTra(v.kt().maKiemTra())); });
        sua.setEnabled(false);
        apDung.setEnabled(false);
        boSung.setEnabled(false);
    }

    @Override public void reload() {
        Async.run(this, svc::cauHinh, c -> {
            ch = c;
            cauHinh.setText("Mục đạt khi số lượng ≥ tối thiểu và HSD còn hơn " + c.nguongHsdNgay() + " ngày · chu kỳ kiểm tra " + c.chuKyNgay() + " ngày (giả định, cấu hình được).");
        });
        Async.load(dm, svc::danhMuc);
        Async.load(ls, svc::lichSu);
    }

    private void kiemTra() {
        List<CapCuuView> active = dm.rows().stream().filter(v -> v.muc().dangApDung()).toList();
        if (active.isEmpty()) { Dialogs.error(this, "Danh mục chưa có mục đang áp dụng."); return; }
        String ma = new KiemTraCapCuuDialog(this, active, ch == null ? 30 : ch.nguongHsdNgay()).open();
        if (ma != null) reload();
    }

    private void sua() {
        CapCuuView v = dm.selected();
        if (v != null && new CapCuuMucDialog(this, v.muc()).open() != null) reload();
    }

    private void apDung() {
        CapCuuView v = dm.selected();
        boolean moi = !v.muc().dangApDung();
        if (!Dialogs.confirm(this, v.muc().tenMuc(), moi ? "Áp dụng lại mục này?" : "Ngừng áp dụng mục này? (không xóa, lịch sử kiểm tra giữ nguyên)", "Đồng ý", false)) return;
        Async.run(this, () -> { svc.datApDung(v.muc().maMuc(), moi); return true; }, r -> { Toast.show(this, "Đã cập nhật."); reload(); });
    }

    private void boSung() {
        CapCuuView v = dm.selected();
        if (v != null && new BoSungCapCuuDialog(this, v).open() != null) reload();
    }

    private void cauHinh() {
        String s = Dialogs.reason(this, "Cấu hình ngưỡng", "Nhập \"ngưỡng HSD (ngày), chu kỳ kiểm tra (ngày)\" — hiện tại: "
                + (ch == null ? "" : ch.nguongHsdNgay() + ", " + ch.chuKyNgay()), "Lưu", false);
        if (s == null) return;
        String[] p = s.split("[,;\\s]+");
        try {
            int a = Fmt.parseInt(p[0], "nguong", "Ngưỡng"), b = Fmt.parseInt(p.length > 1 ? p[1] : "", "chuKy", "Chu kỳ");
            Async.run(this, () -> { svc.luuCauHinh(a, b); return true; }, r -> { Toast.show(this, "Đã lưu cấu hình."); reload(); });
        } catch (RuntimeException e) {
            Dialogs.error(this, "Nhập hai số, ví dụ: 30, 7");
        }
    }
}
