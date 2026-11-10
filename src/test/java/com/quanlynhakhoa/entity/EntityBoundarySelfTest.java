package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.dao.phieukham.DonThuocDao;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.kho.ThongTinLoHang;
import com.quanlynhakhoa.model.phieukham.DongChiTietDonThuoc;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Checks the new entity boundaries and JDBC mapping without connecting to a database. */
public final class EntityBoundarySelfTest {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static void absent(Class<?> type, String field) {
        boolean found = Arrays.stream(type.getDeclaredFields()).anyMatch(f -> f.getName().equals(field));
        check(!found, type.getSimpleName() + " must not own projection field " + field);
    }
    private static Object zero(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
    private static final class JdbcProbe implements InvocationHandler {
        final Map<Integer,Object> bound = new LinkedHashMap<>();
        final List<String> events = new ArrayList<>();
        int changed = 1;
        int cursor;
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, this);
        }
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (Set.of("commit", "rollback", "setAutoCommit", "setTransactionIsolation").contains(name))
                throw new AssertionError("DAO took ownership of the caller's transaction");
            if (name.equals("prepareStatement")) {
                bound.clear(); cursor = 0;
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, this);
            }
            if (Set.of("setString", "setNString", "setInt", "setDate").contains(name)) {
                bound.put((Integer)args[0], args[1]); return null;
            }
            if (name.equals("executeUpdate")) return changed;
            if (name.equals("executeQuery"))
                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, this);
            if (name.equals("next")) return cursor++ == 0;
            if (name.equals("getString") || name.equals("getNString")) return switch ((String)args[0]) {
                case "maThuoc" -> "QA_T";
                case "tenThuoc" -> "Thuốc thử";
                case "donViTinh" -> "Viên";
                case "lieuDung" -> "1 viên";
                case "cachDung" -> "Sau ăn";
                case "hoatChat" -> "QA hoạt chất";
                default -> throw new AssertionError("Unexpected column " + args[0]);
            };
            if (name.equals("getInt")) return args[0].equals("soLuong") ? 6 : 3;
            if (name.equals("getBoolean")) return true;
            if (name.equals("close")) {
                events.add(proxy instanceof ResultSet ? "result-close" : "statement-close"); return null;
            }
            return zero(method.getReturnType());
        }
    }
    public static void main(String[] args) throws Exception {
        for (Class<?> type : List.of(HoaDon.class, ChiTietHoaDon.class, ThanhToan.class,
                YeuCauHuyHoaDon.class, TaiKhoan.class, PhanCong.class, PhieuKham.class,
                ChiDinhCanLamSang.class, ChiTietDonThuoc.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                check(Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
                        "Previously immutable values must remain immutable: " + type.getSimpleName());
            }
        }
        absent(HoaDon.class, "congNoBenhNhan"); absent(HoaDon.class, "tenBenhNhan");
        absent(HoaDon.class, "trangThaiYeuCauHuy"); absent(ChiTietHoaDon.class, "tenHang");
        absent(LoHang.class, "tenNhaCungCap"); absent(PhieuKham.class, "tenBacSi");
        absent(HoSoBenhAn.class, "danhSachLuotKham");
        ThongTinHoaDon invoice = new ThongTinHoaDon("QA_H", "QA_NV", "QA_BN", "Tên hiển thị",
                null, LocalDateTime.of(2026,11,10,9,0), TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,
                new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("500"));
        check(invoice.getHoaDon().getMaHoaDon().equals(invoice.maHoaDon()), "Projection must use its entity");
        check(invoice.conNo().equals(new BigDecimal("70")), "Invoice debt must retain its original calculation");
        check(invoice.congNoBenhNhan().equals(new BigDecimal("500")), "Patient aggregate debt stays separate");
        try {
            new ChiTietHoaDon(1, "QA_H", "QA_DV", "QA_T", null, 1, BigDecimal.ONE);
            throw new AssertionError("Mixed item kinds accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        try {
            new ChiTietHoaDon(1, "QA_H", "QA_DV", null, null, 1, new BigDecimal("0.5"));
            throw new AssertionError("Fractional settled price accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        ThongTinLoHang lot = new ThongTinLoHang();
        lot.setMaLo("QA_LO"); lot.setSoLuongTon(12); lot.setHanSuDung(LocalDate.of(2026,11,9));
        check(lot.getMaLo().equals("QA_LO") && lot.getSoLuongTon() == 12, "Legacy lot setters delegate correctly");
        check(lot.daHetHan(LocalDate.of(2026,11,10)), "Expiry behavior retained after delegation");
        check(new ThongTinLoHang().getSoLuongTon() == 0, "Default lots must not share mutable entity state");
        NhanVien employee = new NhanVien("QA_NV", "A", "0900000000", "LT",
                TrangThaiNhanVien.DANG_LAM_VIEC, null, null, null, LocalDate.of(2026,11,10), null, null);
        employee.setHoTen("B"); employee.setSoDienThoai("0911111111"); employee.setEmail("qa@example.invalid");
        check(employee.getHoTen().equals("B") && employee.getEmail().equals("qa@example.invalid"), "Editable profile fields have setters");
        check(employee.getMaNhanVien().equals("QA_NV") && employee.getTrangThai() == TrangThaiNhanVien.DANG_LAM_VIEC,
                "Editing a profile must not change identity or employment state");

        JdbcProbe probe = new JdbcProbe(); DonThuocDao dao = new DonThuocDao();
        DongChiTietDonThuoc item = new DongChiTietDonThuoc("QA_T", "Thuốc thử", "Viên", 6, "1 viên", "Sau ăn", 3, true, "QA hoạt chất");
        dao.taoChiTiet(probe.connection(), "QA_DT", item);
        check(new ArrayList<>(probe.bound.values()).equals(List.of("QA_DT", "QA_T", 6, "1 viên", "Sau ăn", 3)),
                "Prescription entity must preserve all six JDBC bindings in order");
        check(probe.events.equals(List.of("statement-close")), "Insert statement closes without owning the transaction");
        LocalDate before = LocalDate.now();
        dao.taoDonThuoc(probe.connection(), "QA_DT", "QA_PK", "QA_BN", "QA_BS", " ");
        check(probe.bound.get(1).equals("QA_DT") && probe.bound.get(4).equals("QA_BS"), "Prescription identity binding retained");
        LocalDate date = ((java.sql.Date)probe.bound.get(5)).toLocalDate();
        check(!date.isBefore(before) && !date.isAfter(LocalDate.now()), "Prescription date retains the clinic application clock");
        check(probe.bound.containsKey(6) && probe.bound.get(6) == null, "Blank advice must still bind SQL NULL");
        probe.events.clear();
        DongChiTietDonThuoc loaded = dao.danhSachChiTiet(probe.connection(), "QA_DT").getFirst();
        check(loaded.getChiTietDonThuoc().getMaDonThuoc().equals("QA_DT"), "Loaded detail carries the composite prescription key");
        check(loaded.maThuoc().equals("QA_T") && loaded.soLuong() == 6 && loaded.kiemSoatDacBiet(), "Clinical display metadata retained");
        check(probe.events.equals(List.of("result-close", "statement-close")), "ResultSet closes before its statement");
        probe.changed = 0;
        try { dao.taoChiTiet(probe.connection(), "QA_DT", item); throw new AssertionError("Zero-row insert accepted"); }
        catch (SQLException expected) { checks++; }
        System.out.println("PASS EntityBoundarySelfTest: " + checks + " entity, immutability and prescription JDBC checks.");
    }
}
