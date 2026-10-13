package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.dto.XuatKhoForm;
import vn.n08.nhakhoa.model.LoaiDieuChinh;
import vn.n08.nhakhoa.service.DieuChinhKhoService;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.EditTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * UC28 Kiểm kê / UC35 Hủy, hao hụt: nhập số thực tế cho các lô có chênh lệch (để trống = không điều chỉnh).
 * Phiếu chờ một tài khoản KHÁC duyệt mới thay đổi tồn.
 */
public class DieuChinhDialog extends FormDialog<String> {
    private final JComboBox<LoaiDieuChinh> loai = Fields.combo(List.of(LoaiDieuChinh.values()), LoaiDieuChinh::label, null);
    private final JTextArea lyDo = Fields.area(2);
    private final HintField loc = Fields.text("Lọc theo tên / số lô");
    private final List<LoHangView> all = new KhoService().loConTon();
    private final EditTable<LoHangView> table = new EditTable<LoHangView>().col("Mã lô", 75, v -> v.lo().maLo()).col("Mặt hàng", 200, LoHangView::tenHang)
            .col("Số lô", 75, v -> v.lo().soLo()).col("HSD", 85, v -> v.lo().hanSuDung())
            .col("Hạn", 70, v -> v.lo().daHetHan(Now.today()) ? "HẾT HẠN" : "")
            .col("Tồn hệ thống", 85, v -> v.lo().soLuongTon()).input("Thực tế", 80, v -> null);

    public DieuChinhDialog(Component p) {
        super(p, "Lập phiếu kiểm kê / hủy / hao hụt", "Chỉ nhập cột Thực tế cho lô có chênh lệch. Hư hỏng / hết hạn chỉ được giảm tồn; loại Hết hạn chỉ cho lô đã hết hạn.",
                "Lập phiếu (chờ duyệt)", 2);
        table.setPreferredSize(new Dimension(700, 280));
        table.setRows(all);
        loc.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void f() { String s = loc.getText().toLowerCase(); table.setRows(all.stream().filter(v -> (v.tenHang() + " " + v.lo().soLo() + " " + v.lo().maLo()).toLowerCase().contains(s)).toList()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { f(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { f(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { f(); }
        });
        form.add("loai", "Loại phiếu *", loai).add(null, "Lọc lô", loc).add("dong", "Lô còn tồn (lọc trước khi nhập; đổi bộ lọc sẽ xóa số đã nhập)", table, 2)
                .add("lyDo", "Lý do / biên bản *", Fields.scroll(lyDo), 2);
    }

    @Override protected Supplier<String> prepare() {
        List<XuatKhoForm.Dong> d = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            String s = table.input(i, 6);
            if (!s.isEmpty()) d.add(new XuatKhoForm.Dong(table.rows().get(i).lo().maLo(), Fmt.parseInt(s, "dong", "Số lượng thực tế")));
        }
        if (d.isEmpty()) throw new BusinessException("dong", "Nhập số thực tế cho ít nhất một lô có chênh lệch.");
        LoaiDieuChinh l = Fields.selected(loai);
        String ly = lyDo.getText();
        return () -> new DieuChinhKhoService().lap(l, ly, d);
    }

    @Override protected String okMessage(String ma) { return "Đã lập phiếu " + ma + ", chờ duyệt."; }
}
