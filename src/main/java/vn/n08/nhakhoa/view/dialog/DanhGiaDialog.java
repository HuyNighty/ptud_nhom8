package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.DanhGiaView;
import vn.n08.nhakhoa.model.ChiTietDanhGia;
import vn.n08.nhakhoa.service.DanhGiaService;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** UC25 Chấm 5 tiêu chí (1–5), nhận xét; lưu nháp hoặc gửi (đã gửi thì khóa). */
public class DanhGiaDialog extends FormDialog<String> {
    private final DanhGiaView v;
    private final String ky;
    private final List<JSpinner> diem = new ArrayList<>();
    private final JTextArea nhanXet = Fields.area(3);
    private final JLabel tb = new JLabel();
    private boolean gui;

    public DanhGiaDialog(Component p, DanhGiaView v, String ky, List<ChiTietDanhGia> cu) {
        super(p, "Đánh giá " + v.nv().hoTen() + " — kỳ " + ky, "Thang điểm 1–5. \"Gửi\" khóa đánh giá; \"Lưu nháp\" sửa tiếp được.", "Lưu nháp", 2);
        this.v = v;
        this.ky = ky;
        for (int i = 0; i < DanhGiaService.TIEU_CHI.size(); i++) {
            int val = cu.size() > i ? cu.get(i).diem() : 3;
            JSpinner s = new JSpinner(new SpinnerNumberModel(val, 1, 5, 1));
            s.addChangeListener(e -> tinh());
            diem.add(s);
            form.add("diem", DanhGiaService.TIEU_CHI.get(i), s);
        }
        tb.setFont(Theme.H2);
        form.add(null, "Điểm trung bình", tb);
        if (v.dg() != null && v.dg().nhanXet() != null) nhanXet.setText(v.dg().nhanXet());
        form.add("nhanXet", "Nhận xét", Fields.scroll(nhanXet), 2);
        form.full(Btn.success("Gửi đánh giá (khóa)", e -> { gui = true; submit(); }));
        tinh();
    }

    private void tinh() {
        double s = diem.stream().mapToInt(x -> (Integer) x.getValue()).average().orElse(0);
        tb.setText(String.format("%.2f", s));
    }

    @Override protected Supplier<String> prepare() {
        List<Integer> d = diem.stream().map(x -> (Integer) x.getValue()).toList();
        String nx = nhanXet.getText();
        boolean g = gui;
        gui = false;
        return () -> new DanhGiaService().luu(v.nv().maNhanVien(), ky, d, nx, g);
    }

    @Override protected String okMessage(String ma) { return "Đã lưu đánh giá " + ma + "."; }
}
