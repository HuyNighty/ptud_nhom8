package com.quanlynhakhoa.service.baocao;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.model.baocao.DongBaoCaoDoanhThu;
import com.quanlynhakhoa.model.baocao.BaoCaoDoanhThu;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.dao.baocao.BaoCaoDoanhThuDao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** UC029: invoice-date revenue, receipt-date collections and receivables at period end. */
public final class BaoCaoDoanhThuService {
    public interface Connections { Connection open() throws SQLException; }
    private final BaoCaoDoanhThuDao dao = new BaoCaoDoanhThuDao();
    private final DesktopSession session;
    private final Connections connections;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public BaoCaoDoanhThuService(DesktopSession session) { this(session, TeamDevConnection::open); }
    public BaoCaoDoanhThuService(DesktopSession session, Connections connections) {
        this.session = Objects.requireNonNull(session); this.connections = Objects.requireNonNull(connections);
    }
    public static void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.getYear() < 1900 || to.getYear() > 9998)
            throw new IllegalArgumentException("Chọn khoảng ngày hợp lệ; từ ngày không được sau đến ngày.");
    }
    private void require() { authorization.require(session, Quyen.REVENUE_REPORT_READ); }
    public BaoCaoDoanhThu load(LocalDate from, LocalDate to, boolean monthly) throws SQLException {
        require(); validate(from, to);
        // Bound generated display rows without restricting the date interval itself.
        boolean byMonth = monthly || ChronoUnit.DAYS.between(from, to) > 730;
        try (Connection c = connections.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); c.setAutoCommit(false);
            try {
                require();
                TreeMap<LocalDate, DongBaoCaoDoanhThu> rows = new TreeMap<>();
                LocalDate first = byMonth ? from.withDayOfMonth(1) : from;
                for (LocalDate day = first; !day.isAfter(to); day = byMonth ? day.plusMonths(1) : day.plusDays(1))
                    rows.put(day, new DongBaoCaoDoanhThu(day, 0, BigDecimal.ZERO, BigDecimal.ZERO));
                // Separate aggregates avoid multiplying invoice lines by payment rows.
                BigDecimal debt = BigDecimal.ZERO;
                for (var tongHop : dao.load(c, from, to)) {
                    BigDecimal amount = tongHop.soTien();
                    if (tongHop.loai().equals("DEBT")) { debt = amount; continue; }
                    LocalDate day = tongHop.ngay();
                    LocalDate key = byMonth ? day.withDayOfMonth(1) : day;
                    var old = rows.get(key);
                    boolean revenue = tongHop.loai().equals("REVENUE");
                    rows.put(key, new DongBaoCaoDoanhThu(key, old.invoices() + tongHop.soHoaDon(),
                            revenue ? old.revenue().add(amount) : old.revenue(),
                            revenue ? old.collected() : old.collected().add(amount)));
                }
                BigDecimal revenue = BigDecimal.ZERO, collected = BigDecimal.ZERO; int invoices = 0;
                for (var row : rows.values()) { revenue = revenue.add(row.revenue()); collected = collected.add(row.collected()); invoices += row.invoices(); }
                require(); c.commit();
                return new BaoCaoDoanhThu(from, to, byMonth, List.copyOf(rows.values()), revenue, collected, debt, invoices);
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }
}
