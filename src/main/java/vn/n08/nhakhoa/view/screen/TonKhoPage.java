package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.dto.SoKsdbRow;
import vn.n08.nhakhoa.dto.TonKhoItem;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.Tone;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Tag;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.ThuocVatTuDialog;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;

/** UC27 Tìm kiếm, UC29 Quản lý thuốc/vật tư, UC36 Theo dõi tồn kho, UC37 Theo dõi hạn dùng, UC33 Sổ thuốc kiểm soát đặc biệt. */
public class TonKhoPage extends Page {
    private final KhoService svc = new KhoService();
    private final boolean kho = Session.can(Quyen.KHO);
    private final HintField q = Fields.text("Mã, tên, hoạt chất");
    private final JComboBox<String> loc = Fields.combo(List.of("Thuốc", "Vật tư", "Dưới định mức", "Kiểm soát đặc biệt", "Có lô hết hạn"), s -> s, "Tất cả");
    private final DataTable<TonKhoItem> table = new DataTable<TonKhoItem>()
            .col("Mã", 60, TonKhoItem::ma).col("Tên", 200, TonKhoItem::ten).col("Loại", 60, TonKhoItem::loai).col("ĐVT", 60, TonKhoItem::donViTinh)
            .col("Tồn khả dụng", 90, TonKhoItem::tonKhaDung).col("Định mức", 70, TonKhoItem::dinhMucToiThieu)
            .col("Tình trạng", 120, v -> v.duoiDinhMuc() ? (v.daDatHang() ? new Tag("Đã đặt hàng", Tone.BLUE) : new Tag("Dưới định mức", Tone.ORANGE)) : new Tag("Đủ", Tone.GREEN))
            .col("Trong lô hết hạn", 100, v -> v.tonHetHan() == 0 ? null : v.tonHetHan()).col("Số lô", 50, TonKhoItem::soLo)
            .col("HSD gần nhất", 95, TonKhoItem::hsdGanNhat).col("KSĐB", 50, v -> v.kiemSoatDacBiet() ? "Có" : "").emptyText("Không có mặt hàng.")
            .rowTint(v -> v.duoiDinhMuc() && !v.daDatHang() ? Theme.ORANGE_S : null);
    private final DataTable<LoHangView> lots = new DataTable<LoHangView>()
            .col("Mã lô", 80, v -> v.lo().maLo()).col("Số lô", 80, v -> v.lo().soLo()).col("NSX", 90, v -> v.lo().ngaySanXuat()).col("HSD", 90, v -> v.lo().hanSuDung())
            .col("Tồn", 60, v -> v.lo().soLuongTon()).col("Nhà cung cấp", 160, LoHangView::tenNhaCungCap)
            .col("Hạn", 110, v -> v.lo().daHetHan(Now.today()) ? new Tag("Hết hạn", Tone.RED) : v.lo().sapHetHan(Now.today(), 30) ? new Tag("Sắp hết hạn", Tone.AMBER) : new Tag("Còn hạn", Tone.GREEN))
            .emptyText("Mặt hàng chưa có lô.");
    private final DataTable<LoHangView> han = new DataTable<LoHangView>()
            .col("HSD", 90, v -> v.lo().hanSuDung()).col("Còn", 80, v -> java.time.temporal.ChronoUnit.DAYS.between(Now.today(), v.lo().hanSuDung()) + " ngày")
            .col("Mặt hàng", 200, LoHangView::tenHang).col("Mã lô", 80, v -> v.lo().maLo()).col("Số lô", 80, v -> v.lo().soLo()).col("Tồn", 60, v -> v.lo().soLuongTon())
            .col("Tình trạng", 110, v -> v.lo().daHetHan(Now.today()) ? new Tag("Hết hạn", Tone.RED) : new Tag("Sắp hết hạn", Tone.AMBER))
            .emptyText("Không có lô sắp hết hạn.").rowTint(v -> v.lo().daHetHan(Now.today()) ? Theme.RED_S : null);
    private final HintField tu = Fields.date(Now.today().minusDays(90)), den = Fields.date(Now.today());
    private final DataTable<SoKsdbRow> so = new DataTable<SoKsdbRow>()
            .col("Thời gian", 110, SoKsdbRow::thoiGian).col("Loại", 80, SoKsdbRow::loai).col("Chứng từ", 85, SoKsdbRow::chungTu).col("Thuốc", 160, SoKsdbRow::tenThuoc)
            .col("Số lô", 70, SoKsdbRow::soLo).col("SL", 50, SoKsdbRow::soLuong).col("Người thực hiện", 140, SoKsdbRow::nguoiThucHien).col("Ghi chú", 300, SoKsdbRow::ghiChu)
            .emptyText("Không có biến động thuốc kiểm soát đặc biệt trong khoảng này.");
    private final Btn sua = Btn.secondary("Sửa", e -> sua());
    private final Btn datHang = Btn.secondary("Đánh dấu / bỏ đã đặt hàng", e -> datHang());
    private List<TonKhoItem> all = List.of();


