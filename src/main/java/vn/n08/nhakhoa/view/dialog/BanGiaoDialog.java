package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.BanGiaoForm;
import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.service.PhanCongService;
import vn.n08.nhakhoa.service.BanGiaoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

/** UC19 Lập biên bản giao nhận ca. Người nhận (hoặc quản lý khác người lập) xác nhận sau. */
public class BanGiaoDialog extends FormDialog<String> {
    private final HintField ngay = Fields.date(Now.today());
    private final NhanVienCombo giao = new NhanVienCombo(null, "— người giao —", false), nhan = new NhanVienCombo(null, "— người nhận —", false);
    private final JComboBox<CaLamViec> caGiao, caNhan;
    private final JTextArea noiDung = Fields.area(4);
    private final JCheckBox chenh = new JCheckBox("Có chênh lệch (số lượng / tiền / dụng cụ) — người xác nhận phải ghi chú");

    public BanGiaoDialog(Component p) {
        super(p, "Lập biên bản bàn giao ca", "Biên bản do tài khoản " + Session.user().tenDangNhap() + " lập; tài khoản này không được tự xác nhận.", "Lập biên bản", 2);
        List<CaLamViec> ca = new PhanCongService().danhSachCa();
        caGiao = Fields.combo(ca, CaLamViec::toString, "— ca giao —");
        caNhan = Fields.combo(ca, CaLamViec::toString, "— ca nhận —");
        chenh.setOpaque(false);
        giao.selectMa(Session.user().maNhanVien());
        if (!Session.can(Quyen.PHAN_CONG)) giao.setEnabled(false);
        form.add("ngay", "Ngày *", ngay, 2).add("nguoiGiao", "Người giao *", giao).add("caGiao", "Ca giao *", caGiao)
                .add("nguoiNhan", "Người nhận *", nhan).add("caNhan", "Ca nhận *", caNhan).add("noiDung", "Nội dung bàn giao *", Fields.scroll(noiDung), 2).full(chenh);
    }

    @Override protected Supplier<String> prepare() {
        CaLamViec a = Fields.selected(caGiao), b = Fields.selected(caNhan);
        BanGiaoForm f = new BanGiaoForm(Fmt.parseDate(txt(ngay), "ngay", "Ngày"), giao.ma(), a == null ? null : a.maCa(), nhan.ma(),
                b == null ? null : b.maCa(), noiDung.getText(), chenh.isSelected());
        return () -> new BanGiaoService().tao(f);
    }

    @Override protected String okMessage(String ma) { return "Đã lập biên bản " + ma + "."; }
}
