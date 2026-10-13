package vn.n08.nhakhoa.controller;

import vn.n08.nhakhoa.service.AuthService;
import vn.n08.nhakhoa.util.AppLog;
import vn.n08.nhakhoa.view.screen.LoginFrame;
import vn.n08.nhakhoa.view.screen.MainFrame;

import javax.swing.SwingUtilities;

/** Điều phối vòng đời cửa sổ: đăng nhập → khung chính → đăng xuất → đăng nhập. */
public final class AppController {
    private static final AuthService AUTH = new AuthService();

    private AppController() {}

    public static void start() {
        SwingUtilities.invokeLater(() -> new LoginFrame(AUTH, AppController::onLogin).setVisible(true));
    }

    private static void onLogin(LoginFrame f) {
        f.dispose();
        MainFrame m = new MainFrame(() -> logout());
        m.setVisible(true);
        AppLog.info("Mở khung chính");
    }

    public static void logout() {
        try {
            AUTH.logout();
        } catch (RuntimeException e) {
            AppLog.error("Ghi nhật ký đăng xuất thất bại", e);
        }
        start();
    }

    public static AuthService auth() { return AUTH; }
}
