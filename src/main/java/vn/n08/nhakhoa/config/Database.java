package vn.n08.nhakhoa.config;

import vn.n08.nhakhoa.util.DataException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Quản lý kết nối JDBC tới SQL Server. Mỗi lần gọi trả về một Connection mới (đóng bằng try-with-resources). */
public final class Database {
    private static DbConfig config;

    private Database() {}

    public static synchronized void init(DbConfig cfg) {
        config = cfg;
    }

    public static synchronized DbConfig config() {
        if (config == null) config = DbConfig.load();
        return config;
    }

    public static Connection open() {
        DbConfig c = config();
        try {
            if (c.user().isEmpty()) return DriverManager.getConnection(c.url());
            return DriverManager.getConnection(c.url(), c.user(), c.password());
        } catch (SQLException e) {
            throw new DataException(friendly(e, c), e);
        }
    }

    /** Thử kết nối và trả về tên/phiên bản hệ quản trị — dùng cho màn hình đăng nhập. */
    public static String ping() {
        try (Connection con = open()) {
            var md = con.getMetaData();
            return md.getDatabaseProductName() + " " + md.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new DataException("Kết nối được nhưng không đọc được thông tin máy chủ: " + e.getMessage(), e);
        }
    }

    static String friendly(SQLException e, DbConfig c) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("No suitable driver")) {
            return "Thiếu driver JDBC của SQL Server (mssql-jdbc). Chạy bằng Maven hoặc đặt file mssql-jdbc-*.jar vào thư mục lib/.";
        }
        if (msg.toLowerCase().contains("login failed")) {
            return "SQL Server từ chối đăng nhập. Kiểm tra db.user / db.password trong " + c.source() + ".";
        }
        return "Không kết nối được cơ sở dữ liệu (" + c.safeUrl() + "). Kiểm tra SQL Server đang chạy, TCP/IP đã bật và cổng đúng. Chi tiết: " + msg;
    }
}
