package com.quanlynhakhoa.examination;

import com.quanlynhakhoa.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A bounded, read-only view of PhieuKham created by the existing check-in flow. */
public final class ExamQueueDao {
    public static final int LIMIT = 200;

    // A doctor is scoped through the trusted account -> active employee link in SQL.
    // The desktop UI cannot supply an arbitrary doctor ID to widen this query.
    static final String QUEUE_SQL = """
            SELECT TOP (201)
                   p.maPhieuKham, p.maBenhNhan, b.hoTen AS tenBenhNhan,
                   p.maNhanVien AS maBacSi, n.hoTen AS tenBacSi,
                   p.maLichHen, p.thoiGianTiepNhan, p.trangThai
            FROM dbo.PhieuKham AS p
            JOIN dbo.BenhNhan AS b ON b.maBenhNhan = p.maBenhNhan
            JOIN dbo.NhanVien AS n ON n.maNhanVien = p.maNhanVien
            WHERE p.trangThai IN ('CHO_KHAM', 'DANG_KHAM')
              AND p.thoiGianTiepNhan >= ? AND p.thoiGianTiepNhan < ?
              AND (? = 1 OR EXISTS (
                  SELECT 1
                  FROM dbo.TaiKhoan AS a
                  JOIN dbo.NhanVien AS assigned ON assigned.maNhanVien = a.maNhanVien
                  WHERE a.maTaiKhoan = ?
                    AND a.vaiTro = 'DOCTOR'
                    AND a.trangThai = 'HOAT_DONG'
                    AND assigned.chucDanh = 'BS'
                    AND assigned.trangThai = 'DANG_LAM_VIEC'
                    AND a.maNhanVien = p.maNhanVien
              ))
            ORDER BY CASE WHEN p.trangThai = 'CHO_KHAM' THEN 0 ELSE 1 END,
                     p.thoiGianTiepNhan, p.maPhieuKham
            """;

    public record Result(List<ExamQueueRow> rows, boolean truncated) {
        public Result {
            rows = List.copyOf(rows);
        }
    }

    public Result listForDay(Connection connection, LocalDate day, Role role, String accountId)
            throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(role, "role");
        if (role != Role.DOCTOR && role != Role.SYSTEM_ADMIN) {
            throw new IllegalArgumentException("Only doctor or system administrator can read this queue");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Missing authenticated account");
        }
        List<ExamQueueRow> rows = new ArrayList<>();
        boolean truncated = false;
        try (PreparedStatement statement = connection.prepareStatement(QUEUE_SQL)) {
            statement.setQueryTimeout(12);
            statement.setTimestamp(1, Timestamp.valueOf(day.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(day.plusDays(1).atStartOfDay()));
            statement.setInt(3, role == Role.SYSTEM_ADMIN ? 1 : 0);
            statement.setString(4, accountId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (rows.size() == LIMIT) {
                        truncated = true;
                        break;
                    }
                    Timestamp arrivedAt = result.getTimestamp("thoiGianTiepNhan");
                    if (arrivedAt == null) throw new SQLException("Examination has no arrival time");
                    rows.add(new ExamQueueRow(
                            result.getString("maPhieuKham"),
                            result.getString("maBenhNhan"),
                            result.getNString("tenBenhNhan"),
                            result.getString("maBacSi"),
                            result.getNString("tenBacSi"),
                            result.getString("maLichHen"),
                            arrivedAt.toLocalDateTime(),
                            result.getString("trangThai")));
                }
            }
        }
        return new Result(rows, truncated);
    }
}
