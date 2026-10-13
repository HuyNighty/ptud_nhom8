package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.NhanVienForm;
import vn.n08.nhakhoa.model.GioiTinh;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.NhanVienService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

/** UC22 Thêm / sửa nhân viên (UC26 hồ sơ nhân sự). Chức danh không đổi sau khi tạo. */
public class NhanVienDialog extends FormDialog<String> {
    private final NhanVien cu;
    private final HintField hoTen = Fields.text("Họ và tên"), sdt = Fields.text("10 số"), email = Fields.text("email@…"), chuyenMon = Fields.text("VD: Nội nha");
    private final HintField ngaySinh = Fields.date(null), ngayVao = Fields.date(Now.today());
    private final JComboBox<VaiTro> chucDanh = Fields.combo(List.of(VaiTro.LT, VaiTro.BS, VaiTro.YT, VaiTro.QL, VaiTro.KHO), VaiTro::label, "— chọn —");
    private final JComboBox<GioiTinh> gioiTinh = Fields.combo(List.of(GioiTinh.values()), GioiTinh::label, "—");

    public NhanVienDialog(Component p, NhanVien cu) {
        super(p, cu == null ? "Thêm nhân viên" : "Sửa hồ sơ — " + cu.maNhanVien(), cu == null ? "Tài khoản đăng nhập cấp riêng ở màn hình Tài khoản." : null,
                cu == null ? "Thêm" : "Lưu", 2);
        this.cu = cu;
        form.add("hoTen", "Họ tên *", hoTen).add("chucDanh", "Chức danh *", cu == null ? chucDanh : new JLabel(cu.chucDanh().label()))
                .add("soDienThoai", "Số điện thoại *", sdt).add("email", "Email", email).add("ngaySinh", "Ngày sinh", ngaySinh).add(null, "Giới tính", gioiTinh)
                .add(null, "Ngày vào làm", ngayVao).add(null, "Chuyên môn", chuyenMon);
        if (cu != null) {
            hoTen.setText(cu.hoTen());
            sdt.setText(cu.soDienThoai());
            email.setText(cu.email() == null ? "" : cu.email());
            chuyenMon.setText(cu.chuyenMon() == null ? "" : cu.chuyenMon());
            ngaySinh.setText(cu.ngaySinh() == null ? "" : Fmt.date(cu.ngaySinh()));
            ngayVao.setText(cu.ngayVaoLam() == null ? "" : Fmt.date(cu.ngayVaoLam()));
            gioiTinh.setSelectedItem(cu.gioiTinh());
        }
    }

    @Override protected Supplier<String> prepare() {
        NhanVienForm f = new NhanVienForm(txt(hoTen), txt(sdt), cu == null ? Fields.selected(chucDanh) : cu.chucDanh(),
                Fmt.parseDateOrNull(txt(ngaySinh), "ngaySinh", "Ngày sinh"), Fields.selected(gioiTinh), txt(email),
                Fmt.parseDateOrNull(txt(ngayVao), "ngayVaoLam", "Ngày vào làm"), txt(chuyenMon));
        NhanVienService s = new NhanVienService();
        if (cu == null) return () -> s.them(f);
        return () -> { s.capNhat(cu.maNhanVien(), f); return cu.maNhanVien(); };
    }

    @Override protected String okMessage(String ma) { return "Đã lưu nhân viên " + ma + "."; }
}
