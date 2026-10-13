package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.PhieuKhoDong;
import vn.n08.nhakhoa.dto.PhieuKhoView;
import vn.n08.nhakhoa.model.Tone;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Tag;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.HoanTraDialog;
import vn.n08.nhakhoa.view.dialog.NhapKhoDialog;
import vn.n08.nhakhoa.view.dialog.XuatKhoDialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;

/** UC30 Nhập kho, UC31 Xuất kho, UC32 Hoàn trả kho — phiếu hoàn tất, không sửa/xóa. */
public class PhieuKhoPage extends Page {
    private final KhoService svc = new KhoService();
    private final HintField tu = Fields.date(Now.today().minusDays(30)), den = Fields.date(Now.today());
    private final DataTable<PhieuKhoView> table = new DataTable<PhieuKhoView>()
            .col("Mã phiếu", 85, PhieuKhoView::ma)
            .col("Loại", 100, v -> new Tag(v.loaiLabel(), v.loai().equals("XUAT") ? Tone.ORANGE : v.loai().equals("HOAN_TRA") ? Tone.TEAL : Tone.BLUE))
            .col("Thời gian", 110, PhieuKhoView::ngay).col("Đối tác / nơi nhận", 220, PhieuKhoView::doiTac).col("Người lập", 140, PhieuKhoView::nguoiLap)
            .col("Số dòng", 60, PhieuKhoView::soDong).col("Tổng SL", 70, PhieuKhoView::tongSoLuong).col("Ghi chú / lý do", 220, PhieuKhoView::ghiChu)
            .emptyText("Không có phiếu trong khoảng đã chọn.");
    private final DataTable<PhieuKhoDong> lines = new DataTable<PhieuKhoDong>()
            .col("Mã lô", 80, PhieuKhoDong::maLo).col("Mặt hàng", 220, PhieuKhoDong::tenHang).col("Số lô", 80, PhieuKhoDong::soLo).col("HSD", 90, PhieuKhoDong::hanSuDung)
            .col("SL", 60, PhieuKhoDong::soLuong).col("Đơn giá nhập", 100, PhieuKhoDong::donGia).col("KSĐB", 50, d -> d.kiemSoatDacBiet() ? "Có" : "")
            .emptyText("Chọn một phiếu.");
    private final Btn hoanTu = Btn.secondary("Hoàn trả từ phiếu xuất này", e -> hoanTra(table.selected().ma()));

    public PhieuKhoPage() {
        super("Nhập / xuất / hoàn trả", "UC30 Quản lý nhập kho · UC31 Quản lý xuất kho · UC32 Quản lý hoàn trả kho");
        header.action(Btn.secondary("Hoàn trả", e -> hoanTra(null)));
        header.action(Btn.secondary("Xuất kho", e -> { if (new XuatKhoDialog(this).open() != null) reload(); }));
        header.action(Btn.primary("Nhập kho", e -> { if (new NhapKhoDialog(this).open() != null) reload(); }));
        lines.setPreferredSize(new Dimension(10, 200));
        var box = Layouts.stack(new Toolbar().label("Từ").with(tu).label("đến").with(den).with(Btn.secondary("Lọc", e -> reload()), hoanTu), table);
        box.add(Card.titled("Chi tiết phiếu", null, lines), BorderLayout.SOUTH);
        body(Card.titled("Phiếu kho", null, box));
        table.onSelect(v -> {
            hoanTu.setEnabled(v != null && v.loai().equals("XUAT"));
            if (v == null) lines.setRows(List.of());
            else Async.load(lines, () -> svc.dongPhieu(v.ma(), v.loai().equals("XUAT")));
        });
        hoanTu.setEnabled(false);
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
        Async.load(table, () -> svc.danhSachPhieu(a, b));
    }

    private void hoanTra(String px) {
        if (new HoanTraDialog(this, px).open() != null) reload();
    }
}
