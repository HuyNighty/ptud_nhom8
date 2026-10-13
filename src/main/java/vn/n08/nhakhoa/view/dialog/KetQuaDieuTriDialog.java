package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.ChiTietDieuTriView;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.KeHoachService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JTextArea;
import java.awt.Component;
import java.util.function.Supplier;

/** UC15 Ghi nhận kết quả điều trị một hạng mục; bác sĩ thực hiện được ghi lại. */
public class KetQuaDieuTriDialog extends FormDialog<Boolean> {
    private final ChiTietDieuTriView ct;
    private final HintField ngay = Fields.date(Now.today());
    private final JTextArea kq = Fields.area(3);
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— chọn bác sĩ —", true);

    public KetQuaDieuTriDialog(Component p, ChiTietDieuTriView ct) {
        super(p, "Ghi kết quả điều trị", ct.tenDichVu() + " · răng " + ct.ct().viTriRang() + " · dự kiến " + Fmt.date(ct.ct().ngayDuKien()), "Ghi kết quả", 2);
        this.ct = ct;
        form.add("bacSi", "Bác sĩ thực hiện *", bs).add("ngay", "Ngày thực hiện *", ngay).add("ketQua", "Kết quả *", Fields.scroll(kq), 2);
    }

    @Override protected Supplier<Boolean> prepare() {
        var d = Fmt.parseDate(ngay.getText(), "ngay", "Ngày thực hiện");
        String k = kq.getText(), b = bs.ma();
        return () -> { new KeHoachService().ghiKetQua(ct.ct().maChiTiet(), d, k, b); return true; };
    }

    @Override protected String okMessage(Boolean r) { return "Đã ghi kết quả điều trị."; }
}