    public TonKhoPage() {
        super("Thuốc & vật tư", "UC27 Tìm kiếm · UC29 Quản lý · UC36 Tồn kho · UC37 Hạn sử dụng · UC33 Thuốc kiểm soát đặc biệt");
        if (kho) {
            header.action(Btn.secondary("Thêm vật tư", e -> { if (new ThuocVatTuDialog(this, null, null, false).open() != null) reload(); }));
            header.action(Btn.primary("Thêm thuốc", e -> { if (new ThuocVatTuDialog(this, null, null, true).open() != null) reload(); }));
        }
        q.addActionListener(e -> reload());
        loc.addActionListener(e -> filter());
        Toolbar t = new Toolbar().with(q, loc, Btn.secondary("Tìm", e -> reload()));
        if (kho) t.with(sua, datHang);
        lots.setPreferredSize(new Dimension(10, 170));
        JPanel tonTab = Layouts.stack(t, table);
        tonTab.add(Card.titled("Lô của mặt hàng đã chọn", "Tồn khả dụng chỉ tính lô chưa hết hạn", lots), BorderLayout.SOUTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tồn kho", Layouts.pad(Card.titled("Tồn kho tổng hợp theo lô", null, tonTab), 10, 0, 0, 0));
        tabs.addTab("Hạn sử dụng", Layouts.pad(Card.titled("Lô còn tồn sắp hết hạn / đã hết hạn", "Lô hết hạn không xuất được; lập phiếu hủy ở màn hình Kiểm kê & hao hụt", han), 10, 0, 0, 0));
        tabs.addTab("Sổ thuốc KSĐB", Layouts.pad(Card.titled("Sổ theo dõi thuốc kiểm soát đặc biệt", "Nhập, hoàn trả, xuất (kèm người nhận, BS chịu trách nhiệm), điều chỉnh đã duyệt, kê đơn",
                Layouts.stack(new Toolbar().label("Từ").with(tu).label("đến").with(den).with(Btn.secondary("Xem", e -> loadSo())), so)), 10, 0, 0, 0));
        body(tabs);
        table.autoSelect();
        table.onSelect(v -> {
            sua.setEnabled(v != null);
            datHang.setEnabled(v != null && v.duoiDinhMuc());
            if (v == null) lots.setRows(List.of());
            else Async.load(lots, () -> svc.loCua(v.laThuoc(), v.ma()));
        });
        table.onOpen(v -> { if (kho) sua(); });
        sua.setEnabled(false);
        datHang.setEnabled(false);
    }

    @Override public void reload() {
        String s = q.getText();
        Async.run(this, () -> svc.tonKho(s), r -> { all = r; filter(); }, e -> table.error(e.getMessage()));
        Async.load(han, svc::canhBaoHanDung);
        loadSo();
    }

    private void filter() {
        String f = Fields.selected(loc);
        table.setRows(all.stream().filter(v -> f == null || switch (f) {
            case "Thuốc" -> v.laThuoc();
            case "Vật tư" -> !v.laThuoc();
            case "Dưới định mức" -> v.duoiDinhMuc();
            case "Kiểm soát đặc biệt" -> v.kiemSoatDacBiet();
            default -> v.tonHetHan() > 0;
        }).toList());
    }

    private void loadSo() {
        LocalDate a, b;
        try {
            a = Fmt.parseDate(tu.getText(), "tu", "Từ ngày");
            b = Fmt.parseDate(den.getText(), "den", "Đến ngày");
        } catch (RuntimeException e) {
            return;
        }
        Async.load(so, () -> svc.soKiemSoatDacBiet(a, b));
    }

    private void sua() {
        TonKhoItem v = table.selected();
        if (v == null) return;
        Async.run(this, () -> v.laThuoc() ? svc.danhSachThuoc().stream().filter(x -> x.maThuoc().equals(v.ma())).findFirst().orElse(null)
                : svc.danhSachVatTu().stream().filter(x -> x.maVatTu().equals(v.ma())).findFirst().orElse(null), o -> {
            if (new ThuocVatTuDialog(this, o instanceof Thuoc t ? t : null, o instanceof VatTu x ? x : null, v.laThuoc()).open() != null) reload();
        });
    }

    private void datHang() {
        TonKhoItem v = table.selected();
        boolean moi = !v.daDatHang();
        if (!Dialogs.confirm(this, v.ten(), moi ? "Đánh dấu đã đặt hàng bổ sung? (tự bỏ khi nhập kho)" : "Bỏ đánh dấu đã đặt hàng?", "Đồng ý", false)) return;
        Async.run(this, () -> { svc.danhDauDaDatHang(v.laThuoc(), v.ma(), moi); return true; }, r -> { Toast.show(this, "Đã cập nhật."); reload(); });
    }
}
