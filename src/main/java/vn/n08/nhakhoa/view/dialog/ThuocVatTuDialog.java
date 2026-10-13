package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.util.function.Supplier;

/** UC29 Thêm / sửa thuốc hoặc vật tư, định mức tối thiểu; UC33 cờ thuốc kiểm soát đặc biệt. */
public class ThuocVatTuDialog extends FormDialog<String> {
    private final boolean laThuoc;
    private final String ma;
    private final boolean daDat;
    private final HintField ten = Fields.text("Tên"), dvt = Fields.text("Viên, ống, hộp…"), dinhMuc = Fields.text("0");
    private final HintField hamLuong = Fields.text("VD: 500mg"), hoatChat = Fields.text("VD: Amoxicillin"), moTa = Fields.text("Mô tả");
    private final JCheckBox ksdb = new JCheckBox("Thuốc kiểm soát đặc biệt (ghi sổ theo dõi, xuất cần BS chịu trách nhiệm)");

    public ThuocVatTuDialog(Component p, Thuoc t, VatTu v, boolean laThuoc) {
        super(p, (t == null && v == null ? "Thêm " : "Sửa ") + (laThuoc ? "thuốc" : "vật tư"), null, "Lưu", 2);
        this.laThuoc = laThuoc;
        this.ma = laThuoc ? (t == null ? null : t.maThuoc()) : (v == null ? null : v.maVatTu());
        this.daDat = laThuoc ? t != null && t.daDatHang() : v != null && v.daDatHang();
        ksdb.setOpaque(false);
        form.add("ten", "Tên *", ten, 2).add("donViTinh", "Đơn vị tính *", dvt).add("dinhMuc", "Định mức tồn tối thiểu", dinhMuc);
        if (laThuoc) form.add(null, "Hàm lượng", hamLuong).add(null, "Hoạt chất", hoatChat).full(ksdb);
        else form.add(null, "Mô tả", moTa, 2);
        if (t != null) {
            ten.setText(t.tenThuoc()); dvt.setText(t.donViTinh()); dinhMuc.setText(String.valueOf(t.dinhMucToiThieu()));
            hamLuong.setText(t.hamLuong() == null ? "" : t.hamLuong()); hoatChat.setText(t.hoatChat() == null ? "" : t.hoatChat());
            ksdb.setSelected(t.kiemSoatDacBiet());
        }
        if (v != null) {
            ten.setText(v.tenVatTu()); dvt.setText(v.donViTinh()); dinhMuc.setText(String.valueOf(v.dinhMucToiThieu()));
            moTa.setText(v.moTa() == null ? "" : v.moTa());
        }
    }

    @Override protected Supplier<String> prepare() {
        int dm = dinhMuc.getText().isBlank() ? 0 : Fmt.parseInt(dinhMuc.getText(), "dinhMuc", "Định mức");
        KhoService s = new KhoService();
        if (laThuoc) {
            Thuoc t = new Thuoc(ma, txt(ten), txt(dvt), ksdb.isSelected(), txt(hamLuong), txt(hoatChat), dm, daDat);
            return () -> s.luuThuoc(t);
        }
        VatTu v = new VatTu(ma, txt(ten), txt(dvt), txt(moTa), dm, daDat);
        return () -> s.luuVatTu(v);
    }

    @Override protected String okMessage(String m) { return "Đã lưu " + m + "."; }
}
