package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.view.hoadon.ThanhToanPanel;
import com.quanlynhakhoa.view.hoadon.HuyHoaDonPanel;
import com.quanlynhakhoa.view.hoadon.HoaDonPanel;
import com.quanlynhakhoa.view.hoadon.CongNoPanel;
import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.dao.hoadon.HuyHoaDonDao;
import com.quanlynhakhoa.service.hoadon.CongNoService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import static com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest.*;

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
        List<ThongTinHoaDon> all;
        int expectedRequests;
        java.util.Map<String, DuLieuChiTietHoaDon> histories = new java.util.HashMap<>();
        HoaDonDao dao = new HoaDonDao();
        try (Connection c = TeamDevConnection.open(); Statement s = c.createStatement()) {
            all = dao.find(c, "", null, null, null);
            HuyHoaDonDao cancellations = new HuyHoaDonDao();
            var requests = cancellations.find(c, "", null, null, null);
            expectedRequests = requests.size();
            var historySession = session(VaiTroHeThong.RECEPTIONIST);
            try {
                var invoiceService = new com.quanlynhakhoa.service.hoadon.HoaDonService(
                        historySession, TeamDevConnection::open);
                for (ThongTinHoaDon invoice : all) {
                    DuLieuChiTietHoaDon detail = invoiceService.detail(invoice.maHoaDon());
                    var expectedHistory = cancellations.findForInvoice(c, invoice.maHoaDon());
                    check(detail.cancellationHistory().equals(expectedHistory), "service returns invoice-scoped cancellation history");
                    for (var request : detail.cancellationHistory())
                        check(request.maHoaDon().equals(invoice.maHoaDon()), "history never includes another invoice");
                    histories.put(invoice.maHoaDon(), detail);
                }
            } finally { historySession.logout(); }
            for (ThongTinHoaDon invoice : all) {
                ThongTinYeuCauHuyHoaDon latest = null;
                for (var request : requests) {
                    if (!request.maHoaDon().equals(invoice.maHoaDon())) continue;
                    if (latest == null
                            || (request.trangThai() == TrangThaiYeuCauHuyHoaDon.CHO_DUYET
                                && latest.trangThai() != TrangThaiYeuCauHuyHoaDon.CHO_DUYET)
                            || (request.trangThai() == latest.trangThai()
                                && (request.thoiGianTao().isAfter(latest.thoiGianTao())
                                    || (request.thoiGianTao().equals(latest.thoiGianTao())
                                        && request.maYeuCau().compareTo(latest.maYeuCau()) > 0)))
                            || (request.trangThai() != TrangThaiYeuCauHuyHoaDon.CHO_DUYET
                                && latest.trangThai() != TrangThaiYeuCauHuyHoaDon.CHO_DUYET
                                && request.thoiGianTao().isAfter(latest.thoiGianTao()))) latest = request;
                }
                check(invoice.cancellationStatus() == (latest == null ? null : latest.trangThai()),
                        "invoice cancellation status matches approval history");
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.YeuCauHuyHoaDon")) {
                r.next(); check(expectedRequests == r.getInt(1), "empty cancellation search returns all requests");
            }
            for (TrangThaiYeuCauHuyHoaDon status : TrangThaiYeuCauHuyHoaDon.values()) {
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
            List<ThongTinHoaDon> filtered = dao.find(c, id, null, null, null);
            check(!filtered.isEmpty(), "specific search finds invoice");
            for (ThongTinHoaDon invoice : filtered) check(invoice.maHoaDon().contains(id)
                    || invoice.maBenhNhan().contains(id) || invoice.tenBenhNhan().contains(id), "search filters rows");
            check(dao.find(c, "NO_MATCH_TEST_123456789", null, null, null).isEmpty(), "unmatched search empty");
            for (TrangThaiHoaDon status : TrangThaiHoaDon.values()) {
                int expected = 0;
                for (ThongTinHoaDon invoice : all) if (invoice.trangThai() == status) expected++;
                check(dao.find(c, "", null, null, status).size() == expected, "status filter with empty keyword");
            }
            var day = all.getFirst().ngayLap().toLocalDate();
            int expected = 0;
            for (ThongTinHoaDon invoice : all) if (invoice.ngayLap().toLocalDate().equals(day)) expected++;
            check(dao.find(c, "", day, day, null).size() == expected, "inclusive date filter");
        }
        var session = session(VaiTroHeThong.RECEPTIONIST);
        var approvalSession = session(VaiTroHeThong.CLINIC_MANAGER);
        HoaDonPanel[] invoices = new HoaDonPanel[1];
        CongNoPanel[] debt = new CongNoPanel[1];
        ThanhToanPanel[] payment = new ThanhToanPanel[1];
        HuyHoaDonPanel[] cancellation = new HuyHoaDonPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            invoices[0] = new HoaDonPanel(session, TeamDevConnection::open);
            debt[0] = new CongNoPanel(session, TeamDevConnection::open);
            payment[0] = new ThanhToanPanel(session, TeamDevConnection::open);
            cancellation[0] = new HuyHoaDonPanel(approvalSession, TeamDevConnection::open);
            invoices[0].addNotify(); debt[0].addNotify(); payment[0].addNotify(); cancellation[0].addNotify();
        });
        int expectedInvoices = all.size();
        int expectedPatients = CongNoService.byPatient(all).size();
        int expectedPayments = CongNoService.outstanding(all).size();
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
        final List<ThongTinHoaDon> expectedRows = all;
        SwingUtilities.invokeAndWait(() -> {
            try {
                JTable table = field(invoices[0], "tblHoaDon", JTable.class);
                for (int row = 0; row < expectedRows.size(); row++) {
                    ThongTinHoaDon invoice = expectedRows.get(row);
                    check(table.getModel().getValueAt(row, 7).equals(invoice.noTheoDoi()),
                            "displayed receivable excludes cancelled invoice");
                    check(table.getModel().getValueAt(row, 9).equals(invoice.cancellationLabel()),
                            "cancellation request column displays current request status");
                    DuLieuChiTietHoaDon detail = histories.get(invoice.maHoaDon());
                    invoices[0].showDetail(detail);
                    JTable historyTable = field(invoices[0], "tblLichSuHuy", JTable.class);
                    check(historyTable.getRowCount() == detail.cancellationHistory().size(), "history tab row count");
                    for (int index = 0; index < detail.cancellationHistory().size(); index++) {
                        var request = detail.cancellationHistory().get(index);
                        check(historyTable.getModel().getValueAt(index, 0).equals(request.maYeuCau()), "history request id");
                        check(java.util.Objects.equals(historyTable.getModel().getValueAt(index, 7), request.lyDoDuyet()), "decision reason shown");
                    }
                    invoices[0].clearDetail();
                    check(historyTable.getRowCount() == 0, "history clears when changing invoice");
                    if (invoice.trangThai() == TrangThaiHoaDon.DA_HUY)
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
