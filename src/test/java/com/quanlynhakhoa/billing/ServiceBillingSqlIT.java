package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.auth.*;
import com.quanlynhakhoa.billing.model.HoaDon;
import com.quanlynhakhoa.billing.service.HoaDonService;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.security.DesktopSession;
import java.io.Console;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/** Opt-in real development SQL acceptance using a genuine manager login; retains test service. */
public final class ServiceBillingSqlIT {
    public static void main(String[] args) throws Exception {
        if (!TeamDevConnection.hasLocalConfig()) {
            System.err.println("BLOCKED: missing approved .n08-dev.local.properties; no SQL attempted."); System.exit(2);
        }
        if (args.length != 2 || !(args[0].equals("--write") || args[0].equals("--read"))
                || !args[1].startsWith("TST") || args[1].length() > 10) throw new IllegalArgumentException("Use --write/--read TSTxxxxxxx");
        Console console = System.console();
        if (console == null) throw new IllegalStateException("Run java in an interactive terminal for hidden password input.");
        String username = console.readLine("Swing username (CLINIC_MANAGER): ");
        char[] password = console.readPassword("Swing password: ");
        AuthDecision decision;
        try {
            decision = new JdbcAuthService(TeamDevConnection::open, new LoginPolicy(new BCryptPasswordVerifier()), Clock.systemUTC())
                    .authenticate(username, new String(password));
        } finally { Arrays.fill(password, '\0'); }
        DesktopSession session = DesktopSession.fromSuccessfulLogin(decision);
        try {
            DichVuService catalog = new DichVuService(session, TeamDevConnection::open);
            if (args[0].equals("--write")) {
                if (!catalog.find(args[1], null).isEmpty()) throw new IllegalArgumentException("Test ID exists; choose a fresh ID. No automatic overwrite/retry.");
                catalog.save(new DichVu(args[1], "[KIỂM THỬ M1] Dịch vụ tổng hợp", BigDecimal.ZERO, "Dữ liệu kiểm thử giữ lại, không xóa.", false, true), true);
                verify(catalog, args[1], "0", true);
                catalog.save(new DichVu(args[1], "[KIỂM THỬ M1] Dịch vụ tổng hợp", new BigDecimal("123456"), "Dữ liệu kiểm thử giữ lại, không xóa.", true, true), false);
                verify(catalog, args[1], "123456", true);
                catalog.changeActive(args[1], false); verify(catalog, args[1], "123456", false);
                catalog.changeActive(args[1], true);
            }
            verify(catalog, args[1], "123456", true);
            List<HoaDon> invoices = new HoaDonService(session, TeamDevConnection::open).find("", null, null, null);
            System.out.println("PASS: real service read " + args[1] + "; billing read " + invoices.size() + " invoices (no patient details printed).");
            System.out.println("Run --read with this ID in a NEW process to verify restart persistence; UI still requires manual acceptance.");
        } finally { session.logout(); }
    }
    private static void verify(DichVuService service, String id, String price, boolean active) throws Exception {
        for (DichVu value : service.find(id, null)) {
            if (value.maDichVu().equals(id) && value.donGia().compareTo(new BigDecimal(price)) == 0 && value.dangApDung() == active) return;
        }
        throw new AssertionError("Service readback failed");
    }
}
