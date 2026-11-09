package com.quanlynhakhoa.hethong.xacthuc;

import com.quanlynhakhoa.enums.VaiTroHeThong;

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
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(false, "x", VaiTroHeThong.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, null, VaiTroHeThong.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, " ", VaiTroHeThong.SYSTEM_ADMIN, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", null, false)));
        check(!DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", VaiTroHeThong.SYSTEM_ADMIN, true)));
        check(DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", VaiTroHeThong.SYSTEM_ADMIN, false)));
        check(DesktopLoginGate.mayOpenMain(new AuthDecision(true, "x", VaiTroHeThong.RECEPTIONIST, false)));
        System.out.println("PASS: R3-A3 DESKTOP LOGIN GATE SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO SWING WINDOW, NO JDBC, NO SQL WRITES");
    }
}
