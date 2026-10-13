package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.TaiKhoanView;
import vn.n08.nhakhoa.model.TrangThaiTaiKhoan;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.TaiKhoanService;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.FormDialog;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.TaiKhoanDialog;

import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** UC21 Quản lý tài khoản và phân quyền: tạo, khóa (BI_KHOA, mở lại được), vô hiệu (VO_HIEU, vĩnh viễn), đặt lại mật khẩu, đổi vai trò. */
public class TaiKhoanPage extends Page {
    private final TaiKhoanService svc = new TaiKhoanService();
    private final HintField q = Fields.text("Tên đăng nhập, họ tên, mã NV");
    private final JComboBox<VaiTro> vt = Fields.combo(List.of(VaiTro.values()), VaiTro::label, "Tất cả vai trò");
    private final JComboBox<TrangThaiTaiKhoan> tt = Fields.combo(List.of(TrangThaiTaiKhoan.values()), TrangThaiTaiKhoan::label, "Tất cả trạng thái");
    private final DataTable<TaiKhoanView> table = new DataTable<TaiKhoanView>()
            .col("Mã TK", 70, v -> v.tk().maTaiKhoan()).col("Tên đăng nhập", 120, v -> v.tk().tenDangNhap()).col("Nhân viên", 170, v -> v.hoTen() + " (" + v.tk().maNhanVien() + ")")
            .col("Vai trò", 120, v -> v.tk().vaiTro()).col("Trạng thái", 100, v -> v.tk().trangThai()).col("Sai MK", 55, v -> v.tk().soLanSai())
            .col("Đăng nhập cuối", 120, v -> v.tk().lanDangNhapCuoi()).col("Lý do khóa / vô hiệu", 240, v -> v.tk().lyDoKhoa())
            .col("NV", 100, TaiKhoanView::trangThaiNhanVien).emptyText("Không có tài khoản.");
    private final Btn khoa = Btn.secondary("Khóa", e -> khoa());
    private final Btn mo = Btn.secondary("Mở khóa", e -> act("Đã mở khóa.", m -> () -> { svc.moKhoa(m); return true; }));
    private final Btn voHieu = Btn.danger("Vô hiệu", e -> voHieu());
    private final Btn reset = Btn.secondary("Đặt lại mật khẩu", e -> reset());
    private final Btn vaiTro = Btn.secondary("Đổi vai trò", e -> doiVaiTro());

