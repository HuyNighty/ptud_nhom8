package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.Tone;
import vn.n08.nhakhoa.service.DichVuService;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Tag;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.DichVuDialog;

/** Bảng giá dịch vụ: xem (thu ngân, bác sĩ) và cập nhật đơn giá (quản lý) — DichVu.capNhatDonGia. */
public class DichVuPage extends Page {
    private final DichVuService svc = new DichVuService();
    private final boolean sua = Session.can(Quyen.BANG_GIA);
    private final DataTable<DichVu> table = new DataTable<DichVu>()
            .col("Mã", 60, DichVu::maDichVu).col("Tên dịch vụ", 240, DichVu::tenDichVu).col("Đơn giá", 120, DichVu::donGia)
            .col("Loại", 110, d -> d.laCanLamSang() ? new Tag("Cận lâm sàng", Tone.TEAL) : new Tag("Điều trị / khám", Tone.BLUE))
            .col("Áp dụng", 100, d -> d.dangApDung() ? new Tag("Đang áp dụng", Tone.GREEN) : new Tag("Ngừng", Tone.GRAY)).col("Mô tả", 260, DichVu::moTa)
            .emptyText("Chưa có dịch vụ.");
    private final Btn suaBtn = Btn.secondary("Sửa / đổi giá", e -> sua());

    public DichVuPage() {
        super("Bảng giá dịch vụ", "Đơn giá hiện hành dùng khi lập hóa đơn và tính chi phí dự kiến của kế hoạch");
        if (sua) header.action(Btn.primary("Thêm dịch vụ", e -> { if (new DichVuDialog(this, null).open() != null) reload(); }));
        Toolbar t = new Toolbar();
        if (sua) t.with(suaBtn);
        body(Card.titled("Dịch vụ", sua ? null : "Chỉ quản lý được đổi giá", Layouts.stack(t, table)));
        table.onSelect(v -> suaBtn.setEnabled(v != null));
        table.onOpen(v -> { if (sua) sua(); });
        suaBtn.setEnabled(false);
    }

    @Override public void reload() {
        Async.load(table, svc::danhSach);
    }

    private void sua() {
        DichVu d = table.selected();
        if (d != null && new DichVuDialog(this, d).open() != null) reload();
    }
}
