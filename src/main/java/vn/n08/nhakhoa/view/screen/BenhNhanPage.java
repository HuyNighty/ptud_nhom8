package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.service.BenhNhanService;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.BenhNhanChiTietPanel;
import vn.n08.nhakhoa.view.dialog.BenhNhanDialog;
import vn.n08.nhakhoa.view.dialog.TienSuDialog;
import vn.n08.nhakhoa.view.dialog.XemDialog;

import java.awt.Dimension;

/** UC08 Tạo hồ sơ, UC09 Xem hồ sơ, UC23 Quản lý hồ sơ bệnh nhân. Không có chức năng xóa hồ sơ. */
public class BenhNhanPage extends Page {
    private final BenhNhanService svc = new BenhNhanService();
    private final HintField q = Fields.text("SĐT, mã BN / hồ sơ hoặc họ tên");
    private final DataTable<BenhNhanView> table = new DataTable<BenhNhanView>()
            .col("Mã BN", 80, v -> v.bn().maBenhNhan()).col("Mã hồ sơ", 80, BenhNhanView::maHoSo).col("Họ tên", 180, v -> v.bn().hoTen())
            .col("Giới tính", 70, v -> v.bn().gioiTinh().label()).col("Ngày sinh", 90, v -> v.bn().ngaySinh()).col("SĐT", 100, v -> v.bn().soDienThoai())
            .col("Địa chỉ", 180, v -> v.bn().diaChi()).col("Lần khám cuối", 120, BenhNhanView::lanKhamCuoi).col("Còn nợ", 100, BenhNhanView::congNo)
            .emptyText("Không tìm thấy bệnh nhân.");
    private final Btn xem = Btn.secondary("Xem hồ sơ", e -> xem());
    private final Btn sua = Btn.secondary("Sửa hành chính", e -> sua());
    private final Btn tienSu = Btn.secondary("Tiền sử / dị ứng", e -> tienSu());

    public BenhNhanPage() {
        super("Hồ sơ bệnh nhân", "UC08 Tạo hồ sơ · UC09 Xem hồ sơ · UC23 Quản lý hồ sơ — hồ sơ không bị xóa");
        if (Session.can(Quyen.BENH_NHAN_TAO)) {
            header.action(Btn.primary("Tạo hồ sơ", e -> {
                String ma = new BenhNhanDialog(this, null).open();
                if (ma != null) { q.setText(ma); reload(); }
            }));
        }
        q.setPreferredSize(new Dimension(320, 32));
        q.addActionListener(e -> reload());
        Toolbar t = new Toolbar().with(q, Btn.secondary("Tìm", e -> reload()), xem);
        if (Session.can(Quyen.BENH_NHAN_SUA)) t.add(sua);
        if (Session.can(Quyen.TIEN_SU)) t.add(tienSu);
        body(Card.titled("Danh sách bệnh nhân", "Mặc định hiển thị hồ sơ mới nhất; nhấp đúp để xem chi tiết", Layouts.stack(t, table)));
        table.onOpen(v -> xem());
        table.onSelect(v -> { xem.setEnabled(v != null); sua.setEnabled(v != null); tienSu.setEnabled(v != null); });
        xem.setEnabled(false);
        sua.setEnabled(false);
        tienSu.setEnabled(false);
    }

    @Override public void reload() {
        String s = q.getText();
        Async.load(table, () -> svc.tim(s));
    }

    private void xem() {
        BenhNhanView v = table.selected();
        if (v == null) return;
        Async.run(this, () -> svc.chiTiet(v.bn().maBenhNhan()), d ->
                XemDialog.show(this, "Hồ sơ " + d.bn().maBenhNhan() + " — " + d.bn().hoTen(), new BenhNhanChiTietPanel(d), 860, 560));
    }

    private void sua() {
        BenhNhanView v = table.selected();
        if (v != null && new BenhNhanDialog(this, v.bn()).open() != null) reload();
    }

    private void tienSu() {
        BenhNhanView v = table.selected();
        if (v == null) return;
        Async.run(this, () -> svc.chiTiet(v.bn().maBenhNhan()), d -> {
            if (new TienSuDialog(this, d.bn().maBenhNhan(), d.bn().hoTen(), d.hoSo()).open() != null) reload();
        });
    }
}
