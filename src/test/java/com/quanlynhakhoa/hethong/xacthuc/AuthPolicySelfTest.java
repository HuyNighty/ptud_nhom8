package com.quanlynhakhoa.hethong.xacthuc;

import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.model.nhanvien.ThongTinNhanVien;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.LocalDateTime;

/** Synthetic-only test. Never connects to SQL Server or reads a real credential. */
public final class AuthPolicySelfTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    private static int checks = 0;
    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }
    private static ThongTinTaiKhoan account(VaiTroHeThong role, String employeeId, TrangThaiTaiKhoan state,
            int attempts, LocalDateTime first, LocalDateTime until, NguonKhoaTaiKhoan source,
            boolean forceChange) {
        return new ThongTinTaiKhoan("TEST0001", employeeId, "synthetic-user", "Synthetic User",
                "synthetic-not-a-stored-hash", role, state, attempts, first,
                until == null ? null : until.minusMinutes(15), until, source,
                source == null ? null : "Synthetic reason", forceChange,
                NOW.minusDays(5), null, NOW.minusDays(5), NOW.minusDays(1), null, null);
    }
    private static final LoginPolicy SYNTH = new LoginPolicy((raw, hash) -> "correct".equals(raw));
    public static void main(String[] args) {
        ThongTinTaiKhoan admin = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.HOAT_DONG,
                0, null, null, null, true);
        LoginPlan good = SYNTH.evaluate(admin, null, "correct", NOW);
        check(good.authenticated() && good.forcePasswordChange(), "valid admin + force-change");
        check(good.stateChanged() && good.nextFailedAttempts() == 0 &&
                good.nextLastLoginAt().equals(NOW), "success transition");
        LoginPlan blank = SYNTH.evaluate(admin, null, " ", NOW);
        check(!blank.authenticated() && !blank.stateChanged(), "blank is rejected without write");
        LoginPlan unknown = SYNTH.evaluate(null, null, "correct", NOW);
        check(!unknown.authenticated() && !unknown.stateChanged(), "unknown account");
        LoginPlan wrong = SYNTH.evaluate(admin, null, "bad", NOW);
        check(!wrong.authenticated() && wrong.nextFailedAttempts() == 1 &&
                wrong.nextFirstFailureAt().equals(NOW), "first failure");
        ThongTinTaiKhoan four = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.HOAT_DONG, 4,
                NOW.minusMinutes(4), null, null, false);
        LoginPlan fifth = SYNTH.evaluate(four, null, "bad", NOW);
        check(fifth.nextStatus() == TrangThaiTaiKhoan.BI_KHOA &&
                fifth.nextFailedAttempts() == 5 &&
                fifth.nextLockSource() == NguonKhoaTaiKhoan.FAILED_LOGIN &&
                fifth.nextLockedUntil().equals(NOW.plusMinutes(15)), "fifth attempt locks");
        ThongTinTaiKhoan oldWindow = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.HOAT_DONG, 4,
                NOW.minusMinutes(15), null, null, false);
        check(SYNTH.evaluate(oldWindow, null, "bad", NOW).nextFailedAttempts() == 1,
                "boundary resets failure window");
        ThongTinTaiKhoan autoLock = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.BI_KHOA, 5,
                NOW.minusMinutes(15), NOW.plusMinutes(1), NguonKhoaTaiKhoan.FAILED_LOGIN, false);
        check(!SYNTH.evaluate(autoLock, null, "correct", NOW).stateChanged(),
                "locked before expiry");
        ThongTinTaiKhoan expired = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.BI_KHOA, 5,
                NOW.minusMinutes(40), NOW, NguonKhoaTaiKhoan.FAILED_LOGIN, false);
        LoginPlan expiredGood = SYNTH.evaluate(expired, null, "correct", NOW);
        check(expiredGood.authenticated() && expiredGood.nextStatus() == TrangThaiTaiKhoan.HOAT_DONG &&
                expiredGood.nextLockSource() == null, "automatic expired unlock");
        LoginPlan expiredBad = SYNTH.evaluate(expired, null, "bad", NOW);
        check(!expiredBad.authenticated() && expiredBad.nextStatus() == TrangThaiTaiKhoan.HOAT_DONG &&
                expiredBad.nextFailedAttempts() == 1, "expired reset + new failed login");
        ThongTinTaiKhoan adminLock = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.BI_KHOA, 0,
                null, null, NguonKhoaTaiKhoan.ADMIN_ACTION, false);
        check(!SYNTH.evaluate(adminLock, null, "correct", NOW).stateChanged(),
                "admin lock never auto unlocks");
        ThongTinTaiKhoan disabled = account(VaiTroHeThong.SYSTEM_ADMIN, null, TrangThaiTaiKhoan.VO_HIEU, 0,
                null, null, null, false);
        check(!SYNTH.evaluate(disabled, null, "correct", NOW).authenticated(),
                "disabled account");
        ThongTinTaiKhoan worker = account(VaiTroHeThong.RECEPTIONIST, "NV0001", TrangThaiTaiKhoan.HOAT_DONG,
                0, null, null, null, false);
        check(!SYNTH.evaluate(worker, null, "correct", NOW).authenticated(),
                "missing employee rejected");
        ThongTinNhanVien departed = new ThongTinNhanVien("NV0001", "Synthetic Person", "LT",
                TrangThaiNhanVien.NGHI_VIEC, NOW.toLocalDate());
        check(!SYNTH.evaluate(worker, departed, "correct", NOW).authenticated(),
                "departed employee rejected");
        ThongTinNhanVien working = new ThongTinNhanVien("NV0001", "Synthetic Person", "LT",
                TrangThaiNhanVien.DANG_LAM_VIEC, null);
        check(SYNTH.evaluate(worker, working, "correct", NOW).authenticated(),
                "working employee accepted");
        ThongTinNhanVien mismatched = new ThongTinNhanVien("NV9999", "Other", "LT",
                TrangThaiNhanVien.DANG_LAM_VIEC, null);
        check(!SYNTH.evaluate(worker, mismatched, "correct", NOW).authenticated(),
                "different employee ID rejected");
        // Check the actual jBCrypt library on synthetic credentials only.
        String sample = org.mindrot.jbcrypt.BCrypt.hashpw("Synthetic#Only",
                org.mindrot.jbcrypt.BCrypt.gensalt(4));
        BCryptPasswordVerifier bcrypt = new BCryptPasswordVerifier();
        check(sample.startsWith("$2a$") && bcrypt.matches("Synthetic#Only", sample),
                "BCrypt $2a verification");
        check(!bcrypt.matches("incorrect", sample), "BCrypt wrong password rejected");
        check(!bcrypt.matches("Synthetic#Only", "invalid"), "invalid hash rejected");
        System.out.println("PASS: R3-A2 AUTH POLICY SYNTHETIC TESTS (" + checks + " checks)");
        System.out.println("NO JDBC, NO SQL WRITES, NO REAL PASSWORD/HASH USED");
    }
}
