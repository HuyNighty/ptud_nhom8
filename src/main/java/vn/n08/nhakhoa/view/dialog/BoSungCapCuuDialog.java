package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.CapCuuView;
import vn.n08.nhakhoa.dto.LoHangView;
import vn.n08.nhakhoa.service.CapCuuService;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.util.function.Supplier;

/** UC34 Bổ sung mục hộp cấp cứu từ kho: tạo phiếu xuất kho (bộ phận "Hộp cấp cứu") từ lô còn hạn. */
public class BoSungCapCuuDialog extends FormDialog<String> {
    private final CapCuuView v;
    private final JComboBox<LoHangView> lo;
    private final HintField sl = Fields.text("SL");

    public BoSungCapCuuDialog(Component p, CapCuuView v) {
        super(p, "Bổ sung từ kho — " + v.muc().tenMuc(), "Tạo phiếu xuất kho; số hiện có của mục tăng tương ứng.", "Xuất & bổ sung", 1);
        this.v = v;
        boolean laThuoc = v.muc().maThuoc() != null;
        String ma = laThuoc ? v.muc().maThuoc() : v.muc().maVatTu();
        lo = Fields.combo(new KhoService().loCua(laThuoc, ma).stream().filter(l -> l.lo().soLuongTon() > 0 && !l.lo().daHetHan(Now.today())).toList(),
                l -> l.lo().maLo() + " · lô " + l.lo().soLo() + " · HSD " + Fmt.date(l.lo().hanSuDung()) + " · tồn " + l.lo().soLuongTon(), "— chọn lô còn hạn —");
        int thieu = Math.max(0, v.muc().soLuongToiThieu() - v.muc().soLuongHienCo());
        sl.setText(String.valueOf(Math.max(thieu, 1)));
        form.add(null, "Hiện có / tối thiểu", new JLabel(v.muc().soLuongHienCo() + " / " + v.muc().soLuongToiThieu() + " " + v.muc().donViTinh()))
                .add("lo", "Lô xuất *", lo).add("soLuong", "Số lượng *", sl);
    }

    @Override protected Supplier<String> prepare() {
        LoHangView l = Fields.selected(lo);
        if (l == null) throw new BusinessException("lo", "Chọn lô.");
        int n = Fmt.parseInt(txt(sl), "soLuong", "Số lượng");
        return () -> new CapCuuService().boSungTuKho(v.muc().maMuc(), l.lo().maLo(), n);
    }

    @Override protected String okMessage(String px) { return "Đã bổ sung, phiếu xuất " + px + "."; }
}
