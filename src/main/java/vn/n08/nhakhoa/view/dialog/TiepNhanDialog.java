package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.TiepNhanService;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.util.function.Supplier;

/** UC02 Tiếp nhận trực tiếp (không hẹn): chọn bác sĩ khám, ghi triệu chứng → tạo phiếu khám chờ khám. */
public class TiepNhanDialog extends FormDialog<String> {
    private final BenhNhanView bn;
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— chọn bác sĩ —", false);
    private final JTextArea trieuChung = Fields.area(3);

    public TiepNhanDialog(Component p, BenhNhanView bn) {
        super(p, "Tiếp nhận bệnh nhân", "Tạo phiếu khám cho bệnh nhân đến trực tiếp, không có lịch hẹn.", "Tạo phiếu khám", 1);
        this.bn = bn;
        form.add(null, "Bệnh nhân", new JLabel(bn.bn().maBenhNhan() + " · " + bn.bn().hoTen() + " · " + bn.bn().soDienThoai()))
                .add("bacSi", "Bác sĩ khám *", bs).add("trieuChung", "Triệu chứng / lý do khám", Fields.scroll(trieuChung));
    }

    @Override protected Supplier<String> prepare() {
        String m = bs.ma(), tc = trieuChung.getText();
        return () -> new TiepNhanService().tiepNhanTrucTiep(bn.bn().maBenhNhan(), m, tc);
    }

    @Override protected String okMessage(String ma) { return "Đã tạo phiếu khám " + ma + "."; }
}
