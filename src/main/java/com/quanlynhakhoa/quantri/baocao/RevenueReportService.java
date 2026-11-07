package com.quanlynhakhoa.quantri.baocao;

import com.quanlynhakhoa.hethong.phanquyen.*;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** UC029: invoice-date revenue, receipt-date collections and receivables at period end. */
public final class RevenueReportService {
    public interface Connections { Connection open() throws SQLException; }
    private final DesktopSession session;
    private final Connections connections;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public RevenueReportService(DesktopSession session) { this(session, TeamDevConnection::open); }
    public RevenueReportService(DesktopSession session, Connections connections) {
        this.session = Objects.requireNonNull(session); this.connections = Objects.requireNonNull(connections);
    }
    public static void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.getYear() < 1900 || to.getYear() > 9998)
            throw new IllegalArgumentException("Chọn khoảng ngày hợp lệ; từ ngày không được sau đến ngày.");
    }
    private void require() { authorization.require(session, Permission.REVENUE_REPORT_READ); }
    public RevenueReport load(LocalDate from, LocalDate to, boolean monthly) throws SQLException {
        require(); validate(from, to);
        // Bound generated display rows without restricting the date interval itself.
        boolean byMonth = monthly || ChronoUnit.DAYS.between(from, to) > 730;
        try (Connection c = connections.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); c.setAutoCommit(false);
            try {
                require();
                TreeMap<LocalDate, RevenueReport.Row> rows = new TreeMap<>();
                LocalDate first = byMonth ? from.withDayOfMonth(1) : from;
                for (LocalDate day = first; !day.isAfter(to); day = byMonth ? day.plusMonths(1) : day.plusDays(1))
                    rows.put(day, new RevenueReport.Row(day, 0, BigDecimal.ZERO, BigDecimal.ZERO));
                // Separate aggregates avoid multiplying invoice lines by payment rows.
                String sql = """
                        WITH InvoiceTotals AS (
                          SELECT maHoaDon,SUM(CAST(soLuong AS decimal(10,0))*donGiaApDung) AS total
                          FROM dbo.ChiTietHoaDon GROUP BY maHoaDon
                        ), ActiveInvoices AS (
                          SELECT h.maHoaDon,h.ngayLap,COALESCE(d.total,0) AS total FROM dbo.HoaDon h
                          LEFT JOIN InvoiceTotals d ON d.maHoaDon=h.maHoaDon
                          WHERE h.ngayLap<? AND (h.trangThai<>'DA_HUY' OR EXISTS (
                            SELECT 1 FROM dbo.YeuCauHuyHoaDon y WHERE y.maHoaDon=h.maHoaDon
                            AND y.trangThai='DA_DUYET' AND y.thoiGianDuyet>=?))
                        ), ValidPayments AS (
                          SELECT maHoaDon,thoiGian,soTien FROM dbo.DongTienHoaDon
                          WHERE thoiGian<? AND ? IS NOT NULL
                        )
                        SELECT 'REVENUE' AS kind,CAST(ngayLap AS date) AS day,COUNT(*) AS invoices,SUM(total) AS amount
                          FROM ActiveInvoices WHERE ngayLap>=? GROUP BY CAST(ngayLap AS date)
                        UNION ALL
                        SELECT 'COLLECTED',CAST(thoiGian AS date),0,SUM(soTien)
                          FROM ValidPayments WHERE thoiGian>=? GROUP BY CAST(thoiGian AS date)
                        UNION ALL
                        SELECT 'DEBT',CAST(NULL AS date),0,COALESCE(SUM(a.total-COALESCE(p.paid,0)),0)
                          FROM ActiveInvoices a LEFT JOIN
                          (SELECT maHoaDon,SUM(soTien) AS paid FROM ValidPayments GROUP BY maHoaDon) p ON p.maHoaDon=a.maHoaDon
                        """;
                BigDecimal debt = BigDecimal.ZERO;
                try (PreparedStatement s = c.prepareStatement(sql)) {
                    s.setQueryTimeout(30);
                    Timestamp cutoff = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
                    for (int i = 1; i <= 4; i++) s.setTimestamp(i, cutoff);
                    s.setTimestamp(5, Timestamp.valueOf(from.atStartOfDay()));
                    s.setTimestamp(6, Timestamp.valueOf(from.atStartOfDay()));
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            BigDecimal amount = r.getBigDecimal("amount");
                            if (r.getString("kind").equals("DEBT")) { debt = amount; continue; }
                            LocalDate day = r.getDate("day").toLocalDate();
                            LocalDate key = byMonth ? day.withDayOfMonth(1) : day;
                            var old = rows.get(key);
                            boolean revenue = r.getString("kind").equals("REVENUE");
                            rows.put(key, new RevenueReport.Row(key, old.invoices() + r.getInt("invoices"),
                                    revenue ? old.revenue().add(amount) : old.revenue(),
                                    revenue ? old.collected() : old.collected().add(amount)));
                        }
                    }
                }
                BigDecimal revenue = BigDecimal.ZERO, collected = BigDecimal.ZERO; int invoices = 0;
                for (var row : rows.values()) { revenue = revenue.add(row.revenue()); collected = collected.add(row.collected()); invoices += row.invoices(); }
                require(); c.commit();
                return new RevenueReport(from, to, byMonth, List.copyOf(rows.values()), revenue, collected, debt, invoices);
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }
}
