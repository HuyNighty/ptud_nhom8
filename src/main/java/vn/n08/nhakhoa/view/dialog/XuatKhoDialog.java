package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.NhanVienCombo;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * UC31 Phiếu xuất kho theo lô: phân bổ FEFO tự động (hết hạn trước xuất trước), chặn lô hết hạn và tồn không đủ.
 * UC33: có thuốc kiểm soát đặc biệt thì bắt buộc người nhận và bác sĩ chịu trách nhiệm.
 */
public class XuatKhoDialog extends FormDialog<String> {
    private final KhoService svc = new KhoService();
    private final HintField boPhan = Fields.text("VD: Phòng khám 1"), nguoiNhan = Fields.text("Họ tên người nhận"), lyDo = Fields.text("Mục đích sử dụng");
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— (bắt buộc nếu có thuốc KSĐB) —", false);
    private final List<Object> hang = new ArrayList<>();
    private final JComboBox<Object> chon;
    private final HintField sl = Fields.text("SL");
    private final List<XuatKhoForm.Dong> dong = new ArrayList<>();
    private final Map<String, LoHangView> lo = new HashMap<>();
    private final DataTable<XuatKhoForm.Dong> table = new DataTable<XuatKhoForm.Dong>()
            .col("Mặt hàng", 200, d -> lo.get(d.maLo()).tenHang()).col("Mã lô", 80, XuatKhoForm.Dong::maLo).col("Số lô", 80, d -> lo.get(d.maLo()).lo().soLo())
            .col("HSD", 90, d -> lo.get(d.maLo()).lo().hanSuDung()).col("Tồn lô", 60, d -> lo.get(d.maLo()).lo().soLuongTon()).col("SL xuất", 60, XuatKhoForm.Dong::soLuong)
            .col("KSĐB", 50, d -> lo.get(d.maLo()).kiemSoatDacBiet() ? "Có" : "").emptyText("Chọn mặt hàng, số lượng rồi bấm Phân bổ FEFO.");

    public XuatKhoDialog(Component p) {
        super(p, "Phiếu xuất kho", "Hệ thống chọn lô còn hạn có HSD gần nhất trước; lô đã hết hạn không được xuất.", "Lập phiếu xuất", 2);
        hang.addAll(svc.danhSachThuoc());
        hang.addAll(svc.danhSachVatTu());
        chon = Fields.combo(hang, o -> o instanceof Thuoc t ? "Thuốc · " + t + (t.kiemSoatDacBiet() ? " [KSĐB]" : "") : "Vật tư · " + ((VatTu) o).tenVatTu(), "— mặt hàng —");
        chon.setPreferredSize(new Dimension(300, 30));
        sl.setColumns(5);
        form.add("boPhanNhan", "Bộ phận nhận *", boPhan).add("nguoiNhan", "Người nhận", nguoiNhan).add("lyDo", "Lý do", lyDo).add("bacSi", "BS chịu trách nhiệm (thuốc KSĐB)", bs);
        table.setPreferredSize(new Dimension(680, 200));
        JPanel box = Layouts.stack(new Toolbar().with(chon, sl, Btn.secondary("Phân bổ FEFO", e -> fefo())), table);
        box.add(new Toolbar().with(Btn.ghost("Xóa dòng đã chọn", e -> { dong.remove(table.selected()); table.setRows(dong); })), BorderLayout.SOUTH);
        form.add("dong", "Các dòng xuất", box, 2);
        table.setRows(dong);
    }

    private void fefo() {
        form.clearErrors();
        Object o = Fields.selected(chon);
        if (o == null) { form.showError("dong", "Chọn mặt hàng."); return; }
        int n;
        try {
            n = Fmt.parseInt(sl.getText(), "dong", "Số lượng");
        } catch (BusinessException e) {
            form.showError("dong", e.getMessage());
            return;
        }
        boolean laThuoc = o instanceof Thuoc;
        String ma = laThuoc ? ((Thuoc) o).maThuoc() : ((VatTu) o).maVatTu();
        Async.run(this, () -> {
            List<XuatKhoForm.Dong> alloc = svc.goiYFefo(laThuoc, ma, n);
            for (LoHangView v : svc.loCua(laThuoc, ma)) lo.put(v.lo().maLo(), v);
            return alloc;
        }, alloc -> {
            for (XuatKhoForm.Dong d : alloc) {
                if (dong.stream().anyMatch(x -> x.maLo().equals(d.maLo()))) { form.showError("dong", "Lô " + d.maLo() + " đã có trong phiếu; xóa dòng cũ trước."); return; }
            }
            dong.addAll(alloc);
            table.setRows(dong);
            sl.setText("");
        }, e -> form.showError("dong", e.getMessage()));
    }

    @Override protected Supplier<String> prepare() {
        XuatKhoForm f = new XuatKhoForm(txt(boPhan), txt(nguoiNhan), txt(lyDo), bs.ma(), List.copyOf(dong));
        return () -> svc.xuatKho(f);
    }

    @Override protected String okMessage(String ma) { return "Đã lập phiếu xuất " + ma + "."; }

}
