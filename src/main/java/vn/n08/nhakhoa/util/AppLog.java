package vn.n08.nhakhoa.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Nhật ký kỹ thuật của ứng dụng (lỗi ngoài dự kiến, kết nối) ghi ra logs/n08-nhakhoa.log.
 * Khác với NhatKyThaoTac (nhật ký nghiệp vụ trong CSDL). Không ghi mật khẩu.
 */
public final class AppLog {
    private static final Logger LOG = Logger.getLogger("vn.n08.nhakhoa");
    private static boolean ready;

    private AppLog() {}

    private static synchronized void init() {
        if (ready) return;
        ready = true;
        try {
            Files.createDirectories(Path.of("logs"));
            FileHandler fh = new FileHandler("logs/n08-nhakhoa.log", 2_000_000, 3, true);
            fh.setEncoding("UTF-8");
            fh.setFormatter(new SimpleFormatter());
            LOG.addHandler(fh);
        } catch (IOException | SecurityException e) {
            LOG.warning("Không mở được file log, chỉ ghi ra console: " + e.getMessage());
        }
    }

    public static void info(String msg) {
        init();
        LOG.info(msg);
    }

    public static void error(String msg, Throwable t) {
        init();
        LOG.log(Level.SEVERE, msg, t);
    }
}
