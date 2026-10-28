package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.model.Role;

/** Headless safety checks. Does not instantiate Swing or connect to SQL. */
public final class DesktopLoginGateSelfTest {
    private static int checks;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("DesktopLoginGate test failed at " + (checks + 1));
        checks++;
    }
    public static void main(String[] args) {
        check(!DesktopLoginGate.mayOpenMain(null));
        check(!DesktopLoginGate.mayOpenMain(AuthDecision.rejected()));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(false, "x", Role.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, null, Role.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, " ", Role.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", null, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", Role.SYSTEM_ADMIN, true)));
        check(DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", Role.SYSTEM_ADMIN, false)));
        check(DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", Role.RECEPTIONIST, false)));
        System.out.println("PASS: R3-A3 DESKTOP LOGIN GATE SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO SWING WINDOW, NO JDBC, NO SQL WRITES");
    }
}
