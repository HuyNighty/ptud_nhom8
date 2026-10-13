package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.BenhNhanService;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JTextArea;
import java.awt.Component;
import java.util.function.Supplier;

/** Bác sĩ cập nhật tiền sử bệnh / dị ứng (Admin phải chọn bác sĩ chịu trách nhiệm). */
public class TienSuDialog extends FormDialog<Boolean> {
    private final String maBn;
    private final JTextArea tienSu = Fields.area(3), diUng = Fields.area(2);
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— chọn bác sĩ —", true);

    public TienSuDialog(Component p, String maBn, String tenBn, HoSoBenhAn hs) {
        super(p, "Tiền sử & dị ứng — " + tenBn, "Thông tin dị ứng được dùng để cảnh báo khi kê đơn.", "Lưu", 1);
        this.maBn = maBn;
        if (hs != null) {
            tienSu.setText(hs.tienSuBenh() == null ? "" : hs.tienSuBenh());
            diUng.setText(hs.diUng() == null ? "" : hs.diUng());
        }
        form.add("bacSi", "Bác sĩ chịu trách nhiệm *", bs).add("tienSu", "Tiền sử bệnh", Fields.scroll(tienSu)).add("diUng", "Dị ứng (thuốc, hoạt chất)", Fields.scroll(diUng));
    }

    @Override protected Supplier<Boolean> prepare() {
        String a = tienSu.getText(), b = diUng.getText(), m = bs.ma();
        return () -> { new BenhNhanService().capNhatTienSu(maBn, a, b, m); return true; };
    }

    @Override protected String okMessage(Boolean r) { return "Đã cập nhật tiền sử / dị ứng."; }
}
