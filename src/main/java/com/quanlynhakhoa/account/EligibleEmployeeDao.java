package com.quanlynhakhoa.account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Read-only employee picker source. AccountCreateDao always revalidates on insert. */
public final class EligibleEmployeeDao {
    private static final int LIMIT = 200;
    static final String LIST_SQL = """
            SELECT TOP (201) n.maNhanVien, n.hoTen, n.chucDanh
            FROM dbo.NhanVien AS n
            WHERE n.trangThai = 'DANG_LAM_VIEC'
              AND n.chucDanh IN ('LT', 'BS', 'YT', 'QL', 'KHO')
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.TaiKhoan AS a
                  WHERE a.maNhanVien = n.maNhanVien
              )
            ORDER BY n.hoTen, n.maNhanVien
            """;

    public record Result(List<EligibleEmployee> employees, boolean truncated) {
        public Result {
            employees = List.copyOf(Objects.requireNonNull(employees, "employees"));
        }
    }

    public Result findEligible(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        List<EligibleEmployee> employees = new ArrayList<>();
        boolean truncated = false;
        try (PreparedStatement statement = connection.prepareStatement(LIST_SQL)) {
            statement.setQueryTimeout(12);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (employees.size() >= LIMIT) {
                        truncated = true;
                        break;
                    }
                    String title = rows.getString("chucDanh");
                    employees.add(new EligibleEmployee(
                            rows.getString("maNhanVien"),
                            rows.getNString("hoTen"),
                            title,
                            EligibleEmployee.roleForJob(title)));
                }
            }
        }
        return new Result(employees, truncated);
    }
}
