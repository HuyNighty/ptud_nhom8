package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.service.KhamService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.awt.Component;
import java.util.function.Supplier;

/** UC11 Lập phiếu chỉ định cận lâm sàng. */
public class ChiDinhDialog extends FormDialog<String> {
    private final String maPk;
    private final JComboBox<DichVu> dv;
    private final JTextArea yc = Fields.area(3);

    public ChiDinhDialog(Component p, String maPk, String tenBs) {
        super(p, "Chỉ định cận lâm sàng — " + maPk, "Bác sĩ chịu trách nhiệm: " + tenBs, "Lập chỉ định", 1);
        this.maPk = maPk;
        dv = Fields.combo(new KhamService().dichVuCanLamSang(), d -> d.tenDichVu() + " · " + Fmt.money(d.donGia()), "— chọn dịch vụ —");
        form.add("dichVu", "Dịch vụ cận lâm sàng *", dv).add("yeuCau", "Yêu cầu / vị trí *", Fields.scroll(yc));
    }

    @Override protected Supplier<String> prepare() {
        DichVu d = Fields.selected(dv);
        String y = yc.getText();
        return () -> new KhamService().chiDinh(maPk, d == null ? null : d.maDichVu(), y);
    }

    @Override protected String okMessage(String ma) { return "Đã lập chỉ định " + ma + "."; }
}
