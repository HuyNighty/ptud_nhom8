package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.service.DichVuService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.util.function.Supplier;

/** Thêm / sửa dịch vụ và đơn giá (DichVu.capNhatDonGia). */
public class DichVuDialog extends FormDialog<String> {
    private final DichVu cu;
    private final HintField ten = Fields.text("Tên dịch vụ"), moTa = Fields.text("Mô tả");
    private final HintField gia;
    private final JCheckBox cls = new JCheckBox("Dịch vụ cận lâm sàng (dùng khi lập chỉ định)"), apDung = new JCheckBox("Đang áp dụng");

    public DichVuDialog(Component p, DichVu cu) {
        super(p, cu == null ? "Thêm dịch vụ" : "Sửa dịch vụ " + cu.maDichVu(), "Hóa đơn đã lập giữ đơn giá tại thời điểm lập; giá mới chỉ áp dụng cho hóa đơn sau.", "Lưu", 2);
        this.cu = cu;
        gia = Fields.money(cu == null ? null : cu.donGia());
        cls.setOpaque(false);
        apDung.setOpaque(false);
        apDung.setSelected(cu == null || cu.dangApDung());
        form.add("ten", "Tên dịch vụ *", ten, 2).add("donGia", "Đơn giá (₫) *", gia).add("moTa", "Mô tả", moTa).full(cls);
        if (cu != null) {
            ten.setText(cu.tenDichVu());
            moTa.setText(cu.moTa() == null ? "" : cu.moTa());
            cls.setSelected(cu.laCanLamSang());
            form.full(apDung);
        }
    }

    @Override protected Supplier<String> prepare() {
        DichVu d = new DichVu(cu == null ? null : cu.maDichVu(), txt(ten), Fmt.parseMoney(txt(gia), "donGia", "Đơn giá"), txt(moTa), cls.isSelected(), apDung.isSelected());
        return () -> new DichVuService().luu(d);
    }

    @Override protected String okMessage(String ma) { return "Đã lưu dịch vụ " + ma + "."; }
}
