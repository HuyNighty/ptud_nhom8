package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.CapCuuView;
import vn.n08.nhakhoa.dto.KiemTraCapCuuForm;
import vn.n08.nhakhoa.service.CapCuuService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.EditTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** UC34 Ghi kết quả kiểm tra: số lượng đếm được và hạn dùng gần nhất của từng mục đang áp dụng. */
public class KiemTraCapCuuDialog extends FormDialog<String> {
    private final EditTable<CapCuuView> table = new EditTable<CapCuuView>().col("Mã", 50, v -> v.muc().maMuc()).col("Mục", 220, v -> v.muc().tenMuc())
            .col("ĐVT", 60, v -> v.muc().donViTinh()).col("Tối thiểu", 70, v -> v.muc().soLuongToiThieu())
            .input("SL thực tế", 85, v -> v.muc().soLuongHienCo()).input("HSD gần nhất (dd/mm/yyyy)", 160, v -> v.muc().hanSuDungGanNhat());
    private final HintField ghiChu = Fields.text("Ghi chú");

    public KiemTraCapCuuDialog(Component p, List<CapCuuView> active, int nguong) {
        super(p, "Kiểm tra hộp cấp cứu", "Một mục đạt khi SL ≥ tối thiểu và HSD còn hơn " + nguong + " ngày (ngưỡng cấu hình). Để trống HSD với vật tư không có hạn.",
                "Ghi kết quả", 1);
        table.setPreferredSize(new Dimension(680, 260));
        table.setRows(active);
        form.add("dong", "Kết quả từng mục (sửa cột vàng)", table).add("ghiChu", "Ghi chú", ghiChu);
    }

    @Override protected Supplier<String> prepare() {
        List<KiemTraCapCuuForm.Dong> d = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            CapCuuView v = table.rows().get(i);
            d.add(new KiemTraCapCuuForm.Dong(v.muc().maMuc(), Fmt.parseInt(table.input(i, 4), "dong", "SL thực tế của " + v.muc().tenMuc()),
                    Fmt.parseDateOrNull(table.input(i, 5), "dong", "HSD của " + v.muc().tenMuc())));
        }
        KiemTraCapCuuForm f = new KiemTraCapCuuForm(txt(ghiChu), d);
        return () -> new CapCuuService().kiemTra(f);
    }

    @Override protected String okMessage(String ma) { return "Đã ghi kết quả kiểm tra " + ma + "."; }
}
