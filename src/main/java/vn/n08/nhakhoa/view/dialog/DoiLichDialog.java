package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.LichHenService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

/** UC07 Thay đổi lịch hẹn (đổi giờ và/hoặc bác sĩ). Lịch cũ được giữ với trạng thái "Đã đổi lịch". */
public class DoiLichDialog extends FormDialog<String> {
    private final LichHenView lh;
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, null, false);
    private final HintField ngay;
    private final JComboBox<LocalTime> gio = Fields.combo(List.of(), Fmt::time, "— chọn giờ —");
    private final HintField lyDo = Fields.text("Lý do đổi lịch");

    public DoiLichDialog(Component p, LichHenView lh) {
        super(p, "Đổi lịch hẹn " + lh.lh().maLichHen(), "Hiện tại: " + Fmt.dateTime(lh.lh().thoiGianHen()) + " · " + lh.tenBacSi()
                + ". Lịch mới tạo bản ghi riêng trỏ về lịch gốc.", "Đổi lịch", 2);
        this.lh = lh;
        ngay = Fields.date(lh.lh().thoiGianHen().toLocalDate());
        JLabel who = new JLabel(lh.tenBenhNhan() + " · " + lh.soDienThoai());
        form.add(null, "Bệnh nhân", who, 2).add("bacSi", "Bác sĩ", bs).add("ngay", "Ngày mới *", ngay).add("thoiGian", "Khung giờ trống *", gio)
                .add("lyDo", "Lý do *", lyDo);
        bs.selectMa(lh.lh().maNhanVien());
        bs.addActionListener(e -> loadSlots());
        ngay.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { loadSlots(); } });
        loadSlots();
    }

    private void loadSlots() {
        String m = bs.ma() == null ? lh.lh().maNhanVien() : bs.ma();
        LocalDate d;
        try {
            d = Fmt.parseDate(txt(ngay), "ngay", "Ngày");
        } catch (BusinessException e) {
            return;
        }
        Async.run(this, () -> new LichHenService().khungGioTrong(m, d, lh.lh().maLichHen()), r -> Fields.setItems(gio, r, true));
    }

    @Override protected Supplier<String> prepare() {
        LocalDate d = Fmt.parseDate(txt(ngay), "ngay", "Ngày");
        LocalTime t = Fields.selected(gio);
        if (t == null) throw new BusinessException("thoiGian", "Chọn khung giờ.");
        String m = bs.ma() == null ? lh.lh().maNhanVien() : bs.ma();
        String ly = txt(lyDo);
        return () -> new LichHenService().doiLich(lh.lh().maLichHen(), m, d.atTime(t), ly);
    }

    @Override protected String okMessage(String ma) { return "Đã đổi lịch, lịch mới " + ma + "."; }
}
