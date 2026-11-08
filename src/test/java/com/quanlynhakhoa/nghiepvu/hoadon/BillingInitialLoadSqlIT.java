package com.quanlynhakhoa.nghiepvu.hoadon;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.InvoiceDao;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.CancellationDao;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.*;
import com.quanlynhakhoa.nghiepvu.hoadon.service.DebtService;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.*;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import static com.quanlynhakhoa.nghiepvu.hoadon.ServiceBillingSelfTest.*;

/** Opt-in SQL read-only acceptance: empty search and initial Swing screen loading. */
public final class BillingInitialLoadSqlIT {
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(owner));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--read-only") || !TeamDevConnection.hasLocalConfig()) {
            throw new IllegalArgumentException("Requires --read-only and local development configuration");
        }
        System.setProperty("java.awt.headless", "true");
        List<Invoice> all;
        int expectedRequests;
        java.util.Map<String, InvoiceDetails> histories = new java.util.HashMap<>();
        InvoiceDao dao = new InvoiceDao();
        try (Connection c = TeamDevConnection.open(); Statement s = c.createStatement()) {
            all = dao.find(c, "", null, null, null);
            CancellationDao cancellations = new CancellationDao();
            var requests = cancellations.find(c, "", null, null, null);
            expectedRequests = requests.size();
            var historySession = session(Role.RECEPTIONIST);
            try {
                var invoiceService = new com.quanlynhakhoa.nghiepvu.hoadon.service.InvoiceService(
                        historySession, TeamDevConnection::open);
                for (Invoice invoice : all) {
                    InvoiceDetails detail = invoiceService.detail(invoice.maHoaDon());
                    var expectedHistory = cancellations.findForInvoice(c, invoice.maHoaDon());
                    check(detail.cancellationHistory().equals(expectedHistory), "service returns invoice-scoped cancellation history");
                    for (var request : detail.cancellationHistory())
                        check(request.maHoaDon().equals(invoice.maHoaDon()), "history never includes another invoice");
                    histories.put(invoice.maHoaDon(), detail);
                }
            } finally { historySession.logout(); }
            for (Invoice invoice : all) {
                InvoiceCancellationRequest latest = null;
                for (var request : requests) {
                    if (!request.maHoaDon().equals(invoice.maHoaDon())) continue;
                    if (latest == null
                            || (request.trangThai() == InvoiceCancellationRequest.TrangThai.CHO_DUYET
                                && latest.trangThai() != InvoiceCancellationRequest.TrangThai.CHO_DUYET)
                            || (request.trangThai() == latest.trangThai()
                                && (request.thoiGianTao().isAfter(latest.thoiGianTao())
                                    || (request.thoiGianTao().equals(latest.thoiGianTao())
                                        && request.maYeuCau().compareTo(latest.maYeuCau()) > 0)))
                            || (request.trangThai() != InvoiceCancellationRequest.TrangThai.CHO_DUYET
                                && latest.trangThai() != InvoiceCancellationRequest.TrangThai.CHO_DUYET
                                && request.thoiGianTao().isAfter(latest.thoiGianTao()))) latest = request;
                }
                check(invoice.cancellationStatus() == (latest == null ? null : latest.trangThai()),
                        "invoice cancellation status matches approval history");
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.YeuCauHuyHoaDon")) {
                r.next(); check(expectedRequests == r.getInt(1), "empty cancellation search returns all requests");
            }
            for (InvoiceCancellationRequest.TrangThai status : InvoiceCancellationRequest.TrangThai.values()) {
                int count = 0;
                for (var request : requests) if (request.trangThai() == status) count++;
                check(cancellations.find(c, "", null, null, status).size() == count, "cancellation status filter");
            }
            for (var request : requests) {
                var day = request.thoiGianTao().toLocalDate();
                check(cancellations.find(c, request.maYeuCau(), day, day, request.trangThai()).size() == 1,
                        "cancellation exact request, inclusive date and status filters");
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.HoaDon")) {
                r.next(); check(all.size() == r.getInt(1), "empty keyword returns all invoices");
            }
            check(!all.isEmpty(), "test requires existing invoices");
            String id = all.getFirst().maHoaDon();
            List<Invoice> filtered = dao.find(c, id, null, null, null);
            check(!filtered.isEmpty(), "specific search finds invoice");
            for (Invoice invoice : filtered) check(invoice.maHoaDon().contains(id)
                    || invoice.maBenhNhan().contains(id) || invoice.tenBenhNhan().contains(id), "search filters rows");
            check(dao.find(c, "NO_MATCH_TEST_123456789", null, null, null).isEmpty(), "unmatched search empty");
            for (InvoiceStatus status : InvoiceStatus.values()) {
                int expected = 0;
                for (Invoice invoice : all) if (invoice.trangThai() == status) expected++;
                check(dao.find(c, "", null, null, status).size() == expected, "status filter with empty keyword");
            }
            var day = all.getFirst().ngayLap().toLocalDate();
            int expected = 0;
            for (Invoice invoice : all) if (invoice.ngayLap().toLocalDate().equals(day)) expected++;
            check(dao.find(c, "", day, day, null).size() == expected, "inclusive date filter");
        }
        var session = session(Role.RECEPTIONIST);
        var approvalSession = session(Role.CLINIC_MANAGER);
        InvoicePanel[] invoices = new InvoicePanel[1];
        DebtPanel[] debt = new DebtPanel[1];
        PaymentPanel[] payment = new PaymentPanel[1];
        CancellationPanel[] cancellation = new CancellationPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            invoices[0] = new InvoicePanel(session, TeamDevConnection::open);
            debt[0] = new DebtPanel(session, TeamDevConnection::open);
            payment[0] = new PaymentPanel(session, TeamDevConnection::open);
            cancellation[0] = new CancellationPanel(approvalSession, TeamDevConnection::open);
            invoices[0].addNotify(); debt[0].addNotify(); payment[0].addNotify(); cancellation[0].addNotify();
        });
        int expectedInvoices = all.size();
        int expectedPatients = DebtService.byPatient(all).size();
        int expectedPayments = DebtService.outstanding(all).size();
        boolean[] loaded = {false};
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while (!loaded[0] && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    loaded[0] = field(invoices[0], "tblHoaDon", JTable.class).getRowCount() == expectedInvoices
                            && debt[0].tblBenhNhan.getRowCount() == expectedPatients
                            && payment[0].cmbHoaDon.getItemCount() == expectedPayments
                            && cancellation[0].tblYeuCau.getRowCount() == expectedRequests;
                } catch (Exception failure) { throw new RuntimeException(failure); }
            });
            if (!loaded[0]) Thread.sleep(30);
        }
        check(loaded[0], "all four screens load without entering a keyword or clicking Search");
        final List<Invoice> expectedRows = all;
        SwingUtilities.invokeAndWait(() -> {
            try {
                JTable table = field(invoices[0], "tblHoaDon", JTable.class);
                for (int row = 0; row < expectedRows.size(); row++) {
                    Invoice invoice = expectedRows.get(row);
                    check(table.getModel().getValueAt(row, 7).equals(invoice.noTheoDoi()),
                            "displayed receivable excludes cancelled invoice");
                    check(table.getModel().getValueAt(row, 9).equals(invoice.cancellationLabel()),
                            "cancellation request column displays current request status");
                    InvoiceDetails detail = histories.get(invoice.maHoaDon());
                    invoices[0].showDetail(detail);
                    JTable historyTable = field(invoices[0], "tblCancellationHistory", JTable.class);
                    check(historyTable.getRowCount() == detail.cancellationHistory().size(), "history tab row count");
                    for (int index = 0; index < detail.cancellationHistory().size(); index++) {
                        var request = detail.cancellationHistory().get(index);
                        check(historyTable.getModel().getValueAt(index, 0).equals(request.maYeuCau()), "history request id");
                        check(java.util.Objects.equals(historyTable.getModel().getValueAt(index, 7), request.lyDoDuyet()), "decision reason shown");
                    }
                    invoices[0].clearDetail();
                    check(historyTable.getRowCount() == 0, "history clears when changing invoice");
                    if (invoice.trangThai() == InvoiceStatus.DA_HUY)
                        check(((java.math.BigDecimal) table.getModel().getValueAt(row, 7)).signum() == 0,
                                "cancelled invoice displays zero receivable");
                }
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
        SwingUtilities.invokeAndWait(() -> {
            session.logout(); approvalSession.logout();
            invoices[0].removeNotify(); debt[0].removeNotify(); payment[0].removeNotify(); cancellation[0].removeNotify();
        });
        System.out.println("PASS: SQL empty search, specific search, status/date filters; initial invoice/debt/payment loading without Search.");
        System.out.println("Loaded: " + expectedInvoices + " invoices, " + expectedPatients
                + " debtor patients, " + expectedPayments + " invoices available for collection; " + expectedRequests + " cancellation requests.");
    }
}
