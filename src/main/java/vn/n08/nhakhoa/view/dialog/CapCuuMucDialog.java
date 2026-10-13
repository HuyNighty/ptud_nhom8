package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.DanhMucCapCuu;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.service.CapCuuService;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** UC34 Cấu hình mục hộp cấp cứu (tên, đơn vị, số tối thiểu, liên kết thuốc/vật tư trong kho để bổ sung). */
public class CapCuuMucDialog extends FormDialog<String> {
    private final DanhMucCapCuu cu;
    private final HintField ten = Fields.text("Tên mục"), dvt = Fields.text("Ống, viên, cái…"), toiThieu = Fields.text("1"), ghiChu = Fields.text("Ghi chú");
    private final JComboBox<Object> lienKet;

    public CapCuuMucDialog(Component p, DanhMucCapCuu cu) {
        super(p, cu == null ? "Thêm mục hộp cấp cứu" : "Sửa mục " + cu.maMuc(), "Danh mục do phòng khám tự cấu hình; sau khi sửa, mục không còn đánh dấu dữ liệu mẫu.", "Lưu", 2);
        this.cu = cu;
        KhoService k = new KhoService();
        List<Object> items = new ArrayList<>(k.danhSachThuoc());
        items.addAll(k.danhSachVatTu());
        lienKet = Fields.combo(items, o -> o instanceof Thuoc t ? "Thuốc · " + t : "Vật tư · " + ((VatTu) o).tenVatTu(), "— không liên kết —");
        form.add("ten", "Tên mục *", ten, 2).add("donViTinh", "Đơn vị tính *", dvt).add("toiThieu", "Số lượng tối thiểu *", toiThieu)
                .add("lienKet", "Liên kết kho (để bổ sung)", lienKet, 2).add("ghiChu", "Ghi chú", ghiChu, 2);
        if (cu != null) {
            ten.setText(cu.tenMuc()); dvt.setText(cu.donViTinh()); toiThieu.setText(String.valueOf(cu.soLuongToiThieu()));
            ghiChu.setText(cu.ghiChu() == null ? "" : cu.ghiChu());
            for (int i = 0; i < lienKet.getItemCount(); i++) {
                Object o = lienKet.getItemAt(i);
                if (o instanceof Thuoc t && t.maThuoc().equals(cu.maThuoc()) || o instanceof VatTu v && v.maVatTu().equals(cu.maVatTu())) lienKet.setSelectedIndex(i);
            }
        }
    }

    @Override protected Supplier<String> prepare() {
        Object o = Fields.selected(lienKet);
        DanhMucCapCuu m = new DanhMucCapCuu(cu == null ? null : cu.maMuc(), txt(ten), txt(dvt), Fmt.parseInt(txt(toiThieu), "toiThieu", "Số lượng tối thiểu"),
                0, null, o instanceof Thuoc t ? t.maThuoc() : null, o instanceof VatTu v ? v.maVatTu() : null, true, false, txt(ghiChu));
        return () -> new CapCuuService().luuMuc(m);
    }

    @Override protected String okMessage(String ma) { return "Đã lưu mục " + ma + "."; }
}
