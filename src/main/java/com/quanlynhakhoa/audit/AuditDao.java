package com.quanlynhakhoa.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One bounded SELECT; never reads noiDung, passwords or session secrets. */
public final class AuditDao {
    public static final int PAGE_LIMIT = 200;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String SEARCH_SQL = """
            SELECT TOP (201) maNhatKy, thoiGian, loaiTacNhan, maTaiKhoan,
                   chucNang, hanhDong, ketQua, doiTuong, maDoiTuong
            FROM dbo.NhatKyThaoTac
            WHERE (? IS NULL OR thoiGian >= ?)
              AND (? IS NULL OR thoiGian < ?)
              AND (? IS NULL OR ketQua = ?)
              AND (? = N'' OR CHARINDEX(?, chucNang) > 0
                  OR CHARINDEX(?, hanhDong) > 0
                  OR CHARINDEX(?, maTaiKhoan) > 0
                  OR CHARINDEX(?, doiTuong) > 0
                  OR CHARINDEX(?, maDoiTuong) > 0)
            ORDER BY thoiGian DESC, maNhatKy DESC
            """;

    public record Result(List<AuditEntry> rows, boolean truncated) {
        public Result {
            rows = List.copyOf(rows);
        }
    }

    public Result search(Connection connection, AuditFilter filter) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(filter, "filter");
        LocalDateTime fromUtc = toUtcStart(filter.from());
        LocalDateTime endExclusiveUtc = filter.to() == null ? null : toUtcStart(filter.to().plusDays(1));
        try (PreparedStatement statement = connection.prepareStatement(SEARCH_SQL)) {
            statement.setQueryTimeout(12);
            bindTime(statement, 1, fromUtc);
            bindTime(statement, 2, fromUtc);
            bindTime(statement, 3, endExclusiveUtc);
            bindTime(statement, 4, endExclusiveUtc);
            statement.setString(5, filter.result());
            statement.setString(6, filter.result());
            statement.setNString(7, filter.keyword());
            for (int position = 8; position <= 12; position++) {
                statement.setNString(position, filter.keyword());
            }
            List<AuditEntry> rows = new ArrayList<>();
            boolean truncated = false;
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (rows.size() >= PAGE_LIMIT) {
                        truncated = true;
                        break;
                    }
                    Timestamp time = result.getTimestamp("thoiGian");
                    long id = result.getLong("maNhatKy");
                    rows.add(new AuditEntry(id, Objects.requireNonNull(time, "thoiGian").toLocalDateTime(),
                            result.getString("loaiTacNhan"), result.getString("maTaiKhoan"),
                            result.getNString("chucNang"), result.getNString("hanhDong"),
                            result.getString("ketQua"), result.getString("doiTuong"),
                            result.getString("maDoiTuong")));
                }
            }
            return new Result(rows, truncated);
        }
    }

    private static LocalDateTime toUtcStart(LocalDate day) {
        return day == null ? null : LocalDateTime.ofInstant(day.atStartOfDay(VIETNAM).toInstant(), ZoneOffset.UTC);
    }

    private static void bindTime(PreparedStatement statement, int position, LocalDateTime time)
            throws SQLException {
        if (time == null) statement.setNull(position, java.sql.Types.TIMESTAMP);
        else statement.setTimestamp(position, Timestamp.valueOf(time));
    }
}
