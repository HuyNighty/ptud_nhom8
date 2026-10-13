package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.service.PhanCongService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import java.awt.Component;
import java.util.function.Supplier;

/** CaLamViec.capNhatCa: tên, giờ, định mức bác sĩ / y tá / lễ tân tối thiểu. */
public class CaLamViecDialog extends FormDialog<Boolean> {
    private final CaLamViec ca;
    private final HintField ten = Fields.text("Tên ca"), bd = Fields.text("HH:mm"), kt = Fields.text("HH:mm"), bs = Fields.text("0"), yt = Fields.text("0"), lt = Fields.text("0");

    public CaLamViecDialog(Component p, CaLamViec ca) {
        super(p, "Cập nhật ca " + ca.maCa(), "Định mức dùng để cảnh báo ca thiếu người trên lịch tuần và Tổng quan.", "Lưu", 3);
        this.ca = ca;
        ten.setText(ca.tenCa()); bd.setText(Fmt.time(ca.gioBatDau())); kt.setText(Fmt.time(ca.gioKetThuc()));
        bs.setText(String.valueOf(ca.soBacSi())); yt.setText(String.valueOf(ca.soYTa())); lt.setText(String.valueOf(ca.soLeTan()));
        form.add("tenCa", "Tên ca *", ten).add("gio", "Bắt đầu *", bd).add(null, "Kết thúc *", kt)
                .add("dinhMuc", "Bác sĩ tối thiểu", bs).add(null, "Y tá tối thiểu", yt).add(null, "Lễ tân tối thiểu", lt);
    }

    @Override protected Supplier<Boolean> prepare() {
        CaLamViec x = new CaLamViec(ca.maCa(), txt(ten), Fmt.parseTime(txt(bd), "gio", "Giờ bắt đầu"), Fmt.parseTime(txt(kt), "gio", "Giờ kết thúc"),
                Fmt.parseInt(txt(bs), "dinhMuc", "Định mức BS"), Fmt.parseInt(txt(yt), "dinhMuc", "Định mức YT"), Fmt.parseInt(txt(lt), "dinhMuc", "Định mức LT"));
        return () -> { new PhanCongService().capNhatCa(x); return true; };
    }

    @Override protected String okMessage(Boolean r) { return "Đã cập nhật ca."; }
}
