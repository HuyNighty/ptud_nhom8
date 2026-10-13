package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.DonThuocForm;
import vn.n08.nhakhoa.dto.DonThuocView;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.service.KhamService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** UC16 Kê đơn: thêm dòng thuốc; khi lưu, Service cảnh báo dị ứng / thuốc kiểm soát đặc biệt để xác nhận. */
public class DonThuocDialog extends FormDialog<String> {
    private final String maPk;
    private final List<Thuoc> thuoc = new KhamService().danhMucThuoc();
    private final Map<String, Thuoc> byMa = thuoc.stream().collect(Collectors.toMap(Thuoc::maThuoc, t -> t));
    private final JComboBox<Thuoc> chon = Fields.combo(thuoc, t -> t + (t.kiemSoatDacBiet() ? "  [KSĐB]" : ""), "— chọn thuốc —");
    private final HintField sl = Fields.text("SL"), lieu = Fields.text("VD: 1 viên/lần"), cach = Fields.text("VD: uống sau ăn, 2 lần/ngày"), ngay = Fields.text("ngày");
    private final JTextArea loiDan = Fields.area(2);
    private final List<DonThuocForm.Dong> dong = new ArrayList<>();
    private final DataTable<DonThuocForm.Dong> table = new DataTable<DonThuocForm.Dong>()
            .col("Thuốc", 200, d -> byMa.get(d.maThuoc()).toString()).col("SL", 50, DonThuocForm.Dong::soLuong).col("Liều", 110, DonThuocForm.Dong::lieuDung)
            .col("Cách dùng", 170, DonThuocForm.Dong::cachDung).col("Ngày", 50, DonThuocForm.Dong::soNgayDung)
            .col("KSĐB", 50, d -> byMa.get(d.maThuoc()).kiemSoatDacBiet() ? "Có" : "").emptyText("Chưa có thuốc trong đơn.");

    public DonThuocDialog(Component p, String maPk, String benhNhan, String diUng, DonThuocView cu) {
        super(p, "Kê đơn — " + maPk, benhNhan + (diUng == null || diUng.isBlank() ? "" : " · DỊ ỨNG: " + diUng), "Lưu đơn thuốc", 1);
        this.maPk = maPk;
        if (cu != null) {
            for (DonThuocView.Dong d : cu.dong()) dong.add(new DonThuocForm.Dong(d.ct().maThuoc(), d.ct().soLuong(), d.ct().lieuDung(), d.ct().cachDung(), d.ct().soNgayDung()));
            loiDan.setText(cu.don().loiDan() == null ? "" : cu.don().loiDan());
        }
        sl.setColumns(4);
        ngay.setColumns(4);
        chon.setPreferredSize(new Dimension(240, 30));
        Toolbar add = new Toolbar().with(chon, sl, lieu, cach, ngay, Btn.secondary("Thêm", e -> them()));
        Toolbar rm = new Toolbar().with(Btn.ghost("Xóa dòng đã chọn", e -> { dong.remove(table.selected()); table.setRows(dong); }));
        table.setPreferredSize(new Dimension(720, 200));
        JPanel box = Layouts.stack(add, table);
        box.add(rm, BorderLayout.SOUTH);
        if (diUng != null && !diUng.isBlank()) form.full(Layouts.note("Hồ sơ ghi dị ứng: <b>" + diUng + "</b>. Thuốc trùng tên / hoạt chất sẽ được cảnh báo khi lưu.", true));
        form.add("dong", "Thuốc trong đơn", box).add("loiDan", "Lời dặn", Fields.scroll(loiDan));
        table.setRows(dong);
    }

    private void them() {
        form.clearErrors();
        Thuoc t = Fields.selected(chon);
        try {
            if (t == null) throw new BusinessException("dong", "Chọn thuốc.");
            int s = Fmt.parseInt(sl.getText(), "dong", "Số lượng");
            int n = Fmt.parseInt(ngay.getText(), "dong", "Số ngày dùng");
            if (lieu.getText().isBlank() || cach.getText().isBlank()) throw new BusinessException("dong", "Nhập liều dùng và cách dùng.");
            dong.removeIf(d -> d.maThuoc().equals(t.maThuoc()));
            dong.add(new DonThuocForm.Dong(t.maThuoc(), s, lieu.getText().trim(), cach.getText().trim(), n));
            table.setRows(dong);
            sl.setText(""); lieu.setText(""); cach.setText(""); ngay.setText("");
        } catch (BusinessException e) {
            form.showError("dong", e.getMessage());
        }
    }

    @Override protected Supplier<String> prepare() {
        DonThuocForm f = new DonThuocForm(loiDan.getText(), List.copyOf(dong));
        boolean ok = daXacNhan;
        return () -> new KhamService().keDon(maPk, f, ok);
    }

    @Override protected String okMessage(String ma) { return "Đã lưu đơn thuốc " + ma + "."; }
}
