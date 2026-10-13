package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.DongHoaDonForm;
import vn.n08.nhakhoa.model.DichVu;
import vn.n08.nhakhoa.model.Thuoc;
import vn.n08.nhakhoa.model.VatTu;
import vn.n08.nhakhoa.service.HoaDonService;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.BenhNhanPicker;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** UC04 Lập hóa đơn: dòng dịch vụ (giá theo bảng giá) và thuốc / vật tư (nhập đơn giá). Tổng tiền tính, không lưu. */
public class HoaDonDialog extends FormDialog<String> {
    private final BenhNhanPicker bn = new BenhNhanPicker();
    private final String maBn, maPk;
    private final Map<String, String> ten = new HashMap<>();
    private final Map<String, BigDecimal> gia = new HashMap<>();
    private final List<DichVu> dv;
    private final List<Thuoc> thuoc;
    private final List<VatTu> vatTu;
    private final JComboBox<DongHoaDonForm.Loai> loai = Fields.combo(List.of(DongHoaDonForm.Loai.values()), DongHoaDonForm.Loai::toString, null);
    private final JComboBox<Object> muc = new JComboBox<>();
    private final HintField sl = Fields.text("SL"), donGia = Fields.money(null);
    private final List<DongHoaDonForm> dong = new ArrayList<>();
    private final JLabel tong = new JLabel("Tổng: 0 ₫");
    private final DataTable<DongHoaDonForm> table = new DataTable<DongHoaDonForm>()
            .col("Loại", 70, d -> d.loai().toString()).col("Mục", 220, d -> ten.get(d.ma())).col("SL", 40, DongHoaDonForm::soLuong)
            .col("Đơn giá", 100, this::giaDong).col("Thành tiền", 110, d -> giaDong(d).multiply(BigDecimal.valueOf(d.soLuong()))).emptyText("Chưa có dòng.");

    public HoaDonDialog(Component p, String maBn, String tenBn, String maPk, List<DongHoaDonForm> goiY) {
        super(p, "Lập hóa đơn", maPk == null ? "Hóa đơn không gắn lượt khám" : "Lượt khám " + maPk + " — dòng cận lâm sàng đã được gợi ý sẵn", "Lập hóa đơn", 1);
        this.maBn = maBn;
        this.maPk = maPk;
        HoaDonService s = new HoaDonService();
        KhoService k = new KhoService();
        dv = s.bangGia();
        thuoc = k.danhSachThuoc();
        vatTu = k.danhSachVatTu();
        dv.forEach(d -> { ten.put(d.maDichVu(), d.tenDichVu()); gia.put(d.maDichVu(), d.donGia()); });
        thuoc.forEach(t -> ten.put(t.maThuoc(), t.toString()));
        vatTu.forEach(v -> ten.put(v.maVatTu(), v.tenVatTu()));
        if (maBn == null) form.add("benhNhan", "Bệnh nhân *", bn);
        else form.add(null, "Bệnh nhân", new JLabel(maBn + " · " + tenBn));
        sl.setColumns(4);
        sl.setText("1");
        muc.setPreferredSize(new Dimension(260, 30));
        loai.addActionListener(e -> fillMuc());
        muc.addActionListener(e -> {
            Object o = muc.getSelectedItem();
            donGia.setEnabled(!(o instanceof DichVu));
            if (o instanceof DichVu d) donGia.setText(Fmt.money(d.donGia()).replace(" ₫", ""));
        });
        fillMuc();
        Toolbar add = new Toolbar().with(loai, muc, sl, donGia, Btn.secondary("Thêm dòng", e -> them()));
        table.setPreferredSize(new Dimension(660, 200));
        JPanel box = Layouts.stack(add, table);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.add(Btn.ghost("Xóa dòng đã chọn", e -> { dong.remove(table.selected()); refresh(); }), BorderLayout.WEST);
        tong.setFont(Theme.H2);
        foot.add(tong, BorderLayout.EAST);
        box.add(foot, BorderLayout.SOUTH);
        form.add("dong", "Các dòng hóa đơn", box);
        if (goiY != null) dong.addAll(goiY);
        refresh();
    }

    private BigDecimal giaDong(DongHoaDonForm d) {
        return d.loai() == DongHoaDonForm.Loai.DICH_VU ? gia.get(d.ma()) : d.donGia();
    }

    private void fillMuc() {
        muc.removeAllItems();
        DongHoaDonForm.Loai l = Fields.selected(loai);
        List<?> src = l == DongHoaDonForm.Loai.DICH_VU ? dv : l == DongHoaDonForm.Loai.THUOC ? thuoc : vatTu;
        for (Object o : src) muc.addItem(o);
    }

    private void them() {
        form.clearErrors();
        try {
            Object o = muc.getSelectedItem();
            if (o == null) throw new BusinessException("dong", "Chọn mục.");
            int n = Fmt.parseInt(sl.getText(), "dong", "Số lượng");
            DongHoaDonForm d;
            if (o instanceof DichVu x) d = new DongHoaDonForm(DongHoaDonForm.Loai.DICH_VU, x.maDichVu(), n, null);
            else {
                BigDecimal g = Fmt.parseMoney(donGia.getText(), "dong", "Đơn giá");
                d = o instanceof Thuoc t ? new DongHoaDonForm(DongHoaDonForm.Loai.THUOC, t.maThuoc(), n, g)
                        : new DongHoaDonForm(DongHoaDonForm.Loai.VAT_TU, ((VatTu) o).maVatTu(), n, g);
            }
            dong.add(d);
            refresh();
        } catch (BusinessException e) {
            form.showError("dong", e.getMessage());
        }
    }

    private void refresh() {
        table.setRows(dong);
        BigDecimal t = dong.stream().map(d -> giaDong(d).multiply(BigDecimal.valueOf(d.soLuong()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        tong.setText("Tổng: " + Fmt.money(t));
    }

    @Override protected Supplier<String> prepare() {
        String m = maBn != null ? maBn : bn.maBenhNhan();
        if (m == null) throw new BusinessException("benhNhan", "Chọn bệnh nhân.");
        List<DongHoaDonForm> d = List.copyOf(dong);
        return () -> new HoaDonService().lap(m, maPk, d);
    }

    @Override protected String okMessage(String ma) { return "Đã lập hóa đơn " + ma + "."; }
}
