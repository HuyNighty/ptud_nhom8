package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.BanNhapHoaDon;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.*;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Invoice creation is available only with approved clinical and ID dependencies. No payment writes. */
public final class InvoiceService {
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final InvoiceDao dao;
    private InvoiceCreationService creation;
    public InvoiceService(DesktopSession session, ConnectionProvider connections,
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
    public List<LuaChonLapHoaDon> patientsForInvoice(String text) throws SQLException { return creation().patients(text); }
    public List<LuaChonLapHoaDon> visitsForInvoice(String patient) throws SQLException { return creation().visits(patient); }
    public BanNhapHoaDon previewInvoice(String patient, String visit) throws SQLException { return creation().preview(patient, visit); }
    public String createInvoice(BanNhapHoaDon draft) throws SQLException { return creation().create(draft); }
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    public InvoiceService(DesktopSession session, ConnectionProvider connections) { this(session, connections, new InvoiceDao()); }
    public InvoiceService(DesktopSession session, ConnectionProvider connections, InvoiceDao dao) {
        this.session = Objects.requireNonNull(session);
        this.connections = Objects.requireNonNull(connections);
        this.dao = Objects.requireNonNull(dao);
    }
    public List<ThongTinHoaDon> find(String keyword, LocalDate from, LocalDate to, InvoiceStatus status) throws SQLException {
        require();
        String text = keyword == null ? "" : keyword.trim();
        if (text.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        validateDates(from, to);
        try(Connection c=connections.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            List<ThongTinHoaDon> result=dao.find(c,text,from,to,status);require();return result;
        }
    }
    public List<ThongTinHoaDon> findPage(String keyword,LocalDate from,LocalDate to,InvoiceStatus status,int page)throws SQLException {
        require();validateDates(from,to);String text=keyword==null?"":keyword.trim();
        if(text.length()>100||page<0||page>1000000)throw new IllegalArgumentException("Bộ lọc hoặc trang không hợp lệ.");
        try(Connection c=connections.open()){
            // A single READ COMMITTED statement avoids holding range locks across list rendering.
            List<ThongTinHoaDon> result=dao.findPage(c,text,from,to,status,Math.multiplyExact(page,100),101);require();return result;
        }
    }

    public List<ThongTinHoaDon> outstandingChoices(String keyword)throws SQLException {
        require();String text=keyword==null?"":keyword.trim();if(text.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        try(Connection c=connections.open()){List<ThongTinHoaDon> rows=dao.findPage(c,text,null,null,null,0,101,true);require();return rows;}
    }

    public DuLieuChiTietHoaDon detail(String id) throws SQLException {
        require();
        if (id == null || id.isBlank() || id.trim().length() > 10) throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        try (Connection connection = connections.open()) {
            require();
            beginRead(connection);
            try {
                ThongTinHoaDon invoice = dao.get(connection, id.trim());
                if (invoice == null) throw new IllegalArgumentException("Hóa đơn không tồn tại.");
                List<DongChiTietHoaDon> lines = dao.lines(connection, id.trim());
                List<ThongTinThanhToan> payments = dao.payments(connection, id.trim());
                var cancellationHistory = new CancellationDao().findForInvoice(connection, id.trim());
                var prescriptions = dao.prescribedMedicines(connection, id.trim());
                if (invoice.tongTien().compareTo(BillingCalculator.total(lines)) != 0
                        || invoice.daThu().compareTo(BillingCalculator.collected(payments)) != 0) {
                    throw new SQLException("Kết quả đối soát hóa đơn không nhất quán.");
                }
                require(); connection.commit();
                return new DuLieuChiTietHoaDon(invoice, lines, payments, cancellationHistory, prescriptions);
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
