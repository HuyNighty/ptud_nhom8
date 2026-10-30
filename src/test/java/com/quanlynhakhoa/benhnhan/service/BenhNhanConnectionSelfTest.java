package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/** No DB: injected provider is used only by authorized service operations. */
public final class BenhNhanConnectionSelfTest {
    private static int checks;
    private static void check(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message);
    }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }
    public static void main(String[] args) {
        AtomicInteger opened = new AtomicInteger();
        BenhNhanService service = new BenhNhanService(session(Role.RECEPTIONIST), () -> {
            opened.incrementAndGet();
            throw new SQLException("Synthetic connection failure");
        });
        check(opened.get() == 0, "constructor does not access SQL");
        try {
            service.timKiem("", "");
            throw new AssertionError("Expected injected connection exception");
        } catch (SQLException expected) {
            check("Synthetic connection failure".equals(expected.getMessage()), "uses injected connection");
        }
        check(opened.get() == 1, "connection requested exactly once");
        BenhNhanService blocked = new BenhNhanService(session(Role.INVENTORY_MANAGER), () -> {
            opened.incrementAndGet();
            throw new SQLException("Should not open");
        });
        try {
            blocked.timKiem("", "");
            throw new AssertionError("Missing authorization check");
        } catch (AccessDeniedException expected) {
            check(true, "unauthorized request rejected");
        } catch (SQLException e) {
            throw new AssertionError("Connected before authorization", e);
        }
        check(opened.get() == 1, "unauthorized request never opens SQL");
        System.out.println("PASS: BENH NHAN CONNECTION (" + checks + " checks; no DB, no GUI)");
    }
}
