package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** No DB: UC009 — quyền mở hồ sơ bệnh án, ghi nhật ký người xem TRƯỚC khi đọc hồ sơ. */
public final class HoSoBenhAnServiceSelfTest {
    private static int checks;

    private static void check(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message);
    }

    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }

    public static void main(String[] args) throws Exception {
        kiemTraQuyen();
        kiemTraNhatKyTruoc();
        kiemTraNhatKyLoi();

        // Chưa chọn bệnh nhân → báo lỗi, không mở kết nối
        AtomicInteger opened = new AtomicInteger();
        HoSoBenhAnService doctor = new HoSoBenhAnService(session(Role.DOCTOR), () -> {
            opened.incrementAndGet();
            throw new SQLException("Should not open");
        });
        try {
            doctor.moHoSo(" ");
            throw new AssertionError("Blank patient id must be rejected");
        } catch (IllegalArgumentException e) {
            check(opened.get() == 0, "blank patient id rejected before SQL");
        }

        // Đã đăng xuất → bị chặn
        DesktopSession daDangXuat = session(Role.DOCTOR);
        daDangXuat.logout();
        try {
            new HoSoBenhAnService(daDangXuat, () -> {
                opened.incrementAndGet();
                throw new SQLException("Should not open");
            }).moHoSo("BN000001");
            throw new AssertionError("Logged-out session must be rejected");
        } catch (AccessDeniedException e) {
            check(opened.get() == 0, "logged-out session never opens SQL");
        }

        System.out.println("PASS: HO SO BENH AN SERVICE (" + checks + " checks; no DB, no GUI)");
    }

    // Bác sĩ + admin được mở (đi tới bước mở kết nối); vai trò khác bị chặn trước khi mở kết nối
    private static void kiemTraQuyen() {
        for (Role role : Role.values()) {
            boolean duocXem = role == Role.DOCTOR || role == Role.SYSTEM_ADMIN;
            AtomicInteger opened = new AtomicInteger();
            HoSoBenhAnService service = new HoSoBenhAnService(session(role), () -> {
                opened.incrementAndGet();
                throw new SQLException("Synthetic connection failure");
            });
            check(service.coQuyen(Permission.MEDICAL_RECORD_READ) == duocXem, role + " coQuyen");
            try {
                service.moHoSo("BN000001");
                throw new AssertionError("Expected exception for " + role);
            } catch (AccessDeniedException e) {
                check(!duocXem, role + " should be allowed");
                check(opened.get() == 0, role + " blocked before SQL");
            } catch (SQLException e) {
                check(duocXem, role + " should be denied before SQL");
                check(opened.get() == 1, role + " opens exactly one connection");
            }
        }
    }

    // UC009 bước 2: câu lệnh ĐẦU TIÊN phải là ghi nhật ký người xem
    private static void kiemTraNhatKyTruoc() {
        List<String> sqlDaChay = new ArrayList<>();
        HoSoBenhAnService service = new HoSoBenhAnService(session(Role.DOCTOR),
                () -> ketNoiGhiLai(sqlDaChay, false));
        try {
            service.moHoSo("BN000001");
            throw new AssertionError("Expected synthetic failure after audit");
        } catch (SQLException e) {
            check("Synthetic read".equals(e.getMessage()), "fails only at the read step");
        }
        check(sqlDaChay.size() == 2, "audit then read were attempted");
        check(sqlDaChay.get(0).startsWith("INSERT INTO dbo.NhatKyThaoTac"), "first statement is the audit insert");
        check(sqlDaChay.get(0).contains("Xem hồ sơ bệnh án"), "audit records a view action");
        check(sqlDaChay.get(1).contains("FROM dbo.HoSoBenhAn"), "medical record read after audit");
    }

    // Ghi nhật ký lỗi → không đọc hồ sơ
    private static void kiemTraNhatKyLoi() {
        List<String> sqlDaChay = new ArrayList<>();
        HoSoBenhAnService service = new HoSoBenhAnService(session(Role.DOCTOR),
                () -> ketNoiGhiLai(sqlDaChay, true));
        try {
            service.moHoSo("BN000001");
            throw new AssertionError("Audit failure must stop opening the record");
        } catch (SQLException e) {
            check("Synthetic audit failure".equals(e.getMessage()), "audit failure propagates");
        }
        check(sqlDaChay.size() == 1, "no medical record read when audit fails");
    }

    // Kết nối giả: ghi lại câu SQL; INSERT nhật ký thành công (hoặc lỗi), mọi câu SELECT báo lỗi
    private static Connection ketNoiGhiLai(List<String> sqlDaChay, boolean nhatKyLoi) {
        return proxy(Connection.class, (p, method, args) -> switch (method.getName()) {
            case "prepareStatement" -> {
                String sql = (String) args[0];
                sqlDaChay.add(sql);
                yield proxy(PreparedStatement.class, (ps, m, a) -> switch (m.getName()) {
                    case "executeUpdate" -> {
                        if (nhatKyLoi) throw new SQLException("Synthetic audit failure");
                        yield 1;
                    }
                    case "executeQuery" -> throw new SQLException("Synthetic read");
                    case "close", "setString", "setQueryTimeout" -> null;
                    default -> throw new AssertionError("Unexpected PreparedStatement." + m.getName());
                });
            }
            case "close" -> null;
            default -> throw new AssertionError("Unexpected Connection." + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
