package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.DanhGiaView;
import vn.n08.nhakhoa.model.ChiTietDanhGia;
import vn.n08.nhakhoa.model.TrangThaiDanhGia;
import vn.n08.nhakhoa.service.DanhGiaService;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.DanhGiaDialog;

import javax.swing.JComboBox;
import java.util.List;

/** UC25 Đánh giá nhân sự theo quý (Figma QL-07). */
public class DanhGiaPage extends Page {
    private final DanhGiaService svc = new DanhGiaService();
    private final JComboBox<String> ky = Fields.combo(DanhGiaService.cacKy(), s -> "Kỳ " + s, null);
    private final DataTable<DanhGiaView> table = new DataTable<DanhGiaView>()
            .col("Mã NV", 70, v -> v.nv().maNhanVien()).col("Họ tên", 170, v -> v.nv().hoTen()).col("Chức danh", 110, v -> v.nv().chucDanh())
            .col("Điểm TB", 70, v -> v.dg() == null ? null : v.dg().diem()).col("Trạng thái", 110, v -> v.dg() == null ? "Chưa đánh giá" : v.dg().trangThai())
            .col("Người đánh giá", 150, DanhGiaView::tenNguoiDanhGia).col("Ngày", 90, v -> v.dg() == null ? null : v.dg().ngayDanhGia())
            .col("Nhận xét", 240, v -> v.dg() == null ? null : v.dg().nhanXet()).emptyText("Không có nhân viên.");
    private final Btn danhGia = Btn.primary("Đánh giá / sửa nháp", e -> danhGia());
    private final Btn xem = Btn.secondary("Xem điểm từng tiêu chí", e -> xem());

    public DanhGiaPage() {
        super("Đánh giá nhân sự", "UC25 — 5 tiêu chí, thang 1–5; không tự đánh giá; đã gửi thì không sửa");
        ky.addActionListener(e -> reload());
        body(Card.titled("Đánh giá theo kỳ", null, Layouts.stack(new Toolbar().with(ky, danhGia, xem), table)));
        table.onSelect(v -> {
            boolean self = v != null && v.nv().maNhanVien().equals(Session.user().maNhanVien());
            danhGia.setEnabled(v != null && !self && (v.dg() == null ? v.nv().dangLamViec() : v.dg().trangThai() == TrangThaiDanhGia.NHAP));
            xem.setEnabled(v != null && v.dg() != null);
        });
        table.onOpen(v -> { if (danhGia.isEnabled()) danhGia(); else if (xem.isEnabled()) xem(); });
        danhGia.setEnabled(false);
        xem.setEnabled(false);
    }

    @Override public void reload() {
        String k = (String) ky.getSelectedItem();
        Async.load(table, () -> svc.theoKy(k));
    }

    private void danhGia() {
        DanhGiaView v = table.selected();
        String k = (String) ky.getSelectedItem();
        Async.run(this, () -> v.dg() == null ? List.<ChiTietDanhGia>of() : svc.chiTiet(v.dg().maDanhGia()), cu -> {
            if (new DanhGiaDialog(this, v, k, cu).open() != null) reload();
        });
    }

    private void xem() {
        DanhGiaView v = table.selected();
        Async.run(this, () -> svc.chiTiet(v.dg().maDanhGia()), cu -> {
            DataTable<ChiTietDanhGia> t = new DataTable<ChiTietDanhGia>().col("Tiêu chí", 260, ChiTietDanhGia::tieuChi).col("Điểm", 60, ChiTietDanhGia::diem);
            t.setRows(cu);
            vn.n08.nhakhoa.view.dialog.XemDialog.show(this, "Đánh giá " + v.nv().hoTen() + " — " + v.dg().kyDanhGia() + " · TB " + v.dg().diem(), t, 460, 340);
        });
    }
}
