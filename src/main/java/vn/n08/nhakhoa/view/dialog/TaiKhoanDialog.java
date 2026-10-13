package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.TaiKhoanService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

/** UC21 Cấp tài khoản cho nhân viên đang làm việc chưa có tài khoản; vai trò mặc định theo chức danh. */
public class TaiKhoanDialog extends FormDialog<String> {
    private final JComboBox<NhanVien> nv = Fields.combo(new TaiKhoanService().nhanVienChuaCoTaiKhoan(), n -> n.hoTen() + " (" + n.maNhanVien() + ") · " + n.chucDanh().label(), "— chọn —");
    private final HintField ten = Fields.text("chữ thường không dấu, số, . _");
    private final JComboBox<VaiTro> vt = Fields.combo(List.of(VaiTro.values()), VaiTro::label, "— chọn —");
    private final JPasswordField mk = new JPasswordField();

    public TaiKhoanDialog(Component p) {
        super(p, "Cấp tài khoản", "Mật khẩu tạm tối thiểu 8 ký tự, có chữ và số; giao cho nhân viên đổi sau khi đăng nhập.", "Tạo tài khoản", 2);
        form.add("nhanVien", "Nhân viên *", nv, 2).add("tenDangNhap", "Tên đăng nhập *", ten).add("vaiTro", "Vai trò *", vt).add("matKhau", "Mật khẩu tạm *", mk);
        nv.addActionListener(e -> {
            NhanVien n = Fields.selected(nv);
            if (n != null) vt.setSelectedItem(n.chucDanh());
        });
    }

    @Override protected Supplier<String> prepare() {
        NhanVien n = Fields.selected(nv);
        if (n == null) throw new BusinessException("nhanVien", "Chọn nhân viên.");
        String t = txt(ten), m = new String(mk.getPassword());
        VaiTro v = Fields.selected(vt);
        return () -> new TaiKhoanService().tao(n.maNhanVien(), t, v, m);
    }

    @Override protected String okMessage(String ma) { return "Đã tạo tài khoản " + ma + "."; }
}
