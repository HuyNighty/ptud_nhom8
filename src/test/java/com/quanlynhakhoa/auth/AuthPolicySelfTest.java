package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.model.*;
import java.time.LocalDateTime;

/** Synthetic-only test. Never connects to SQL Server or reads a real credential. */
public final class AuthPolicySelfTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    private static int checks = 0;
    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }
    private static Account account(Role role, String employeeId, AccountStatus state,
            int attempts, LocalDateTime first, LocalDateTime until, LockSource source,
            boolean forceChange) {
        return new Account("TEST0001", employeeId, "synthetic-user", "Synthetic User",
                "synthetic-not-a-stored-hash", role, state, attempts, first,
                until == null ? null : until.minusMinutes(15), until, source,
                source == null ? null : "Synthetic reason", forceChange,
                NOW.minusDays(5), null, NOW.minusDays(5), NOW.minusDays(1), null, null);
    }
    private static final LoginPolicy SYNTH = new LoginPolicy((raw, hash) -> "correct".equals(raw));
    public static void main(String[] args) {
        Account admin = account(Role.SYSTEM_ADMIN, null, AccountStatus.HOAT_DONG,
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
        Account four = account(Role.SYSTEM_ADMIN, null, AccountStatus.HOAT_DONG, 4,
                NOW.minusMinutes(4), null, null, false);
        LoginPlan fifth = SYNTH.evaluate(four, null, "bad", NOW);
        check(fifth.nextStatus() == AccountStatus.BI_KHOA &&
                fifth.nextFailedAttempts() == 5 &&
                fifth.nextLockSource() == LockSource.FAILED_LOGIN &&
                fifth.nextLockedUntil().equals(NOW.plusMinutes(15)), "fifth attempt locks");
        Account oldWindow = account(Role.SYSTEM_ADMIN, null, AccountStatus.HOAT_DONG, 4,
                NOW.minusMinutes(15), null, null, false);
        check(SYNTH.evaluate(oldWindow, null, "bad", NOW).nextFailedAttempts() == 1,
                "boundary resets failure window");
        Account autoLock = account(Role.SYSTEM_ADMIN, null, AccountStatus.BI_KHOA, 5,
                NOW.minusMinutes(15), NOW.plusMinutes(1), LockSource.FAILED_LOGIN, false);
        check(!SYNTH.evaluate(autoLock, null, "correct", NOW).stateChanged(),
                "locked before expiry");
        Account expired = account(Role.SYSTEM_ADMIN, null, AccountStatus.BI_KHOA, 5,
                NOW.minusMinutes(40), NOW, LockSource.FAILED_LOGIN, false);
        LoginPlan expiredGood = SYNTH.evaluate(expired, null, "correct", NOW);
        check(expiredGood.authenticated() && expiredGood.nextStatus() == AccountStatus.HOAT_DONG &&
                expiredGood.nextLockSource() == null, "automatic expired unlock");
        LoginPlan expiredBad = SYNTH.evaluate(expired, null, "bad", NOW);
        check(!expiredBad.authenticated() && expiredBad.nextStatus() == AccountStatus.HOAT_DONG &&
                expiredBad.nextFailedAttempts() == 1, "expired reset + new failed login");
        Account adminLock = account(Role.SYSTEM_ADMIN, null, AccountStatus.BI_KHOA, 0,
                null, null, LockSource.ADMIN_ACTION, false);
        check(!SYNTH.evaluate(adminLock, null, "correct", NOW).stateChanged(),
                "admin lock never auto unlocks");
        Account disabled = account(Role.SYSTEM_ADMIN, null, AccountStatus.VO_HIEU, 0,
                null, null, null, false);
        check(!SYNTH.evaluate(disabled, null, "correct", NOW).authenticated(),
                "disabled account");
        Account worker = account(Role.RECEPTIONIST, "NV0001", AccountStatus.HOAT_DONG,
                0, null, null, null, false);
        check(!SYNTH.evaluate(worker, null, "correct", NOW).authenticated(),
                "missing employee rejected");
        Employee departed = new Employee("NV0001", "Synthetic Person", "LT",
                EmployeeStatus.NGHI_VIEC, NOW.toLocalDate());
        check(!SYNTH.evaluate(worker, departed, "correct", NOW).authenticated(),
                "departed employee rejected");
        Employee working = new Employee("NV0001", "Synthetic Person", "LT",
                EmployeeStatus.DANG_LAM_VIEC, null);
        check(SYNTH.evaluate(worker, working, "correct", NOW).authenticated(),
                "working employee accepted");
        Employee mismatched = new Employee("NV9999", "Other", "LT",
                EmployeeStatus.DANG_LAM_VIEC, null);
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
