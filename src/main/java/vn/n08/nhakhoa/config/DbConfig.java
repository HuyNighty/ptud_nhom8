package vn.n08.nhakhoa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Đọc cấu hình kết nối từ file properties.
 * Thứ tự tìm: -Ddb.config=đường_dẫn  →  ./config/db.properties.
 * Không có mật khẩu nào được ghi cứng trong mã nguồn.
 */
public final class DbConfig {
    public static final String DEFAULT_PATH = "config/db.properties";

    private final String url;
    private final String user;
    private final String password;
    private final String source;

    private DbConfig(String url, String user, String password, String source) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.source = source;
    }

    public static DbConfig load() {
        String p = System.getProperty("db.config", DEFAULT_PATH);
        Path path = Path.of(p);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Không tìm thấy file cấu hình " + path.toAbsolutePath()
                    + ". Sao chép config/db.properties.example thành config/db.properties rồi nhập thông tin SQL Server.");
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path);
             InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được file cấu hình " + path + ": " + e.getMessage(), e);
        }
        String url = props.getProperty("db.url", "").trim();
        if (url.isEmpty()) {
            throw new IllegalStateException("File " + path + " chưa có khóa db.url.");
        }
        return new DbConfig(url, props.getProperty("db.user", "").trim(), props.getProperty("db.password", ""), path.toAbsolutePath().toString());
    }

    /** Tạo cấu hình trực tiếp (dùng cho kiểm thử tự động với CSDL thay thế). */
    public static DbConfig of(String url, String user, String password) {
        return new DbConfig(url, user == null ? "" : user, password == null ? "" : password, "(cấu hình trong mã kiểm thử)");
    }

    public String url() { return url; }
    public String user() { return user; }
    public String password() { return password; }
    public String source() { return source; }

    /** URL đã che mật khẩu (nếu có) để hiển thị trong thông báo lỗi. */
    public String safeUrl() {
        return url.replaceAll("(?i)(password=)[^;]*", "$1***");
    }
}
