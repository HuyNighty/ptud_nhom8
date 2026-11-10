package com.quanlynhakhoa.security;

import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KeHoachDangNhap {
    private final boolean authenticated;
    private final boolean forcePasswordChange;
    private final boolean stateChanged;
    private final TrangThaiTaiKhoan nextStatus;
    private final int nextFailedAttempts;
    private final LocalDateTime nextFirstFailureAt;
    private final LocalDateTime nextLockedAt;
    private final LocalDateTime nextLockedUntil;
    private final NguonKhoaTaiKhoan nextLockSource;
    private final String nextLockReason;
    private final LocalDateTime nextLastLoginAt;
    private final LocalDateTime nextUpdatedAt;

    public KeHoachDangNhap(boolean authenticated, boolean forcePasswordChange, boolean stateChanged, TrangThaiTaiKhoan nextStatus, int nextFailedAttempts, LocalDateTime nextFirstFailureAt, LocalDateTime nextLockedAt, LocalDateTime nextLockedUntil, NguonKhoaTaiKhoan nextLockSource, String nextLockReason, LocalDateTime nextLastLoginAt, LocalDateTime nextUpdatedAt) {
        this.authenticated = authenticated;
        this.forcePasswordChange = forcePasswordChange;
        this.stateChanged = stateChanged;
        this.nextStatus = nextStatus;
        this.nextFailedAttempts = nextFailedAttempts;
        this.nextFirstFailureAt = nextFirstFailureAt;
        this.nextLockedAt = nextLockedAt;
        this.nextLockedUntil = nextLockedUntil;
        this.nextLockSource = nextLockSource;
        this.nextLockReason = nextLockReason;
        this.nextLastLoginAt = nextLastLoginAt;
        this.nextUpdatedAt = nextUpdatedAt;
    }

    public boolean authenticated() { return authenticated; }
    public boolean forcePasswordChange() { return forcePasswordChange; }
    public boolean stateChanged() { return stateChanged; }
    public TrangThaiTaiKhoan nextStatus() { return nextStatus; }
    public int nextFailedAttempts() { return nextFailedAttempts; }
    public LocalDateTime nextFirstFailureAt() { return nextFirstFailureAt; }
    public LocalDateTime nextLockedAt() { return nextLockedAt; }
    public LocalDateTime nextLockedUntil() { return nextLockedUntil; }
    public NguonKhoaTaiKhoan nextLockSource() { return nextLockSource; }
    public String nextLockReason() { return nextLockReason; }
    public LocalDateTime nextLastLoginAt() { return nextLastLoginAt; }
    public LocalDateTime nextUpdatedAt() { return nextUpdatedAt; }

    public static KeHoachDangNhap rejectedUnknown() {
        return new KeHoachDangNhap(false, false, false, null, 0,
                null, null, null, null, null, null, null);
    }

    public static KeHoachDangNhap unchanged(ThongTinTaiKhoan account) {
        return new KeHoachDangNhap(false, false, false,
                account.status(), account.failedAttempts(), account.firstFailureAt(),
                account.lockedAt(), account.lockedUntil(), account.lockSource(),
                account.lockReason(), account.lastLoginAt(), account.updatedAt());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KeHoachDangNhap value)) return false;
        return authenticated == value.authenticated
                && forcePasswordChange == value.forcePasswordChange
                && stateChanged == value.stateChanged
                && java.util.Objects.equals(nextStatus, value.nextStatus)
                && nextFailedAttempts == value.nextFailedAttempts
                && java.util.Objects.equals(nextFirstFailureAt, value.nextFirstFailureAt)
                && java.util.Objects.equals(nextLockedAt, value.nextLockedAt)
                && java.util.Objects.equals(nextLockedUntil, value.nextLockedUntil)
                && java.util.Objects.equals(nextLockSource, value.nextLockSource)
                && java.util.Objects.equals(nextLockReason, value.nextLockReason)
                && java.util.Objects.equals(nextLastLoginAt, value.nextLastLoginAt)
                && java.util.Objects.equals(nextUpdatedAt, value.nextUpdatedAt);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(this.authenticated);
        result = 31 * result + Boolean.hashCode(this.forcePasswordChange);
        result = 31 * result + Boolean.hashCode(this.stateChanged);
        result = 31 * result + java.util.Objects.hashCode(this.nextStatus);
        result = 31 * result + Integer.hashCode(this.nextFailedAttempts);
        result = 31 * result + java.util.Objects.hashCode(this.nextFirstFailureAt);
        result = 31 * result + java.util.Objects.hashCode(this.nextLockedAt);
        result = 31 * result + java.util.Objects.hashCode(this.nextLockedUntil);
        result = 31 * result + java.util.Objects.hashCode(this.nextLockSource);
        result = 31 * result + java.util.Objects.hashCode(this.nextLockReason);
        result = 31 * result + java.util.Objects.hashCode(this.nextLastLoginAt);
        result = 31 * result + java.util.Objects.hashCode(this.nextUpdatedAt);
        return result;
    }

    @Override
    public String toString() {
        return "LoginPlan[" + "authenticated=" + authenticated + ", " + "forcePasswordChange=" + forcePasswordChange + ", " + "stateChanged=" + stateChanged + ", " + "nextStatus=" + nextStatus + ", " + "nextFailedAttempts=" + nextFailedAttempts + ", " + "nextFirstFailureAt=" + nextFirstFailureAt + ", " + "nextLockedAt=" + nextLockedAt + ", " + "nextLockedUntil=" + nextLockedUntil + ", " + "nextLockSource=" + nextLockSource + ", " + "nextLockReason=" + nextLockReason + ", " + "nextLastLoginAt=" + nextLastLoginAt + ", " + "nextUpdatedAt=" + nextUpdatedAt + "]";
    }
}
