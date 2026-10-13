package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.NhapKhoForm;
import vn.n08.nhakhoa.model.NhaCungCap;
import vn.n08.nhakhoa.model.Thuoc;
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
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.BoxLayout;
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

/** UC30 Lập phiếu nhập kho theo lô từ nhà cung cấp (chặn lô đã hết hạn). */
public class NhapKhoDialog extends FormDialog<String> {
    private final KhoService svc = new KhoService();
    private final JComboBox<NhaCungCap> ncc = Fields.combo(svc.nhaCungCapDangHopTac(), NhaCungCap::tenNhaCungCap, "— nhà cung cấp —");
    private final HintField ghiChu = Fields.text("Số hóa đơn NCC, ghi chú…");
    private final List<Object> hang = new ArrayList<>();
    private final Map<String, String> ten = new HashMap<>();
    private final JComboBox<Object> chon;
    private final HintField soLo = Fields.text("Số lô"), nsx = Fields.date(null), hsd = Fields.date(null), sl = Fields.text("SL"), gia = Fields.money(null);
    private final List<NhapKhoForm.Dong> dong = new ArrayList<>();
    private final DataTable<NhapKhoForm.Dong> table = new DataTable<NhapKhoForm.Dong>()
            .col("Mặt hàng", 200, d -> ten.get(d.maHang())).col("Số lô", 80, NhapKhoForm.Dong::soLo).col("NSX", 85, NhapKhoForm.Dong::ngaySanXuat)
            .col("HSD", 85, NhapKhoForm.Dong::hanSuDung).col("SL", 50, NhapKhoForm.Dong::soLuong).col("Đơn giá nhập", 100, NhapKhoForm.Dong::donGia).emptyText("Chưa có dòng nhập.");

    public NhapKhoDialog(Component p) {
        super(p, "Phiếu nhập kho", "Nhập theo lô; lô trùng (cùng mặt hàng, số lô, NCC, hạn) được cộng dồn; mặt hàng tự bỏ cờ \"đã đặt hàng\".", "Lập phiếu nhập", 2);
        for (Thuoc t : svc.danhSachThuoc()) { hang.add(t); ten.put(t.maThuoc(), t.toString()); }
        for (VatTu v : svc.danhSachVatTu()) { hang.add(v); ten.put(v.maVatTu(), v.tenVatTu()); }
        chon = Fields.combo(hang, o -> o instanceof Thuoc t ? "Thuốc · " + t : "Vật tư · " + ((VatTu) o).tenVatTu(), "— mặt hàng —");
        chon.setPreferredSize(new Dimension(260, 30));
        soLo.setColumns(7);
        sl.setColumns(4);
        form.add("ncc", "Nhà cung cấp *", ncc).add("ghiChu", "Ghi chú", ghiChu);
        JPanel entry = new JPanel();
        entry.setOpaque(false);
        entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
        entry.add(new Toolbar().with(chon, soLo, sl, gia));
        entry.add(javax.swing.Box.createVerticalStrut(6));
        entry.add(new Toolbar().label("NSX").with(nsx).label("HSD").with(hsd).with(Btn.secondary("Thêm dòng", e -> them())));
        table.setPreferredSize(new Dimension(680, 200));
        JPanel box = Layouts.stack(entry, table);
        box.add(new Toolbar().with(Btn.ghost("Xóa dòng đã chọn", e -> { dong.remove(table.selected()); table.setRows(dong); })), BorderLayout.SOUTH);
        form.add("dong", "Các dòng nhập", box, 2);
        table.setRows(dong);
    }

    private void them() {
        form.clearErrors();
        try {
            Object o = Fields.selected(chon);
            if (o == null) throw new BusinessException("dong", "Chọn mặt hàng.");
            boolean laThuoc = o instanceof Thuoc;
            String ma = laThuoc ? ((Thuoc) o).maThuoc() : ((VatTu) o).maVatTu();
            dong.add(new NhapKhoForm.Dong(laThuoc, ma, soLo.getText().trim(), Fmt.parseDateOrNull(nsx.getText(), "dong", "NSX"),
                    Fmt.parseDateOrNull(hsd.getText(), "dong", "HSD"), Fmt.parseInt(sl.getText(), "dong", "Số lượng"), Fmt.parseMoney(gia.getText(), "dong", "Đơn giá")));
            table.setRows(dong);
            soLo.setText(""); sl.setText(""); nsx.setText(""); hsd.setText("");
        } catch (BusinessException e) {
            form.showError("dong", e.getMessage());
        }
    }

    @Override protected Supplier<String> prepare() {
        NhaCungCap n = Fields.selected(ncc);
        NhapKhoForm f = new NhapKhoForm(n == null ? null : n.maNhaCungCap(), txt(ghiChu), List.copyOf(dong));
        return () -> svc.nhapKho(f);
    }

    @Override protected String okMessage(String ma) { return "Đã lập phiếu nhập " + ma + "."; }
}
