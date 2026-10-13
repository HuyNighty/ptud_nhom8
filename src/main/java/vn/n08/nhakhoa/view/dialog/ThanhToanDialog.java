package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.dto.ThanhToanForm;
import vn.n08.nhakhoa.model.PhuongThucThanhToan;
import vn.n08.nhakhoa.service.HoaDonService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * UC04 Thu tiền một lần: mỗi lần chọn một phương thức (Tiền mặt / Chuyển khoản / Thẻ POS).
 * CK/POS bắt buộc mã giao dịch; tiền mặt nhập tiền khách đưa và hiện tiền thối. Không thu vượt số còn nợ.
 */
public class ThanhToanDialog extends FormDialog<String> {
    private final HoaDonView hd;
    private final HintField soTien;
    private final JComboBox<PhuongThucThanhToan> pt = Fields.combo(List.of(PhuongThucThanhToan.values()), PhuongThucThanhToan::label, null);
    private final HintField maGd = Fields.text("Mã tham chiếu trên biên lai CK / POS");
    private final HintField khachDua = Fields.money(null);
    private final JLabel thoi = new JLabel("Tiền thối: —");

    public ThanhToanDialog(Component p, HoaDonView hd) {
        super(p, "Thu tiền — " + hd.hd().maHoaDon(), "Không tích hợp cổng thanh toán trực tuyến; nhân viên đối chiếu biên lai rồi nhập mã giao dịch.", "Ghi nhận thu", 2);
        this.hd = hd;
        soTien = Fields.money(hd.conNo());
        DetailGrid g = new DetailGrid(3);
        g.row("Tổng tiền", hd.tongTien()).row("Đã thu", hd.daThanhToan()).row("Còn nợ", hd.conNo());
        form.full(g);
        form.add("soTien", "Số tiền thu lần này *", soTien).add("phuongThuc", "Phương thức *", pt)
                .add("maGiaoDich", "Mã giao dịch (CK / POS)", maGd).add("tienKhachDua", "Tiền khách đưa (tiền mặt)", khachDua);
        thoi.setFont(Theme.H2);
        thoi.setForeground(Theme.GREEN);
        form.full(thoi);
        pt.addActionListener(e -> mode());
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tinhThoi(); }
            public void removeUpdate(DocumentEvent e) { tinhThoi(); }
            public void changedUpdate(DocumentEvent e) { tinhThoi(); }
        };
        soTien.getDocument().addDocumentListener(dl);
        khachDua.getDocument().addDocumentListener(dl);
        mode();
    }

    private void mode() {
        boolean tm = Fields.selected(pt) == PhuongThucThanhToan.TIEN_MAT;
        maGd.setEnabled(!tm);
        khachDua.setEnabled(tm);
        thoi.setVisible(tm);
        if (tm) maGd.setText(""); else khachDua.setText("");
        tinhThoi();
    }

    private void tinhThoi() {
        try {
            BigDecimal a = Fmt.parseMoney(soTien.getText(), "x", "x"), b = Fmt.parseMoney(khachDua.getText(), "x", "x");
            BigDecimal t = b.subtract(a);
            thoi.setText(t.signum() < 0 ? "Khách đưa thiếu " + Fmt.money(t.negate()) : "Tiền thối: " + Fmt.money(t));
            thoi.setForeground(t.signum() < 0 ? Theme.RED : Theme.GREEN);
        } catch (BusinessException e) {
            thoi.setText("Tiền thối: —");
        }
    }

    @Override protected Supplier<String> prepare() {
        PhuongThucThanhToan p = Fields.selected(pt);
        BigDecimal a = Fmt.parseMoney(soTien.getText(), "soTien", "Số tiền");
        BigDecimal k = p == PhuongThucThanhToan.TIEN_MAT ? Fmt.parseMoney(khachDua.getText(), "tienKhachDua", "Tiền khách đưa") : null;
        ThanhToanForm f = new ThanhToanForm(a, p, txt(maGd), k);
        return () -> new HoaDonService().thuTien(hd.hd().maHoaDon(), f);
    }

    @Override protected String okMessage(String ma) { return "Đã ghi nhận lần thu " + ma + "."; }
}
