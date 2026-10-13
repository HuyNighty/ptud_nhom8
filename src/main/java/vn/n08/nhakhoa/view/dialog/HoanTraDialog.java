package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.PhieuKhoDong;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.EditTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** UC32 Hoàn trả hàng đã xuất về kho: chỉ hàng nguyên seal, không vượt số đã xuất trừ phần đã hoàn. */
public class HoanTraDialog extends FormDialog<String> {
    private final KhoService svc = new KhoService();
    private final HintField maPx = Fields.text("VD: PX000003");
    private final EditTable<PhieuKhoDong> table = new EditTable<PhieuKhoDong>().col("Mã lô", 80, PhieuKhoDong::maLo).col("Mặt hàng", 200, PhieuKhoDong::tenHang)
            .col("Số lô", 80, PhieuKhoDong::soLo).col("HSD", 90, PhieuKhoDong::hanSuDung).col("Còn được hoàn", 100, PhieuKhoDong::soLuong).input("SL hoàn trả", 90, d -> null);
    private final JCheckBox seal = new JCheckBox("Đã kiểm tra: hàng hoàn trả còn nguyên seal / niêm phong");
    private final HintField ghiChu = Fields.text("Lý do hoàn trả");
    private String loaded;

    public HoanTraDialog(Component p, String presetPx) {
        super(p, "Hoàn trả về kho", "Hàng đã bóc / mở seal không nhận lại; lô đã hết hạn không nhập lại kho.", "Lập phiếu hoàn trả", 1);
        seal.setOpaque(false);
        table.setPreferredSize(new Dimension(660, 180));
        form.add("phieu", "Phiếu xuất gốc *", new Toolbar().with(maPx, Btn.secondary("Tải dòng", e -> load())))
                .add("dong", "Số lượng hoàn trả theo lô (nhập vào cột vàng)", table).add("seal", "Xác nhận *", seal).add("ghiChu", "Lý do *", ghiChu);
        if (presetPx != null) { maPx.setText(presetPx); load(); }
    }

    private void load() {
        String m = maPx.getText().trim().toUpperCase();
        form.clearErrors();
        Async.run(this, () -> svc.dongPhieuXuat(m), r -> { loaded = m; table.setRows(r); }, e -> form.showError("phieu", e.getMessage()));
    }

    @Override protected Supplier<String> prepare() {
        if (loaded == null) throw new BusinessException("phieu", "Nhập mã phiếu xuất và bấm Tải dòng.");
        List<XuatKhoForm.Dong> d = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String s = table.input(i, 5);
            if (!s.isEmpty()) d.add(new XuatKhoForm.Dong(table.rows().get(i).maLo(), Fmt.parseInt(s, "dong", "Số lượng hoàn trả")));
        }
        boolean ok = seal.isSelected();
        String gc = txt(ghiChu), px = loaded;
        return () -> svc.hoanTra(px, d, ok, gc);
    }

    @Override protected String okMessage(String ma) { return "Đã lập phiếu hoàn trả " + ma + "."; }

}
