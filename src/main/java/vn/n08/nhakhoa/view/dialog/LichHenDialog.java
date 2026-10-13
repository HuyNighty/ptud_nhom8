package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.LichHenForm;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.LichHenService;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.BenhNhanPicker;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.NhanVienCombo;

import javax.swing.JComboBox;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

/** UC03 Tạo lịch hẹn / UC14 tạo lịch tái khám (gắn kế hoạch). Chỉ hiện khung giờ còn trống của bác sĩ. */
public class LichHenDialog extends FormDialog<String> {
    private final BenhNhanPicker bn = new BenhNhanPicker();
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "— chọn bác sĩ —", false);
    private final HintField ngay = Fields.date(Now.today().plusDays(1));
    private final JComboBox<LocalTime> gio = Fields.combo(List.of(), Fmt::time, "— chọn giờ —");
    private final HintField lyDo = Fields.text("VD: Khám tổng quát");
    private final String maKeHoach;

    public LichHenDialog(Component p, String presetBn, String presetBs, String maKeHoach, String lyDoMacDinh) {
        super(p, maKeHoach == null ? "Tạo lịch hẹn" : "Tạo lịch tái khám — kế hoạch " + maKeHoach,
                "Danh sách giờ chỉ gồm khung giờ còn trống của bác sĩ đã chọn.", "Tạo lịch hẹn", 2);
        this.maKeHoach = maKeHoach;
        form.add("benhNhan", "Bệnh nhân *", bn, 2).add("bacSi", "Bác sĩ *", bs).add("ngay", "Ngày hẹn *", ngay)
                .add("thoiGian", "Khung giờ trống *", gio).add("lyDoHen", "Lý do hẹn", lyDo);
        if (presetBn != null) bn.preset(presetBn);
        if (presetBs != null) bs.selectMa(presetBs);
        if (lyDoMacDinh != null) lyDo.setText(lyDoMacDinh);
        bs.addActionListener(e -> loadSlots());
        ngay.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { loadSlots(); } });
        ngay.addActionListener(e -> loadSlots());
    }

    private void loadSlots() {
        String m = bs.ma();
        LocalDate d;
        try {
            d = Fmt.parseDate(txt(ngay), "ngay", "Ngày hẹn");
        } catch (BusinessException e) {
            return;
        }
        form.clearErrors();
        Async.run(this, () -> new LichHenService().khungGioTrong(m, d, null), r -> {
            Fields.setItems(gio, r, true);
            if (m != null && r.isEmpty()) form.showError("thoiGian", "Bác sĩ không còn khung giờ trống ngày này.");
        });
    }

    @Override protected Supplier<String> prepare() {
        LocalDate d = Fmt.parseDate(txt(ngay), "ngay", "Ngày hẹn");
        LocalTime t = Fields.selected(gio);
        if (t == null) throw new BusinessException("thoiGian", "Chọn khung giờ trống.");
        LichHenForm f = new LichHenForm(bn.maBenhNhan(), bs.ma(), d.atTime(t), txt(lyDo), maKeHoach);
        return () -> new LichHenService().tao(f);
    }

    @Override protected String okMessage(String ma) { return "Đã tạo lịch hẹn " + ma + "."; }
}
