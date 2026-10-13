package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.BenhNhanForm;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.GioiTinh;
import vn.n08.nhakhoa.service.BenhNhanService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

/** UC08 Tạo hồ sơ bệnh nhân (chính thức, trực tiếp) / UC23 sửa thông tin hành chính. Cảnh báo nghi trùng cần xác nhận. */
public class BenhNhanDialog extends FormDialog<String> {
    private final BenhNhan cu;
    private final HintField hoTen = Fields.text("VD: Nguyễn Văn A");
    private final HintField ngaySinh = Fields.date(null);
    private final JComboBox<GioiTinh> gioiTinh = Fields.combo(List.of(GioiTinh.values()), GioiTinh::label, "— chọn —");
    private final HintField sdt = Fields.text("10 số, bắt đầu bằng 0");
    private final HintField diaChi = Fields.text("Số nhà, đường, xã/phường…");
    private final HintField lienHe = Fields.text("Họ tên · quan hệ · SĐT");

    public BenhNhanDialog(Component p, BenhNhan cu) {
        super(p, cu == null ? "Tạo hồ sơ bệnh nhân" : "Sửa thông tin hành chính — " + cu.maBenhNhan(),
                cu == null ? "Hồ sơ chính thức được tạo ngay kèm hồ sơ bệnh án. Hệ thống cảnh báo nếu trùng SĐT hoặc trùng họ tên + ngày sinh."
                        : "Chỉ sửa thông tin hành chính; tiền sử, dị ứng do bác sĩ cập nhật.", cu == null ? "Lưu hồ sơ" : "Lưu thay đổi", 2);
        this.cu = cu;
        form.add("hoTen", "Họ tên *", hoTen, 2).add("ngaySinh", "Ngày sinh *", ngaySinh).add("gioiTinh", "Giới tính *", gioiTinh)
                .add("soDienThoai", "Số điện thoại *", sdt).add("nguoiLienHe", "Người liên hệ", lienHe).add("diaChi", "Địa chỉ", diaChi, 2);
        if (cu != null) {
            hoTen.setText(cu.hoTen());
            ngaySinh.setText(Fmt.date(cu.ngaySinh()));
            gioiTinh.setSelectedItem(cu.gioiTinh());
            sdt.setText(cu.soDienThoai());
            diaChi.setText(cu.diaChi() == null ? "" : cu.diaChi());
            lienHe.setText(cu.nguoiLienHe() == null ? "" : cu.nguoiLienHe());
        }
    }

    @Override protected Supplier<String> prepare() {
        BenhNhanForm f = new BenhNhanForm(txt(hoTen), Fmt.parseDateOrNull(txt(ngaySinh), "ngaySinh", "Ngày sinh"), Fields.selected(gioiTinh),
                txt(sdt), txt(diaChi), txt(lienHe));
        boolean ok = daXacNhan;
        BenhNhanService s = new BenhNhanService();
        if (cu == null) return () -> s.tao(f, ok);
        return () -> { s.capNhat(cu.maBenhNhan(), f, ok); return cu.maBenhNhan(); };
    }

    @Override protected String okMessage(String ma) { return cu == null ? "Đã tạo hồ sơ " + ma + "." : "Đã lưu thông tin " + ma + "."; }
}
