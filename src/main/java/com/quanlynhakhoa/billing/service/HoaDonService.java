package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.security.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Invoice creation is available only with approved clinical and ID dependencies. No payment writes. */
public final class HoaDonService {
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final HoaDonDao dao;
    private InvoiceCreationService creation;
    public HoaDonService(DesktopSession session, ConnectionProvider connections,
                         ClinicalBillingSource source, InvoiceIdGenerator ids) {
        this(session, connections);
        creation = new InvoiceCreationService(session, connections, source, ids);
    }
    public boolean creationConfigured() { return creation != null; }
    private InvoiceCreationService creation() {
        authorization.require(session, Permission.BILLING_MANAGE);
        if (creation == null) throw new IllegalStateException("Chờ hợp đồng dữ liệu khám và bộ sinh mã được duyệt.");
        return creation;
    }
    public List<BillingChoice> patientsForInvoice(String text) throws SQLException { return creation().patients(text); }
    public List<BillingChoice> visitsForInvoice(String patient) throws SQLException { return creation().visits(patient); }
    public InvoiceDraft previewInvoice(String patient, String visit) throws SQLException { return creation().preview(patient, visit); }
    public String createInvoice(InvoiceDraft draft) throws SQLException { return creation().create(draft); }
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public HoaDonService(DesktopSession session, ConnectionProvider connections) { this(session, connections, new HoaDonDao()); }
    public HoaDonService(DesktopSession session, ConnectionProvider connections, HoaDonDao dao) {
        this.session = Objects.requireNonNull(session);
        this.connections = Objects.requireNonNull(connections);
        this.dao = Objects.requireNonNull(dao);
    }
    public List<HoaDon> find(String keyword, LocalDate from, LocalDate to, TrangThaiHoaDon status) throws SQLException {
        require();
        String text = keyword == null ? "" : keyword.trim();
        if (text.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        validateDates(from, to);
        // SERIALIZABLE also covers each aggregate scan of the list, including row-versioned databases.
        try (Connection connection = connections.open()) {
            require();
            beginRead(connection);
            try {
                List<HoaDon> result = dao.find(connection, text, from, to, status);
                require(); connection.commit(); return result;
            } catch (SQLException | RuntimeException failure) { rollback(connection, failure); throw failure; }
        }
    }
    public ChiTietBilling detail(String id) throws SQLException {
        require();
        if (id == null || id.isBlank() || id.trim().length() > 10) throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        try (Connection connection = connections.open()) {
            require();
            beginRead(connection);
            try {
                HoaDon invoice = dao.get(connection, id.trim());
                if (invoice == null) throw new IllegalArgumentException("Hóa đơn không tồn tại.");
                List<ChiTietHoaDon> lines = dao.lines(connection, id.trim());
                List<ThanhToan> payments = dao.payments(connection, id.trim());
                if (invoice.tongTien().compareTo(BillingCalculator.total(lines)) != 0
                        || invoice.daThu().compareTo(BillingCalculator.collected(payments)) != 0) {
                    throw new SQLException("Kết quả đối soát hóa đơn không nhất quán.");
                }
                require(); connection.commit();
                return new ChiTietBilling(invoice, lines, payments);
            } catch (SQLException | RuntimeException failure) { rollback(connection, failure); throw failure; }
        }
    }
    private void require() { authorization.require(session, Permission.BILLING_READ); }
    private static void beginRead(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Expected fresh auto-commit connection");
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setAutoCommit(false);
    }
    private static void rollback(Connection connection, Exception failure) {
        try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
    }
    private static void validateDates(LocalDate from, LocalDate to) {
        LocalDate min = LocalDate.of(1, 1, 1), max = LocalDate.of(9999, 12, 30);
        if ((from != null && (from.isBefore(min) || from.isAfter(max)))
                || (to != null && (to.isBefore(min) || to.isAfter(max)))
                || (from != null && to != null && from.isAfter(to))) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ (0001-01-01 đến 9999-12-30).");
        }
    }
}
