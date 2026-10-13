package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.TrangThaiNCC;
import vn.n08.nhakhoa.service.NhaCungCapService;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.NccDialog;

/** Danh mục nhà cung cấp (hỗ trợ UC30). Không xóa; ngừng hợp tác thì không chọn khi nhập kho. */
public class NhaCungCapPage extends Page {
    private final NhaCungCapService svc = new NhaCungCapService();
    private final HintField q = Fields.text("Tên, mã, SĐT");
    private final DataTable<NhaCungCap> table = new DataTable<NhaCungCap>()
            .col("Mã", 70, NhaCungCap::maNhaCungCap).col("Tên", 220, NhaCungCap::tenNhaCungCap).col("SĐT", 110, NhaCungCap::soDienThoai)
            .col("Email", 180, NhaCungCap::email).col("Địa chỉ", 240, NhaCungCap::diaChi).col("Trạng thái", 120, NhaCungCap::trangThai).emptyText("Không có nhà cung cấp.");
    private final Btn sua = Btn.secondary("Sửa", e -> sua());
    private final Btn doi = Btn.secondary("Ngừng / hợp tác lại", e -> doi());

    public NhaCungCapPage() {
        super("Nhà cung cấp", "Danh mục nhà cung cấp phục vụ nhập kho (UC30)");
        header.action(Btn.primary("Thêm nhà cung cấp", e -> { if (new NccDialog(this, null).open() != null) reload(); }));
        q.addActionListener(e -> reload());
        body(Card.titled("Danh sách", null, Layouts.stack(new Toolbar().with(q, Btn.secondary("Tìm", e -> reload()), sua, doi), table)));
        table.onSelect(v -> { sua.setEnabled(v != null); doi.setEnabled(v != null); });
        table.onOpen(v -> sua());
        sua.setEnabled(false);
        doi.setEnabled(false);
    }

    @Override public void reload() {
        String s = q.getText();
        Async.load(table, () -> svc.danhSach(s));
    }

    private void sua() {
        NhaCungCap n = table.selected();
        if (n != null && new NccDialog(this, n).open() != null) reload();
    }

    private void doi() {
        NhaCungCap n = table.selected();
        TrangThaiNCC moi = n.trangThai() == TrangThaiNCC.DANG_HOP_TAC ? TrangThaiNCC.NGUNG_HOP_TAC : TrangThaiNCC.DANG_HOP_TAC;
        if (!Dialogs.confirm(this, n.tenNhaCungCap(), "Chuyển sang \"" + moi.label() + "\"?", "Đồng ý", false)) return;
        Async.run(this, () -> { svc.doiTrangThai(n.maNhaCungCap(), moi); return true; }, r -> { Toast.show(this, "Đã cập nhật."); reload(); });
    }
}
