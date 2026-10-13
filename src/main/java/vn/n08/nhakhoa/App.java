package vn.n08.nhakhoa;

import vn.n08.nhakhoa.config.Database;
import vn.n08.nhakhoa.controller.AppController;
import vn.n08.nhakhoa.util.AppLog;
import vn.n08.nhakhoa.view.component.Theme;

/**
 * Điểm khởi động ứng dụng Quản lý phòng khám nha khoa (Java Swing + JDBC + SQL Server).
 * Tham số: --kiem-tra-ket-noi  chỉ thử kết nối CSDL theo config/db.properties rồi thoát.
 */
public final class App {
    private App() {}

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--kiem-tra-ket-noi")) {
            try {
                System.out.println("Cấu hình: " + Database.config().source());
                System.out.println("URL     : " + Database.config().safeUrl());
                System.out.println("KẾT NỐI THÀNH CÔNG: " + Database.ping());
                System.exit(0);
            } catch (RuntimeException e) {
                System.out.println("KẾT NỐI THẤT BẠI: " + e.getMessage());
                System.exit(1);
            }
        }
        AppLog.info("Khởi động ứng dụng");
        Theme.install();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> javax.swing.SwingUtilities.invokeLater(() ->
                vn.n08.nhakhoa.view.component.Dialogs.error(null, vn.n08.nhakhoa.controller.ErrorHandler.message(e))));
        AppController.start();
    }
}
