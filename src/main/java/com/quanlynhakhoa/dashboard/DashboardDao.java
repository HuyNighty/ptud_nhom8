package com.quanlynhakhoa.dashboard;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

final class DashboardDao {
    private static final String PATIENTS = "SELECT COUNT_BIG(*) FROM dbo.BenhNhan";
    private static final String APPOINTMENTS = """
            SELECT COUNT_BIG(*) FROM dbo.LichHen
            WHERE thoiGianHen >= ? AND thoiGianHen < ?
              AND trangThai NOT IN ('DA_HUY', 'DA_DOI_LICH')
            """;
    private static final String IN_PROGRESS = """
            SELECT COUNT_BIG(*) FROM dbo.PhieuKham WHERE trangThai = 'DANG_KHAM'
            """;
    private static final String RECEIVED = """
            SELECT COALESCE(SUM(soTien), 0) FROM dbo.ThanhToan
            WHERE thoiGian >= ? AND thoiGian < ?
              AND trangThai = 'THANH_CONG'
            """;

    long countPatients(Connection connection) throws SQLException {
        return count(connection, PATIENTS);
    }

    long countAppointments(Connection connection, LocalDate day) throws SQLException {
        return countForDay(connection, APPOINTMENTS, day);
    }

    long countInProgress(Connection connection) throws SQLException {
        return count(connection, IN_PROGRESS);
    }

    BigDecimal collectedToday(Connection connection, LocalDate day) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(RECEIVED)) {
            statement.setQueryTimeout(10);
            bindDay(statement, day);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Missing dashboard total");
                BigDecimal amount = rows.getBigDecimal(1);
                return amount == null ? BigDecimal.ZERO : amount;
            }
        }
    }

    private long count(Connection connection, String query) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setQueryTimeout(10);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Missing dashboard count");
                return rows.getLong(1);
            }
        }
    }

    private long countForDay(Connection connection, String query, LocalDate day) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setQueryTimeout(10);
            bindDay(statement, day);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Missing dashboard daily count");
                return rows.getLong(1);
            }
        }
    }

    private void bindDay(PreparedStatement statement, LocalDate day) throws SQLException {
        statement.setObject(1, day.atStartOfDay());
        statement.setObject(2, day.plusDays(1).atStartOfDay());
    }
}
