package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.TrangThaiNCC;
import vn.n08.nhakhoa.service.NhaCungCapService;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import java.awt.Component;
import java.util.function.Supplier;

/** Thêm / sửa nhà cung cấp. */
public class NccDialog extends FormDialog<String> {
    private final NhaCungCap cu;
    private final HintField ten = Fields.text("Tên"), sdt = Fields.text("Số điện thoại"), email = Fields.text("Email"), diaChi = Fields.text("Địa chỉ");

    public NccDialog(Component p, NhaCungCap cu) {
        super(p, cu == null ? "Thêm nhà cung cấp" : "Sửa " + cu.maNhaCungCap(), null, "Lưu", 2);
        this.cu = cu;
        form.add("ten", "Tên nhà cung cấp *", ten, 2).add("soDienThoai", "Số điện thoại *", sdt).add("email", "Email", email).add("diaChi", "Địa chỉ", diaChi, 2);
        if (cu != null) {
            ten.setText(cu.tenNhaCungCap()); sdt.setText(cu.soDienThoai());
            email.setText(cu.email() == null ? "" : cu.email()); diaChi.setText(cu.diaChi() == null ? "" : cu.diaChi());
        }
    }

    @Override protected Supplier<String> prepare() {
        NhaCungCap n = new NhaCungCap(cu == null ? null : cu.maNhaCungCap(), txt(ten), txt(diaChi), txt(sdt), txt(email),
                cu == null ? TrangThaiNCC.DANG_HOP_TAC : cu.trangThai());
        return () -> new NhaCungCapService().luu(n);
    }

    @Override protected String okMessage(String ma) { return "Đã lưu " + ma + "."; }
}
