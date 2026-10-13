package vn.n08.nhakhoa.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tiện ích JDBC tự viết (không phải framework): luôn dùng PreparedStatement,
 * tự đóng Statement/ResultSet, chuyển đổi java.time ↔ java.sql.
 * Connection do tầng Service (Tx) mở và đóng.
 */
public final class Jdbc {
    private Jdbc() {}

    /** Ghi lại câu SQL đã chạy (mỗi câu một lần) khi bật -Dn08.sqltrace=đường_dẫn_file — dùng để rà soát cú pháp T-SQL. */
    private static final String TRACE = System.getProperty("n08.sqltrace");
    private static final java.util.Set<String> SEEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static PreparedStatement prepare(Connection c, String sql, int keys) throws SQLException {
        if (TRACE != null && SEEN.add(sql)) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(TRACE), sql.replace('\n', ' ') + "\n", java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (java.io.IOException ignore) {
                // chỉ phục vụ rà soát
            }
        }
        return keys == 0 ? c.prepareStatement(sql) : c.prepareStatement(sql, keys);
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, 0)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    public static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, 0)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
            }
        }
    }

    public static long count(Connection c, String sql, Object... params) throws SQLException {
        return one(c, sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    public static BigDecimal sum(Connection c, String sql, Object... params) throws SQLException {
        BigDecimal v = one(c, sql, rs -> rs.getBigDecimal(1), params).orElse(null);
        return v == null ? BigDecimal.ZERO : v;
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, 0)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Thực thi INSERT trên bảng có cột IDENTITY và trả về khóa sinh ra. */
    public static long insertReturningKey(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("Không lấy được khóa tự sinh");
        }
    }

    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        ParameterMetaData md = null;
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                int type = Types.VARCHAR;
                try {
                    if (md == null) md = ps.getParameterMetaData();
                    type = md.getParameterType(idx);
                } catch (SQLException ignore) {
                    // một số driver không hỗ trợ metadata cho tham số; dùng VARCHAR
                }
                ps.setNull(idx, type);
            } else if (p instanceof LocalDate d) {
                ps.setDate(idx, Date.valueOf(d));
            } else if (p instanceof LocalDateTime dt) {
                ps.setTimestamp(idx, Timestamp.valueOf(dt.withNano(0)));
            } else if (p instanceof LocalTime t) {
                ps.setTime(idx, Time.valueOf(t));
            } else if (p instanceof Boolean b) {
                ps.setBoolean(idx, b);
            } else if (p instanceof Enum<?> e) {
                ps.setString(idx, e.name());
            } else if (p instanceof BigDecimal bd) {
                ps.setBigDecimal(idx, bd);
            } else if (p instanceof Integer n) {
                ps.setInt(idx, n);
            } else if (p instanceof Long n) {
                ps.setLong(idx, n);
            } else {
                ps.setString(idx, p.toString());
            }
        }
    }

    // ---------- đọc cột ----------
    public static LocalDate date(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    public static LocalTime time(ResultSet rs, String col) throws SQLException {
        Time t = rs.getTime(col);
        return t == null ? null : t.toLocalTime();
    }

    public static BigDecimal money(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v == null ? BigDecimal.ZERO : v;
    }

    public static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** Ký tự đại diện cho LIKE, không phân biệt hoa thường: dùng với LOWER(cột) LIKE ?. */
    public static String like(String q) {
        String s = q == null ? "" : q.trim().toLowerCase();
        s = s.replace("\\", "").replace("%", "").replace("_", "");
        return "%" + s + "%";
    }
}
