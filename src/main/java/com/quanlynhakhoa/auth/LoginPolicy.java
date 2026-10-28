package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.model.Account;
import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.Employee;
import com.quanlynhakhoa.model.EmployeeStatus;
import com.quanlynhakhoa.model.LockSource;
import com.quanlynhakhoa.model.Role;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * R3-A2: deterministic decision logic only. NO JDBC, SQL, or side effects.
 * Caller will eventually own transaction, row locks, and commits.
 */
public final class LoginPolicy {
    private static final int MAX_FAILURES = 5;
    private static final int WINDOW_MINUTES = 15;
    private static final String LOCK_REASON = "Automatic lock after 5 failed logins";
    private final PasswordVerifier verifier;

    public LoginPolicy(PasswordVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    public LoginPlan evaluate(Account account, Employee employee,
                              String rawPassword, LocalDateTime utcNow) {
        Objects.requireNonNull(utcNow, "utcNow");
        if (account == null) return LoginPlan.rejectedUnknown();
        if (rawPassword == null || rawPassword.isBlank()) return LoginPlan.unchanged(account);
        if (account.status() == AccountStatus.VO_HIEU) return LoginPlan.unchanged(account);
        if (account.status() == AccountStatus.BI_KHOA) {
            if (account.lockSource() != LockSource.FAILED_LOGIN ||
                    account.lockedUntil() == null ||
                    utcNow.isBefore(account.lockedUntil())) {
                return LoginPlan.unchanged(account);
            }
        } else if (account.status() != AccountStatus.HOAT_DONG) {
            return LoginPlan.unchanged(account);
        }
        if (!employeeEligible(account, employee)) return LoginPlan.unchanged(account);

        // Expired automatic locks reset the attempt window before verifying credentials.
        boolean expiredAutoLock = account.status() == AccountStatus.BI_KHOA;
        int oldCount = expiredAutoLock ? 0 : account.failedAttempts();
        LocalDateTime oldFirst = expiredAutoLock ? null : account.firstFailureAt();
        boolean matched = verifier.matches(rawPassword, account.passwordHash());
        if (matched) {
            return new LoginPlan(true, account.forcePasswordChange(), true,
                    AccountStatus.HOAT_DONG, 0, null, null, null, null, null,
                    utcNow, utcNow);
        }
        boolean newWindow = oldFirst == null ||
                !utcNow.isBefore(oldFirst.plusMinutes(WINDOW_MINUTES));
        int failures = newWindow ? 1 : oldCount + 1;
        LocalDateTime firstFailureAt = newWindow ? utcNow : oldFirst;
        boolean shouldLock = failures >= MAX_FAILURES;
        return new LoginPlan(false, false, true,
                shouldLock ? AccountStatus.BI_KHOA : AccountStatus.HOAT_DONG,
                failures, firstFailureAt,
                shouldLock ? utcNow : null,
                shouldLock ? utcNow.plusMinutes(WINDOW_MINUTES) : null,
                shouldLock ? LockSource.FAILED_LOGIN : null,
                shouldLock ? LOCK_REASON : null,
                account.lastLoginAt(), utcNow);
    }

    private static boolean employeeEligible(Account account, Employee employee) {
        if (account.employeeId() == null) return account.role() == Role.SYSTEM_ADMIN;
        return employee != null && account.employeeId().equals(employee.id()) &&
                employee.status() == EmployeeStatus.DANG_LAM_VIEC;
    }
}
