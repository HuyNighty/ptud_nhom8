package com.quanlynhakhoa.security;

import com.quanlynhakhoa.security.KeHoachDangNhap;
import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.model.nhanvien.ThongTinNhanVien;
import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
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

    public KeHoachDangNhap evaluate(ThongTinTaiKhoan account, ThongTinNhanVien employee,
                              String rawPassword, LocalDateTime utcNow) {
        Objects.requireNonNull(utcNow, "utcNow");
        if (account == null) return KeHoachDangNhap.rejectedUnknown();
        if (rawPassword == null || rawPassword.isBlank()) return KeHoachDangNhap.unchanged(account);
        if (account.status() == TrangThaiTaiKhoan.VO_HIEU) return KeHoachDangNhap.unchanged(account);
        if (account.status() == TrangThaiTaiKhoan.BI_KHOA) {
            if (account.lockSource() != NguonKhoaTaiKhoan.FAILED_LOGIN ||
                    account.lockedUntil() == null ||
                    utcNow.isBefore(account.lockedUntil())) {
                return KeHoachDangNhap.unchanged(account);
            }
        } else if (account.status() != TrangThaiTaiKhoan.HOAT_DONG) {
            return KeHoachDangNhap.unchanged(account);
        }
        if (!employeeEligible(account, employee)) return KeHoachDangNhap.unchanged(account);

        // Expired automatic locks reset the attempt window before verifying credentials.
        boolean expiredAutoLock = account.status() == TrangThaiTaiKhoan.BI_KHOA;
        int oldCount = expiredAutoLock ? 0 : account.failedAttempts();
        LocalDateTime oldFirst = expiredAutoLock ? null : account.firstFailureAt();
        boolean matched = verifier.matches(rawPassword, account.passwordHash());
        if (matched) {
            return new KeHoachDangNhap(true, account.forcePasswordChange(), true,
                    TrangThaiTaiKhoan.HOAT_DONG, 0, null, null, null, null, null,
                    utcNow, utcNow);
        }
        boolean newWindow = oldFirst == null ||
                !utcNow.isBefore(oldFirst.plusMinutes(WINDOW_MINUTES));
        int failures = newWindow ? 1 : oldCount + 1;
        LocalDateTime firstFailureAt = newWindow ? utcNow : oldFirst;
        boolean shouldLock = failures >= MAX_FAILURES;
        return new KeHoachDangNhap(false, false, true,
                shouldLock ? TrangThaiTaiKhoan.BI_KHOA : TrangThaiTaiKhoan.HOAT_DONG,
                failures, firstFailureAt,
                shouldLock ? utcNow : null,
                shouldLock ? utcNow.plusMinutes(WINDOW_MINUTES) : null,
                shouldLock ? NguonKhoaTaiKhoan.FAILED_LOGIN : null,
                shouldLock ? LOCK_REASON : null,
                account.lastLoginAt(), utcNow);
    }

    private static boolean employeeEligible(ThongTinTaiKhoan account, ThongTinNhanVien employee) {
        if (account.employeeId() == null) return account.role() == VaiTroHeThong.SYSTEM_ADMIN;
        return employee != null && account.employeeId().equals(employee.id()) &&
                employee.status() == TrangThaiNhanVien.DANG_LAM_VIEC;
    }
}
