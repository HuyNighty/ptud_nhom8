package com.quanlynhakhoa.workforce;

import com.quanlynhakhoa.model.EmployeeStatus;
import com.quanlynhakhoa.model.Role;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** SELECT-only data access. No employee/account writes or password fields. */
public final class EmployeeDirectoryDao {
    private static final int PAGE_LIMIT = 200;
    private static final String SEARCH_SQL = """
            SELECT TOP (201)
                   n.maNhanVien, n.hoTen, n.chucDanh, n.trangThai, n.ngayVaoLam,
                   a.maTaiKhoan, a.vaiTro
            FROM dbo.NhanVien AS n
            LEFT JOIN dbo.TaiKhoan AS a ON a.maNhanVien = n.maNhanVien
            WHERE (? = N''
                   OR CHARINDEX(?, n.hoTen) > 0
                   OR CHARINDEX(?, n.maNhanVien) > 0)
              AND (? IS NULL OR n.trangThai = ?)
            ORDER BY n.maNhanVien
            """;

    public record Result(List<EmployeeDirectoryRow> rows, boolean truncated) {
        public Result {
            rows = List.copyOf(rows);
        }
    }

    public Result search(Connection connection, String keyword, EmployeeStatus status) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(keyword, "keyword");
        try (PreparedStatement statement = connection.prepareStatement(SEARCH_SQL)) {
            statement.setQueryTimeout(12);
            statement.setNString(1, keyword);
            statement.setNString(2, keyword);
            statement.setNString(3, keyword);
            String statusName = status == null ? null : status.name();
            statement.setString(4, statusName);
            statement.setString(5, statusName);
            List<EmployeeDirectoryRow> rows = new ArrayList<>();
            boolean truncated = false;
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (rows.size() == PAGE_LIMIT) {
                        truncated = true;
                        break;
                    }
                    Date joined = result.getDate("ngayVaoLam");
                    String accountRole = result.getString("vaiTro");
                    rows.add(new EmployeeDirectoryRow(
                            result.getString("maNhanVien"),
                            result.getNString("hoTen"),
                            result.getString("chucDanh"),
                            EmployeeStatus.valueOf(result.getString("trangThai")),
                            joined == null ? null : joined.toLocalDate(),
                            result.getString("maTaiKhoan"),
                            accountRole == null ? null : Role.valueOf(accountRole)));
                }
            }
            return new Result(rows, truncated);
        }
    }
}
