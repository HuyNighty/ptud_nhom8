package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.HangMucForm;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.KeHoachService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.BenhNhanPicker;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.NhanVienCombo;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** UC12 Lập kế hoạch điều trị nhiều hạng mục; chi phí dự kiến tính từ bảng giá. */
public class KeHoachDialog extends FormDialog<String> {
    private final BenhNhanPicker bn = new BenhNhanPicker();
    private final String maBnCoDinh, maPk;
    private final HintField mucTieu = Fields.text("VD: Phục hồi răng 36, 37");
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— chọn bác sĩ —", true);
    private final List<DichVu> dv = new KeHoachService().dichVuDieuTri();
    private final Map<String, DichVu> byMa = dv.stream().collect(Collectors.toMap(DichVu::maDichVu, d -> d));
    private final JComboBox<DichVu> chon = Fields.combo(dv, d -> d.tenDichVu() + " · " + Fmt.money(d.donGia()), "— dịch vụ —");
    private final HintField rang = Fields.text("Răng"), ngay = Fields.date(Now.today().plusDays(7));
    private final List<HangMucForm> items = new ArrayList<>();
    private final JLabel tong = new JLabel("Chi phí dự kiến: 0 ₫");
    private final DataTable<HangMucForm> table = new DataTable<HangMucForm>()
            .col("Dịch vụ", 200, h -> byMa.get(h.maDichVu()).tenDichVu()).col("Răng", 70, HangMucForm::viTriRang).col("Ngày dự kiến", 100, HangMucForm::ngayDuKien)
            .col("Đơn giá", 100, h -> byMa.get(h.maDichVu()).donGia()).emptyText("Thêm ít nhất một hạng mục.");

    public KeHoachDialog(Component p, String maBn, String tenBn, String maPk) {
        super(p, "Lập kế hoạch điều trị", maPk == null ? null : "Từ lượt khám " + maPk, "Lập kế hoạch", 2);
        this.maBnCoDinh = maBn;
        this.maPk = maPk;
        if (maBn == null) form.add("benhNhan", "Bệnh nhân *", bn, 2);
        else form.add(null, "Bệnh nhân", new JLabel(maBn + " · " + tenBn), 2);
        form.add("mucTieu", "Mục tiêu điều trị *", mucTieu).add("bacSi", "Bác sĩ lập / chịu trách nhiệm *", bs);
        rang.setColumns(5);
        chon.setPreferredSize(new Dimension(260, 30));
        Toolbar add = new Toolbar().with(chon, rang, ngay, Btn.secondary("Thêm hạng mục", e -> them()));
        table.setPreferredSize(new Dimension(640, 180));
        JPanel box = Layouts.stack(add, table);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.add(Btn.ghost("Xóa hạng mục đã chọn", e -> { items.remove(table.selected()); refresh(); }), BorderLayout.WEST);
        foot.add(tong, BorderLayout.EAST);
        box.add(foot, BorderLayout.SOUTH);
        form.add("hangMuc", "Hạng mục", box, 2);
        table.setRows(items);
    }

    private void them() {
        form.clearErrors();
        try {
            DichVu d = Fields.selected(chon);
            if (d == null) throw new BusinessException("hangMuc", "Chọn dịch vụ.");
            if (rang.getText().isBlank()) throw new BusinessException("hangMuc", "Nhập vị trí răng.");
            items.add(new HangMucForm(d.maDichVu(), rang.getText().trim(), Fmt.parseDate(ngay.getText(), "hangMuc", "Ngày dự kiến")));
            rang.setText("");
            refresh();
        } catch (BusinessException e) {
            form.showError("hangMuc", e.getMessage());
        }
    }

    private void refresh() {
        table.setRows(items);
        BigDecimal t = items.stream().map(h -> byMa.get(h.maDichVu()).donGia()).reduce(BigDecimal.ZERO, BigDecimal::add);
        tong.setText("Chi phí dự kiến: " + Fmt.money(t));
    }

    @Override protected Supplier<String> prepare() {
        String m = maBnCoDinh != null ? maBnCoDinh : bn.maBenhNhan();
        if (m == null) throw new BusinessException("benhNhan", "Chọn bệnh nhân.");
        String mt = mucTieu.getText(), b = bs.ma();
        List<HangMucForm> h = List.copyOf(items);
        return () -> new KeHoachService().lap(m, maPk, mt, h, b);
    }

    @Override protected String okMessage(String ma) { return "Đã lập kế hoạch " + ma + "."; }
}