    public TaiKhoanPage() {
        super("Tài khoản & phân quyền", "UC21 — Bị khóa: tạm thời, mở lại được · Vô hiệu: vĩnh viễn, không xóa để giữ lịch sử");
        header.action(Btn.primary("Cấp tài khoản", e -> { if (new TaiKhoanDialog(this).open() != null) reload(); }));
        q.addActionListener(e -> reload());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tài khoản", Layouts.pad(Card.titled("Danh sách tài khoản", null, Layouts.stack(
                new Toolbar().with(q, vt, tt, Btn.secondary("Lọc", e -> reload()), khoa, mo, voHieu, reset, vaiTro), table)), 10, 0, 0, 0));
        DataTable<Quyen> mt = new DataTable<>();
        mt.col("Quyền (kiểm tra ở tầng Service)", 220, Quyen::name);
        for (VaiTro v : VaiTro.values()) mt.col(v.label(), 95, q -> q.allows(v) ? "✓" : "");
        mt.setRows(Arrays.asList(Quyen.values()));
        tabs.addTab("Ma trận quyền", Layouts.pad(Card.titled("Ma trận quyền theo vai trò", "Admin có mọi quyền nhưng thao tác lâm sàng phải chọn bác sĩ chịu trách nhiệm; không tự duyệt yêu cầu của mình", mt), 10, 0, 0, 0));
        body(tabs);
        table.onSelect(this::buttons);
        buttons(null);
    }

    private void buttons(TaiKhoanView v) {
        boolean self = v != null && v.tk().maTaiKhoan().equals(Session.user().maTaiKhoan());
        TrangThaiTaiKhoan s = v == null ? null : v.tk().trangThai();
        khoa.setEnabled(s == TrangThaiTaiKhoan.HOAT_DONG && !self);
        mo.setEnabled(s == TrangThaiTaiKhoan.BI_KHOA);
        voHieu.setEnabled(s != null && s != TrangThaiTaiKhoan.VO_HIEU && !self);
        reset.setEnabled(s != null && s != TrangThaiTaiKhoan.VO_HIEU);
        vaiTro.setEnabled(s != null && s != TrangThaiTaiKhoan.VO_HIEU && !self);
    }

    @Override public void reload() {
        String s = q.getText();
        VaiTro v = Fields.selected(vt);
        TrangThaiTaiKhoan t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSach(s, v, t), r -> buttons(table.selected()));
    }

    private void act(String msg, java.util.function.Function<String, Supplier<Boolean>> f) {
        TaiKhoanView v = table.selected();
        Async.run(this, f.apply(v.tk().maTaiKhoan()), r -> { Toast.show(this, msg); reload(); });
    }

    private void khoa() {
        TaiKhoanView v = table.selected();
        String ly = Dialogs.reason(this, "Khóa " + v.tk().tenDangNhap(), "Khóa tạm thời (mở lại được). Nhập lý do.", "Khóa", false);
        if (ly != null) act("Đã khóa tài khoản.", m -> () -> { svc.khoa(m, ly); return true; });
    }

    private void voHieu() {
        TaiKhoanView v = table.selected();
        String ly = Dialogs.reason(this, "Vô hiệu " + v.tk().tenDangNhap(), "VÔ HIỆU VĨNH VIỄN — không khôi phục được (tài khoản không bị xóa để giữ lịch sử). Nhập lý do.", "Vô hiệu", true);
        if (ly != null) act("Đã vô hiệu tài khoản.", m -> () -> { svc.voHieu(m, ly); return true; });
    }

    private void reset() {
        TaiKhoanView v = table.selected();
        String r = new MatKhauTamDialog(this, v).open();
        if (r != null) reload();
    }

    private void doiVaiTro() {
        TaiKhoanView v = table.selected();
        String r = new VaiTroDialog(this, v).open();
        if (r != null) reload();
    }

    /** Nhập mật khẩu tạm (ẩn ký tự). */
    private static class MatKhauTamDialog extends FormDialog<String> {
        private final TaiKhoanView v;
        private final JPasswordField a = new JPasswordField(), b = new JPasswordField();

        MatKhauTamDialog(Component p, TaiKhoanView v) {
            super(p, "Đặt lại mật khẩu — " + v.tk().tenDangNhap(), "Mật khẩu tạm tối thiểu 8 ký tự, có chữ và số; số lần sai được đặt về 0.", "Đặt lại", 1);
            this.v = v;
            form.add("matKhau", "Mật khẩu tạm *", a).add("nhapLai", "Nhập lại *", b);
        }

        @Override protected Supplier<String> prepare() {
            String x = new String(a.getPassword()), y = new String(b.getPassword());
            if (!x.equals(y)) throw new vn.n08.nhakhoa.util.BusinessException("nhapLai", "Mật khẩu nhập lại không khớp.");
            return () -> { new TaiKhoanService().datLaiMatKhau(v.tk().maTaiKhoan(), x); return v.tk().maTaiKhoan(); };
        }

        @Override protected String okMessage(String r) { return "Đã đặt lại mật khẩu."; }
    }

    private static class VaiTroDialog extends FormDialog<String> {
        private final TaiKhoanView v;
        private final JComboBox<VaiTro> c = Fields.combo(List.of(VaiTro.values()), VaiTro::label, null);

        VaiTroDialog(Component p, TaiKhoanView v) {
            super(p, "Đổi vai trò — " + v.tk().tenDangNhap(), "Chức danh nhân viên: " + v.chucDanh().label(), "Đổi vai trò", 1);
            this.v = v;
            c.setSelectedItem(v.tk().vaiTro());
            form.add("vaiTro", "Vai trò mới *", c);
        }

        @Override protected Supplier<String> prepare() {
            VaiTro x = Fields.selected(c);
            return () -> { new TaiKhoanService().doiVaiTro(v.tk().maTaiKhoan(), x); return v.tk().maTaiKhoan(); };
        }

        @Override protected String okMessage(String r) { return "Đã đổi vai trò."; }
    }
}
