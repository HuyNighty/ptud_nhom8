package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.AppController;
import vn.n08.nhakhoa.view.component.FormDialog;

import javax.swing.JPasswordField;
import java.awt.Component;
import java.util.function.Supplier;

/** Đổi mật khẩu của chính tài khoản đang đăng nhập. */
public class DoiMatKhauDialog extends FormDialog<Boolean> {
    private final JPasswordField cu = new JPasswordField(), moi = new JPasswordField(), lai = new JPasswordField();

    public DoiMatKhauDialog(Component p) {
        super(p, "Đổi mật khẩu", "Mật khẩu mới tối thiểu 8 ký tự, có cả chữ và số.", "Đổi mật khẩu", 1);
        form.add("matKhauCu", "Mật khẩu hiện tại", cu).add("matKhau", "Mật khẩu mới", moi).add("nhapLai", "Nhập lại mật khẩu mới", lai);
    }

    @Override protected Supplier<Boolean> prepare() {
        String a = new String(cu.getPassword()), b = new String(moi.getPassword()), c = new String(lai.getPassword());
        return () -> { AppController.auth().doiMatKhau(a, b, c); return true; };
    }

    @Override protected String okMessage(Boolean r) { return "Đã đổi mật khẩu."; }
}
