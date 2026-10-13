package vn.n08.nhakhoa.util;

import vn.n08.nhakhoa.config.Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý transaction cho tầng Service.
 * - {@link #read}: một kết nối auto-commit cho truy vấn đọc.
 * - {@link #write}: tắt auto-commit, commit khi thành công, rollback khi có lỗi bất kỳ.
 */
public final class Tx {
    private Tx() {}

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    @FunctionalInterface
    public interface VoidWork {
        void run(Connection c) throws SQLException;
    }

    public static <T> T read(Work<T> work) {
        try (Connection c = Database.open()) {
            return work.run(c);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static <T> T write(Work<T> work) {
        try (Connection c = Database.open()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T r = work.run(c);
                c.commit();
                return r;
            } catch (SQLException | RuntimeException e) {
                safeRollback(c);
                if (e instanceof SQLException se) throw translate(se);
                throw (RuntimeException) e;
            } finally {
                try { c.setAutoCommit(old); } catch (SQLException ignore) { /* kết nối sắp đóng */ }
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    public static void run(VoidWork work) {
        write(c -> { work.run(c); return null; });
    }

    private static void safeRollback(Connection c) {
        try { c.rollback(); } catch (SQLException ignore) { /* giữ lỗi gốc */ }
    }

    /** Chuyển lỗi SQL thành thông điệp tiếng Việt dễ hiểu. */
    public static RuntimeException translate(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (state.startsWith("23") || msg.contains("CHECK constraint") || msg.contains("UNIQUE") || msg.contains("FOREIGN KEY")
                || msg.toLowerCase().contains("duplicate key")) {
            return new BusinessException("Dữ liệu vi phạm ràng buộc của cơ sở dữ liệu (" + constraintName(msg)
                    + "). Thao tác đã được hủy, không có thay đổi nào được lưu.");
        }
        if (state.startsWith("08")) {
            return new DataException("Mất kết nối tới cơ sở dữ liệu. Kiểm tra SQL Server rồi thử lại.", e);
        }
        if (state.startsWith("40")) {
            return new DataException("Thao tác bị xung đột với người dùng khác. Vui lòng thử lại.", e);
        }
        return new DataException("Lỗi cơ sở dữ liệu: " + msg, e);
    }

    private static String constraintName(String msg) {
        var m = java.util.regex.Pattern.compile("(?i)['\"]?((?:CK|UQ|UX|FK|PK)_[A-Za-z0-9_]+)['\"]?").matcher(msg);
        return m.find() ? m.group(1) : "ràng buộc dữ liệu";
    }
}
