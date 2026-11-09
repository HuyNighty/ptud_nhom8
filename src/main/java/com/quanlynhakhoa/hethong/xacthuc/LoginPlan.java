package com.quanlynhakhoa.hethong.xacthuc;

import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import java.time.LocalDateTime;

/**
 * Pure-state result. Not a DB update command or permission to open the UI.
 * R3-A2 does not save any field; a later transaction layer must apply this atomically.
 */
public record LoginPlan(
        boolean authenticated,
        boolean forcePasswordChange,
        boolean stateChanged,
        TrangThaiTaiKhoan nextStatus,
        int nextFailedAttempts,
        LocalDateTime nextFirstFailureAt,
        LocalDateTime nextLockedAt,
        LocalDateTime nextLockedUntil,
        NguonKhoaTaiKhoan nextLockSource,
        String nextLockReason,
        LocalDateTime nextLastLoginAt,
        LocalDateTime nextUpdatedAt
) {
    public static LoginPlan rejectedUnknown() {
        return new LoginPlan(false, false, false, null, 0,
                null, null, null, null, null, null, null);
    }

    public static LoginPlan unchanged(ThongTinTaiKhoan account) {
        return new LoginPlan(false, false, false,
                account.status(), account.failedAttempts(), account.firstFailureAt(),
                account.lockedAt(), account.lockedUntil(), account.lockSource(),
                account.lockReason(), account.lastLoginAt(), account.updatedAt());
    }
}
