package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.NhanVienView;
import vn.n08.nhakhoa.model.TrangThaiNhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.NhanVienService;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Timeline;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.NghiViecDialog;
import vn.n08.nhakhoa.view.dialog.NhanVienDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.util.List;

/** UC22 Quản lý nhân viên, UC26 Hồ sơ nhân sự, thủ tục nghỉ việc có điều kiện. */
public class NhanVienPage extends Page {
    private final NhanVienService svc = new NhanVienService();
    private final HintField q = Fields.text("Tên, mã, SĐT");
    private final JComboBox<VaiTro> cd = Fields.combo(List.of(VaiTro.values()), VaiTro::label, "Tất cả chức danh");
    private final JComboBox<TrangThaiNhanVien> tt = Fields.combo(List.of(TrangThaiNhanVien.values()), TrangThaiNhanVien::label, "Tất cả trạng thái");
    private final DataTable<NhanVienView> table = new DataTable<NhanVienView>()
            .col("Mã", 70, v -> v.nv().maNhanVien()).col("Họ tên", 170, v -> v.nv().hoTen()).col("Chức danh", 120, v -> v.nv().chucDanh())
            .col("SĐT", 100, v -> v.nv().soDienThoai()).col("Trạng thái", 110, v -> v.nv().trangThai()).col("Tài khoản", 100, NhanVienView::tenDangNhap)
            .col("TT tài khoản", 100, NhanVienView::trangThaiTaiKhoan).emptyText("Không có nhân viên.");
    private final DetailGrid info = new DetailGrid(1);
    private final Timeline tl = new Timeline();
    private final Btn sua = Btn.secondary("Sửa hồ sơ", e -> sua());
    private final Btn nghi = Btn.danger("Thủ tục nghỉ việc", e -> nghi());

    public NhanVienPage() {
        super("Nhân viên", "UC22 Quản lý nhân viên · UC26 Hồ sơ nhân sự — nhân viên không bị xóa; nghỉ việc theo thủ tục có điều kiện");
        header.action(Btn.primary("Thêm nhân viên", e -> { if (new NhanVienDialog(this, null).open() != null) reload(); }));
        tt.setSelectedItem(TrangThaiNhanVien.DANG_LAM_VIEC);
        q.addActionListener(e -> reload());
        Card list = Card.titled("Danh sách", null, Layouts.stack(new Toolbar().with(q, cd, tt, Btn.secondary("Lọc", e -> reload())), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        JPanel ib = Layouts.stack(info, new Toolbar().with(sua, nghi));
        d.add(Card.titled("Hồ sơ nhân sự", null, ib));
        d.add(javax.swing.Box.createVerticalStrut(10));
        d.add(Card.titled("Lịch sử thao tác", null, tl));
        body(Layouts.masterDetail(list, d, 440));
        table.autoSelect();
        table.onSelect(this::show);
        table.onOpen(v -> sua());
        show(null);
    }

    @Override public void reload() {
        String s = q.getText();
        VaiTro c = Fields.selected(cd);
        TrangThaiNhanVien t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSach(s, c, t), r -> show(table.selected()));
    }

    private void show(NhanVienView v) {
        info.clear();
        if (v == null) {
            info.row("Nhân viên", "Chọn một nhân viên.");
            tl.set(List.of());
        } else {
            var n = v.nv();
            info.row("Mã", n.maNhanVien()).row("Họ tên", n.hoTen()).row("Chức danh", n.chucDanh()).row("Trạng thái", n.trangThai())
                    .row("Số điện thoại", n.soDienThoai()).row("Email", n.email()).row("Ngày sinh", n.ngaySinh())
                    .row("Giới tính", n.gioiTinh() == null ? null : n.gioiTinh().label()).row("Ngày vào làm", n.ngayVaoLam()).row("Chuyên môn", n.chuyenMon())
                    .row("Ngày nghỉ việc", n.ngayNghiViec()).row("Tài khoản", v.tenDangNhap() == null ? "Chưa cấp" : v.tenDangNhap())
                    .row("TT tài khoản", v.trangThaiTaiKhoan());
            Async.run(this, () -> new NhatKyService().lichSu(n.maNhanVien()), tl::set);
        }
        boolean active = v != null && v.nv().dangLamViec();
        sua.setEnabled(active);
        nghi.setEnabled(active && v.nv().chucDanh() != VaiTro.AD);
        info.revalidate();
    }

    private void sua() {
        NhanVienView v = table.selected();
        if (v != null && v.nv().dangLamViec() && new NhanVienDialog(this, v.nv()).open() != null) reload();
    }

    private void nghi() {
        NhanVienView v = table.selected();
        if (v == null) return;
        new NghiViecDialog(this, v.nv().maNhanVien(), v.nv().hoTen()).open();
        reload();
    }
}
