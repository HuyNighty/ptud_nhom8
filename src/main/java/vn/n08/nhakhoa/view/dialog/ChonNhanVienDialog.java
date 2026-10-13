package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import java.awt.Component;
import java.util.function.Function;
import java.util.function.Supplier;

/** Chọn một nhân viên (theo chức danh) rồi thực hiện thao tác truyền vào (VD: đổi bác sĩ, thay người, bác sĩ chịu trách nhiệm). */
public class ChonNhanVienDialog extends FormDialog<String> {
    private final NhanVienCombo combo;
    private final Function<String, String> action;
    private final String done;

    public ChonNhanVienDialog(Component p, String title, String sub, VaiTro chucDanh, boolean bacSiPhuTrach, String okText,
                              Function<String, String> action, String done) {
        super(p, title, sub, okText, 1);
        this.action = action;
        this.done = done;
        combo = new NhanVienCombo(chucDanh, "— chọn —", bacSiPhuTrach);
        form.add("bacSi", chucDanh == VaiTro.BS ? "Bác sĩ *" : "Nhân viên *", combo);
    }

    public ChonNhanVienDialog preset(String ma) {
        if (ma != null) combo.selectMa(ma);
        return this;
    }

    @Override protected Supplier<String> prepare() {
        String m = combo.ma();
        if (m == null) throw new BusinessException("bacSi", "Chọn một người.");
        return () -> action.apply(m);
    }

    @Override protected String okMessage(String r) { return done; }
}
