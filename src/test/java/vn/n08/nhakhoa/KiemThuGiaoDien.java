package vn.n08.nhakhoa;

import vn.n08.nhakhoa.controller.Navigation;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.service.AuthService;
import vn.n08.nhakhoa.service.HoaDonService;
import vn.n08.nhakhoa.service.KhoService;
import vn.n08.nhakhoa.service.CapCuuService;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.dialog.*;
import vn.n08.nhakhoa.view.screen.LoginFrame;
import vn.n08.nhakhoa.view.screen.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Kiểm thử khói giao diện Swing (chạy dưới màn hình ảo Xvfb) trên CSDL THAY THẾ Derby:
 * mở từng màn hình theo vai trò, mở các hộp thoại chính, chụp ảnh, ghi nhận mọi ngoại lệ trên luồng giao diện.
 * Không kiểm tra thao tác chuột thực; nghiệp vụ được kiểm ở KiemThuTichHop.
 */
public final class KiemThuGiaoDien {
    private static final List<String> LOI = new ArrayList<>();
    private static File out;

    public static void main(String[] a) throws Exception {
        out = new File(a.length > 0 ? a[0] : "target/anh-giao-dien");
        out.mkdirs();
        TestDb.init("ui");
        SwingUtilities.invokeAndWait(Theme::install);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> { LOI.add(t.getName() + ": " + e); e.printStackTrace(); });
        // màn hình đăng nhập
        AtomicReference<LoginFrame> lf = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { lf.set(new LoginFrame(new AuthService(), f -> { })); lf.get().setVisible(true); });
        Thread.sleep(1500);
        shot(lf.get(), "00-dang-nhap");
        SwingUtilities.invokeAndWait(() -> lf.get().dispose());

        for (String u : List.of("admin", "lethian", "tranminh", "minhtam", "quockhanh", "vophuc")) {
            new AuthService().login(u, u.equals("admin") ? "Admin@123" : "Nhakhoa@123");
            AtomicReference<MainFrame> mf = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> { mf.set(new MainFrame(() -> { })); mf.get().setVisible(true); });
            Thread.sleep(1500);
            int n = 0;
            for (Navigation.Group g : Navigation.groups()) {
                for (Navigation.Item it : g.items()) {
                    if (!it.visible()) continue;
                    n++;
                    SwingUtilities.invokeAndWait(() -> mf.get().go(it.id()));
                    Thread.sleep(u.equals("admin") ? 1600 : 700);
                    if (u.equals("admin") || it.id().equals("dashboard")) shot(mf.get(), u + "-" + it.id());
                }
            }
            System.out.println(u + ": mở " + n + " màn hình");
            if (u.equals("admin")) hopThoai(mf.get());
            SwingUtilities.invokeAndWait(() -> { Session.logout(); mf.get().dispose(); });
        }
        System.out.println("Ảnh đã lưu tại " + out.getAbsolutePath());
        System.out.println(LOI.isEmpty() ? "KHÔNG CÓ NGOẠI LỆ TRÊN LUỒNG GIAO DIỆN" : "NGOẠI LỆ: " + LOI);
        System.exit(LOI.isEmpty() ? 0 : 1);
    }

    private static void hopThoai(MainFrame f) throws Exception {
        HoaDonView hd = new HoaDonService().danhSach(Now.today().minusDays(60), Now.today(), null, null).stream()
                .filter(h -> h.conNo().signum() > 0 && !h.coYeuCauHuyCho()).findFirst().orElseThrow();
        var cc = new CapCuuService().danhMuc();
        dialog("d01-tao-benh-nhan", () -> new BenhNhanDialog(f, null));
        dialog("d02-lich-hen", () -> new LichHenDialog(f, "0909123456", "NV0001", null, "Khám tổng quát"));
        dialog("d03-hoa-don", () -> new HoaDonDialog(f, null, null, null, null));
        dialog("d04-thanh-toan", () -> new ThanhToanDialog(f, hd));
        dialog("d05-xuat-kho", () -> new XuatKhoDialog(f));
        dialog("d06-nhap-kho", () -> new NhapKhoDialog(f));
        dialog("d07-dieu-chinh", () -> new DieuChinhDialog(f));
        dialog("d08-kiem-tra-cap-cuu", () -> new KiemTraCapCuuDialog(f, cc, 30));
        dialog("d09-ke-hoach", () -> new KeHoachDialog(f, "BN000001", "Nguyễn Thị Lan", null));
        dialog("d10-don-thuoc", () -> new DonThuocDialog(f, "PK000007", "Trần Thị Mai", "Penicillin", null));
        dialog("d11-phan-cong", () -> new PhanCongDialog(f, Now.today().plusDays(1), "CA1"));
        dialog("d12-ban-giao", () -> new BanGiaoDialog(f));
        dialog("d13-tai-khoan", () -> new TaiKhoanDialog(f));
        dialog("d14-nhan-vien", () -> new NhanVienDialog(f, null));
        dialog("d15-thuoc", () -> new ThuocVatTuDialog(f, null, null, true));
        dialog("d16-cap-cuu-muc", () -> new CapCuuMucDialog(f, null));
        dialog("d17-hoan-tra", () -> new HoanTraDialog(f, "PX000001"));
        dialog("d18-doi-mat-khau", () -> new DoiMatKhauDialog(f));
        AtomicReference<NghiViecDialog> nv = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> nv.set(new NghiViecDialog(f, "NV0004", "BS. Lê Thu")));
        SwingUtilities.invokeLater(() -> nv.get().setVisible(true));
        Thread.sleep(1500);
        shot(nv.get(), "d19-nghi-viec");
        SwingUtilities.invokeAndWait(() -> nv.get().dispose());
        System.out.println("Đã mở 19 hộp thoại");
    }

    private static void dialog(String name, Supplier<JDialog> s) throws Exception {
        AtomicReference<JDialog> d = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> d.set(s.get()));
        SwingUtilities.invokeLater(() -> {
            JDialog x = d.get();
            if (x instanceof vn.n08.nhakhoa.view.component.FormDialog<?> fd) fd.open(); else x.setVisible(true);
        });
        Thread.sleep(1200);
        shot(d.get(), name);
        SwingUtilities.invokeAndWait(() -> d.get().dispose());
        Thread.sleep(200);
    }

    private static void shot(Window w, String name) throws Exception {
        AtomicReference<BufferedImage> img = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Component c = w instanceof javax.swing.RootPaneContainer r ? r.getRootPane() : w;
            BufferedImage b = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
            c.printAll(b.getGraphics());
            img.set(b);
        });
        ImageIO.write(img.get(), "png", new File(out, name + ".png"));
    }
}
