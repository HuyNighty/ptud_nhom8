package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.service.PhanCongService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JComboBox;
import java.awt.Component;
import java.time.LocalDate;
import java.util.function.Supplier;

/** UC24 Thêm phân công: nhân viên, ca, ngày, nhiệm vụ. Chặn trùng ca và vượt số ca tối đa trong ngày. */
public class PhanCongDialog extends FormDialog<String> {
    private final NhanVienCombo nv = new NhanVienCombo(null, "— chọn nhân viên —", false);
    private final JComboBox<CaLamViec> ca = Fields.combo(new PhanCongService().danhSachCa(), CaLamViec::toString, "— chọn ca —");
    private final HintField ngay;
    private final HintField nhiemVu = Fields.text("VD: Khám ghế 1, phụ tá ghế 2, trực quầy…");

    public PhanCongDialog(Component p, LocalDate d, String maCa) {
        super(p, "Thêm phân công", "Một nhân viên tối đa số ca/ngày theo cấu hình (mặc định 2) và không trùng ca.", "Phân công", 2);
        ngay = Fields.date(d);
        form.add("nhanVien", "Nhân viên *", nv, 2).add("ca", "Ca *", ca).add("ngay", "Ngày *", ngay).add("nhiemVu", "Nhiệm vụ *", nhiemVu, 2);
        if (maCa != null) for (int i = 0; i < ca.getItemCount(); i++) if (ca.getItemAt(i) != null && ca.getItemAt(i).maCa().equals(maCa)) ca.setSelectedIndex(i);
    }

    @Override protected Supplier<String> prepare() {
        CaLamViec c = Fields.selected(ca);
        LocalDate d = Fmt.parseDate(txt(ngay), "ngay", "Ngày");
        String m = nv.ma(), n = txt(nhiemVu);
        return () -> new PhanCongService().them(m, c == null ? null : c.maCa(), d, n);
    }

    @Override protected String okMessage(String ma) { return "Đã tạo phân công " + ma + "."; }
}
