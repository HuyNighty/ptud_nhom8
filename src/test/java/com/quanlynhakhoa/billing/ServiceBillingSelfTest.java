package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class ServiceBillingSelfTest {
    private static int checks;
    public interface Action { void run() throws Exception; }
    public static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    public static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        try { action.run(); } catch (Throwable failure) {
            check(type.isInstance(failure), "Expected " + type + ", got " + failure); return type.cast(failure);
        }
        throw new AssertionError("Expected " + type);
    }
    public static DesktopSession session(Role role) { return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "TST_ACC", role, false)); }
    private static BigDecimal money(String value) { return new BigDecimal(value); }
    private static DichVu value(String price) { return new DichVu(" TST1 ", " Dịch vụ kiểm thử ", money(price), " ", false, true); }
    private static ThanhToan payment(String id, String amount, boolean cancelled) {
        return new ThanhToan(id, "H1", LocalDateTime.of(2026, 1, 1, 8, 0), money(amount), ThanhToan.PhuongThuc.TIEN_MAT,
                null, money("999999"), cancelled ? ThanhToan.TrangThai.DA_HUY : ThanhToan.TrangThai.THANH_CONG, "NV1", null, null, null);
    }
    private static HoaDon invoice(String total, String collected, TrangThaiHoaDon status) {
        return new HoaDon("H1", "NV1", "B1", "Synthetic", null, LocalDateTime.now(), status, money(total), money(collected), BigDecimal.ZERO);
    }
    public static void main(String[] args) throws Exception {
        calculations(); permissions(); validation(); writes(); reads();
        System.out.println("PASS: ServiceBillingSelfTest " + checks + " checks (synthetic JDBC; no SQL Server).");
    }
    private static void calculations() throws Exception {
        List<ChiTietHoaDon> lines = List.of(new ChiTietHoaDon(1, "H1", "DV1", null, null, 2, money("100")),
                new ChiTietHoaDon(2, "H1", null, "TH1", null, 3, money("20")),
                new ChiTietHoaDon(3, "H1", null, null, "VT1", 1, money("40")));
        check(BillingCalculator.total(lines).compareTo(money("300")) == 0, "all item kinds included");
        check(BillingCalculator.total(List.of()).signum() == 0, "no lines = zero");
        check(BillingCalculator.collected(List.of()).signum() == 0, "no payments = zero");
        List<ThanhToan> payments = List.of(payment("T1", "60", false), payment("T2", "40", false), payment("T3", "900", true));
        check(BillingCalculator.collected(payments).compareTo(money("100")) == 0, "2 lines of successful collection, cancelled ignored, customer tender ignored");
        check(invoice("300", "0", TrangThaiHoaDon.CHUA_THANH_TOAN).conNo().intValueExact() == 300, "unpaid");
        check(invoice("300", "100", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN).conNo().intValueExact() == 200, "partial");
        check(invoice("300", "300", TrangThaiHoaDon.DA_THANH_TOAN).conNo().signum() == 0, "paid");
        HoaDon overpaid = invoice("300", "400", TrangThaiHoaDon.DA_THANH_TOAN);
        check(overpaid.conNo().intValueExact() == -100 && overpaid.canhBao().contains("vượt"), "overpayment not clamped");
        check(BillingCalculator.patientDebt(List.of(invoice("300", "100", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN),
                invoice("1000", "0", TrangThaiHoaDon.DA_HUY), overpaid)).intValueExact() == 100, "cancelled invoices excluded, negative debt visible");
        check(invoice("300", "100", TrangThaiHoaDon.DA_THANH_TOAN).canhBao().contains("BẤT NHẤT"), "status discrepancy");
        BigDecimal big = new ChiTietHoaDon(1, "H1", "DV1", null, null, Integer.MAX_VALUE, money("999999999999999999")).thanhTien();
        check(big.equals(money("2147483646999999997852516353")), "large exact decimal");
        expect(IllegalArgumentException.class, new Action() { public void run() { new ChiTietHoaDon(1, "H1", "D1", "T1", null, 1, money("1")); } });
        expect(IllegalArgumentException.class, new Action() { public void run() { new ChiTietHoaDon(1, "H1", "D1", null, null, 0, money("1")); } });
        // A price catalogue update cannot mutate immutable historical unit prices or rows.
        DichVu changed = value("9999");
        DichVu stopped = new DichVu(changed.maDichVu(), changed.tenDichVu(), changed.donGia(), null, false, false);
        check(!stopped.dangApDung() && BillingCalculator.total(lines).intValueExact() == 300, "historical price/link retained");
    }
    private static void permissions() throws Exception {
        for (Role role : Role.values()) {
            final FeatureJdbcDouble jdbc = new FeatureJdbcDouble(); final DesktopSession session = session(role);
            final DichVuService catalog = new DichVuService(session, jdbc); final HoaDonService billing = new HoaDonService(session, jdbc);
            if (role != Role.CLINIC_MANAGER && role != Role.SYSTEM_ADMIN) {
                expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.find("", null); } });
                expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.save(value("0"), true); } });
                expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.changeActive("TST1", false); } });
            } else catalog.find("", null);
            if (role != Role.CLINIC_MANAGER && role != Role.RECEPTIONIST && role != Role.SYSTEM_ADMIN) {
                expect(AccessDeniedException.class, new Action() { public void run() throws Exception { billing.find("", null, null, null); } });
                expect(AccessDeniedException.class, new Action() { public void run() throws Exception { billing.detail("H1"); } });
                check(jdbc.opens == 0, "denied before connection for " + role);
            } else billing.find("", null, null, null);
            int before = jdbc.opens;
            session.logout();
            expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.find("", null); } });
            expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.save(value("0"), false); } });
            expect(AccessDeniedException.class, new Action() { public void run() throws Exception { catalog.changeActive("TST1", true); } });
            expect(AccessDeniedException.class, new Action() { public void run() throws Exception { billing.find("", null, null, null); } });
            expect(AccessDeniedException.class, new Action() { public void run() throws Exception { billing.detail("H1"); } });
            check(jdbc.opens == before, "all methods blocked before JDBC after logout");
        }
    }
    private static void validation() throws Exception {
        for (final String price : new String[]{"", "-1", "1.2", "1,000", "1.000", "abc", "1e3", "+2", "１２", "1000000000000000000"}) {
            expect(IllegalArgumentException.class, new Action() { public void run() { DichVuService.parsePrice(price); } });
        }
        check(DichVuService.parsePrice(" 0 ").signum() == 0, "zero allowed");
        check(DichVuService.parsePrice("999999999999999999").precision() == 18, "max precision");
        DichVu normalized = DichVuService.validate(value("0"));
        check(normalized.maDichVu().equals("TST1") && normalized.moTa() == null && normalized.tenDichVu().equals("Dịch vụ kiểm thử"), "trim/null");
        final FeatureJdbcDouble jdbc = new FeatureJdbcDouble(); final DichVuService service = new DichVuService(session(Role.CLINIC_MANAGER), jdbc);
        for (final DichVu invalid : List.of(new DichVu("X".repeat(11), "Name", money("1"), null, false, true),
                new DichVu("ĐV1", "Name", money("1"), null, false, true), new DichVu("D V", "Name", money("1"), null, false, true),
                new DichVu("D1", " ", money("1"), null, false, true), new DichVu("D1", "X".repeat(101), money("1"), null, false, true),
                new DichVu("D1", "Name", money("1"), "X".repeat(301), false, true), value("-1"), value("0.1"), value("1000000000000000000"))) {
            expect(IllegalArgumentException.class, new Action() { public void run() throws Exception { service.save(invalid, true); } });
        }
        check(jdbc.opens == 0, "invalid data before connection");
    }
    private static void writes() throws Exception {
        final FeatureJdbcDouble jdbc = new FeatureJdbcDouble(); final DichVuService service = new DichVuService(session(Role.CLINIC_MANAGER), jdbc);
        service.save(value("0"), true); service.save(value("100"), false); service.changeActive("TST1", false); service.changeActive("TST1", true);
        check(jdbc.commits == 4 && jdbc.audits.size() == 4 && jdbc.persisted.size() == 4, "writes and audit committed together");
        check(jdbc.audits.getFirst().get(1).equals("TST_ACC"), "audit uses authenticated account");
        check(jdbc.persisted.getFirst().get(2).equals(BigDecimal.ZERO), "BigDecimal binding");
        for (String query : jdbc.sql) check(!query.contains("UPDATE dbo.ChiTietHoaDon") && !query.contains("DELETE"), "no historical mutation/delete");
        jdbc.existing = true;
        expect(IllegalArgumentException.class, new Action() { public void run() throws Exception { service.save(value("1"), true); } });
        jdbc.existing = false; jdbc.duplicate = true;
        expect(IllegalArgumentException.class, new Action() { public void run() throws Exception { service.save(value("1"), true); } });
        jdbc.duplicate = false;
        final int before = jdbc.persisted.size(); jdbc.auditFailure = true;
        SQLException root = expect(SQLException.class, new Action() { public void run() throws Exception { service.save(value("1"), false); } });
        check(root.getErrorCode() == 229 && jdbc.persisted.size() == before, "audit failure rolls back catalogue write");
        jdbc.rollbackFailure = true; jdbc.closeFailure = true;
        root = expect(SQLException.class, new Action() { public void run() throws Exception { service.changeActive("TST1", false); } });
        check(root.getErrorCode() == 229 && root.getSuppressed().length == 2, "original failure preserved with rollback/close failures");
        jdbc.auditFailure = false; jdbc.rollbackFailure = false;
        expect(DichVuService.CommittedWriteException.class, new Action() { public void run() throws Exception { service.save(value("1"), false); } });
        jdbc.closeFailure = false; jdbc.affected = 0;
        expect(SQLException.class, new Action() { public void run() throws Exception { service.changeActive("missing", true); } });
    }
    private static void reads() throws Exception {
        final FeatureJdbcDouble jdbc = new FeatureJdbcDouble(); final HoaDonService service = new HoaDonService(session(Role.RECEPTIONIST), jdbc);
        service.find("O'Brien_%", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), TrangThaiHoaDon.DA_HUY);
        check(!jdbc.sql.getFirst().contains("O'Brien"), "search is parameterized, wildcard characters literal via CHARINDEX");
        check(jdbc.bindings.getFirst().get(7).equals(Timestamp.valueOf("2026-02-01 00:00:00")), "inclusive end date");
        check(jdbc.isolation == Connection.TRANSACTION_SERIALIZABLE, "consistent read isolation");
        expect(IllegalArgumentException.class, new Action() { public void run() throws Exception { service.detail("missing"); } });
        Map<String, Object> header = new HashMap<>();
        header.put("maHoaDon", "H1"); header.put("maBenhNhan", "BN1"); header.put("maNhanVien", "NV1"); header.put("tenBenhNhan", "Synthetic");
        header.put("ngayLap", Timestamp.valueOf("2026-01-01 08:00:00")); header.put("trangThai", "CHUA_THANH_TOAN");
        header.put("tongTien", BigDecimal.ZERO); header.put("daThu", BigDecimal.ZERO); header.put("congNoBenhNhan", BigDecimal.ZERO);
        jdbc.invoiceRows = List.of(header);
        check(service.detail("H1").chiTiet().isEmpty(), "existing invoice without lines distinguished from missing");
        header.put("tongTien", money("300")); header.put("daThu", money("100")); header.put("trangThai", "THANH_TOAN_MOT_PHAN");
        header.put("congNoBenhNhan", money("700"));
        jdbc.lineRows = List.of(lineRow(1, "maDichVu", "DV1", 2, "100"), lineRow(2, "maThuoc", "T1", 3, "20"),
                lineRow(3, "maVatTu", "VT1", 1, "40"));
        jdbc.lineRows.get(0).put("tenHang","Synthetic service name");jdbc.lineRows.get(1).put("tenHang","Synthetic medicine name");jdbc.lineRows.get(2).put("tenHang","Synthetic material name");
        jdbc.paymentRows = List.of(paymentRow("P1", "60", "THANH_CONG", "TIEN_MAT"),
                paymentRow("P2", "40", "THANH_CONG", "THE_POS"), paymentRow("P3", "900", "DA_HUY", "CHUYEN_KHOAN"));
        ChiTietBilling detail = service.detail("H1");
        check(detail.chiTiet().size() == 3 && detail.thanhToan().size() == 3 && detail.hoaDon().conNo().intValueExact() == 200,
                "real DAO maps mixed item types, multiple payments and cancellation");
        check(detail.hoaDon().congNoBenhNhan().intValueExact() == 700, "patient debt passed through unfiltered aggregate");
        check(detail.chiTiet().get(0).tenHienThi().equals("Synthetic service name")&&detail.chiTiet().get(1).tenHienThi().equals("Synthetic medicine name")&&detail.chiTiet().get(2).tenHienThi().equals("Synthetic material name"),"DAO maps names for all three item types without changing frozen prices");
        check(detail.thanhToan().get(2).maTaiKhoanHuy().equals("TST_ACC") && detail.thanhToan().get(2).thoiGianHuy() != null,
                "cancellation metadata mapped");
        header.put("tongTien", BigDecimal.TEN);
        expect(SQLException.class, new Action() { public void run() throws Exception { service.detail("H1"); } });
        int before = jdbc.opens;
        expect(IllegalArgumentException.class, new Action() { public void run() throws Exception { service.find("", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null); } });
        check(jdbc.opens == before && jdbc.updates == 0, "invalid dates before connection; billing read only");
    }
    private static Map<String, Object> lineRow(int id, String kind, String item, int quantity, String price) {
        Map<String, Object> row = new HashMap<>(); row.put("maChiTiet", id); row.put("maHoaDon", "H1");
        row.put(kind, item); row.put("soLuong", quantity); row.put("donGiaApDung", money(price)); return row;
    }
    private static Map<String, Object> paymentRow(String id, String amount, String status, String method) {
        Map<String, Object> row = new HashMap<>(); row.put("maThanhToan", id); row.put("maHoaDon", "H1");
        row.put("thoiGian", Timestamp.valueOf("2026-01-01 09:00:00")); row.put("soTien", money(amount));
        row.put("phuongThuc", method); row.put("trangThai", status); row.put("maNhanVien", "NV1");
        row.put("tienKhachDua", money("999999")); row.put("maGiaoDich", "Synthetic");
        if (status.equals("DA_HUY")) { row.put("maTaiKhoanHuy", "TST_ACC"); row.put("lyDoHuy", "Synthetic cancellation"); row.put("thoiGianHuy", Timestamp.valueOf("2026-01-01 10:00:00")); }
        return row;
    }
}
