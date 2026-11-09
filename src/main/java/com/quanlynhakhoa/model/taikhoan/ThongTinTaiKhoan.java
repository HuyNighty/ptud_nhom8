package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.quantri.taikhoan.entity.LockSource;
import com.quanlynhakhoa.quantri.taikhoan.entity.AccountStatus;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinTaiKhoan {
    private final String id;
    private final String employeeId;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final VaiTroHeThong role;
    private final AccountStatus status;
    private final int failedAttempts;
    private final LocalDateTime firstFailureAt;
    private final LocalDateTime lockedAt;
    private final LocalDateTime lockedUntil;
    private final LockSource lockSource;
    private final String lockReason;
    private final boolean forcePasswordChange;
    private final LocalDateTime passwordUpdatedAt;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime disabledAt;
    private final String disableReason;

    public ThongTinTaiKhoan(String id, String employeeId, String username, String displayName, String passwordHash, VaiTroHeThong role, AccountStatus status, int failedAttempts, LocalDateTime firstFailureAt, LocalDateTime lockedAt, LocalDateTime lockedUntil, LockSource lockSource, String lockReason, boolean forcePasswordChange, LocalDateTime passwordUpdatedAt, LocalDateTime lastLoginAt, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime disabledAt, String disableReason) {
        this.id = id;
        this.employeeId = employeeId;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.firstFailureAt = firstFailureAt;
        this.lockedAt = lockedAt;
        this.lockedUntil = lockedUntil;
        this.lockSource = lockSource;
        this.lockReason = lockReason;
        this.forcePasswordChange = forcePasswordChange;
        this.passwordUpdatedAt = passwordUpdatedAt;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.disabledAt = disabledAt;
        this.disableReason = disableReason;
    }

    public String id() { return id; }
    public String employeeId() { return employeeId; }
    public String username() { return username; }
    public String displayName() { return displayName; }
    public String passwordHash() { return passwordHash; }
    public VaiTroHeThong role() { return role; }
    public AccountStatus status() { return status; }
    public int failedAttempts() { return failedAttempts; }
    public LocalDateTime firstFailureAt() { return firstFailureAt; }
    public LocalDateTime lockedAt() { return lockedAt; }
    public LocalDateTime lockedUntil() { return lockedUntil; }
    public LockSource lockSource() { return lockSource; }
    public String lockReason() { return lockReason; }
    public boolean forcePasswordChange() { return forcePasswordChange; }
    public LocalDateTime passwordUpdatedAt() { return passwordUpdatedAt; }
    public LocalDateTime lastLoginAt() { return lastLoginAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public LocalDateTime disabledAt() { return disabledAt; }
    public String disableReason() { return disableReason; }

    // passwordHash is intentionally never included in a UI or log response.
    // Do not log/serialize this record: record.toString() contains passwordHash.
    @Override
    public String toString() {
        return "Account[id=" + id + ",role=" + role + ",status=" + status + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinTaiKhoan value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(employeeId, value.employeeId)
                && java.util.Objects.equals(username, value.username)
                && java.util.Objects.equals(displayName, value.displayName)
                && java.util.Objects.equals(passwordHash, value.passwordHash)
                && java.util.Objects.equals(role, value.role)
                && java.util.Objects.equals(status, value.status)
                && failedAttempts == value.failedAttempts
                && java.util.Objects.equals(firstFailureAt, value.firstFailureAt)
                && java.util.Objects.equals(lockedAt, value.lockedAt)
                && java.util.Objects.equals(lockedUntil, value.lockedUntil)
                && java.util.Objects.equals(lockSource, value.lockSource)
                && java.util.Objects.equals(lockReason, value.lockReason)
                && forcePasswordChange == value.forcePasswordChange
                && java.util.Objects.equals(passwordUpdatedAt, value.passwordUpdatedAt)
                && java.util.Objects.equals(lastLoginAt, value.lastLoginAt)
                && java.util.Objects.equals(createdAt, value.createdAt)
                && java.util.Objects.equals(updatedAt, value.updatedAt)
                && java.util.Objects.equals(disabledAt, value.disabledAt)
                && java.util.Objects.equals(disableReason, value.disableReason);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(employeeId);
        result = 31 * result + java.util.Objects.hashCode(username);
        result = 31 * result + java.util.Objects.hashCode(displayName);
        result = 31 * result + java.util.Objects.hashCode(passwordHash);
        result = 31 * result + java.util.Objects.hashCode(role);
        result = 31 * result + java.util.Objects.hashCode(status);
        result = 31 * result + Integer.hashCode(failedAttempts);
        result = 31 * result + java.util.Objects.hashCode(firstFailureAt);
        result = 31 * result + java.util.Objects.hashCode(lockedAt);
        result = 31 * result + java.util.Objects.hashCode(lockedUntil);
        result = 31 * result + java.util.Objects.hashCode(lockSource);
        result = 31 * result + java.util.Objects.hashCode(lockReason);
        result = 31 * result + Boolean.hashCode(forcePasswordChange);
        result = 31 * result + java.util.Objects.hashCode(passwordUpdatedAt);
        result = 31 * result + java.util.Objects.hashCode(lastLoginAt);
        result = 31 * result + java.util.Objects.hashCode(createdAt);
        result = 31 * result + java.util.Objects.hashCode(updatedAt);
        result = 31 * result + java.util.Objects.hashCode(disabledAt);
        result = 31 * result + java.util.Objects.hashCode(disableReason);
        return result;
    }

}
