package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.model.NhanVien;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.NhanVienService;
import vn.n08.nhakhoa.util.Session;

import javax.swing.JComboBox;
import java.util.List;

/**
 * Chọn nhân viên đang làm việc theo chức danh. Với chế độ "bác sĩ chịu trách nhiệm":
 * tài khoản bác sĩ được chọn sẵn chính mình và khóa ô (Service cũng chặn ghi dưới tên người khác).
 */
public class NhanVienCombo extends JComboBox<NhanVien> {
    private String preset;

    public NhanVienCombo(VaiTro chucDanh, String nullLabel, boolean bacSiPhuTrach) {
        super();
        JComboBox<NhanVien> tmp = Fields.combo(List.of(), nv -> nv.hoTen() + " (" + nv.maNhanVien() + ")", nullLabel);
        setRenderer(tmp.getRenderer());
        setFont(Theme.BODY);
        if (nullLabel != null) addItem(null);
        Session.User u = Session.user();
        if (bacSiPhuTrach && u.vaiTro() == VaiTro.BS) preset = u.maNhanVien();
        Async.run(this, () -> new NhanVienService().dangLamViec(chucDanh), list -> {
            for (NhanVien n : list) addItem(n);
            if (preset != null) selectMa(preset);
            if (bacSiPhuTrach && u.vaiTro() == VaiTro.BS) setEnabled(false);
        });
    }

    public void selectMa(String ma) {
        preset = ma;
        for (int i = 0; i < getItemCount(); i++) {
            NhanVien n = getItemAt(i);
            if (n != null && n.maNhanVien().equals(ma)) { setSelectedIndex(i); return; }
        }
    }

    public String ma() {
        NhanVien n = (NhanVien) getSelectedItem();
        return n == null ? null : n.maNhanVien();
    }
}
