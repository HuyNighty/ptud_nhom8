package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.HangMucForm;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.service.KeHoachService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import java.awt.Component;
import java.util.function.Supplier;

/** UC13 Bổ sung hạng mục vào kế hoạch đang thực hiện. */
public class HangMucDialog extends FormDialog<Boolean> {
    private final String maKh;
    private final JComboBox<DichVu> dv = Fields.combo(new KeHoachService().dichVuDieuTri(), d -> d.tenDichVu() + " · " + Fmt.money(d.donGia()), "— chọn —");
    private final HintField rang = Fields.text("VD: 36"), ngay = Fields.date(Now.today().plusDays(7));

    public HangMucDialog(Component p, String maKh) {
        super(p, "Thêm hạng mục — " + maKh, null, "Thêm", 2);
        this.maKh = maKh;
        form.add("hangMuc", "Dịch vụ *", dv, 2).add(null, "Vị trí răng *", rang).add("ngay", "Ngày dự kiến *", ngay);
    }

    @Override protected Supplier<Boolean> prepare() {
        DichVu d = Fields.selected(dv);
        HangMucForm h = new HangMucForm(d == null ? null : d.maDichVu(), rang.getText(), Fmt.parseDate(ngay.getText(), "ngay", "Ngày dự kiến"));
        return () -> { new KeHoachService().themHangMuc(maKh, h); return true; };
    }

    @Override protected String okMessage(Boolean r) { return "Đã thêm hạng mục."; }
}
