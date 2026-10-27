package com.quanlynhakhoa.model;

import java.time.LocalDateTime;

/** Read-only snapshot of dbo.TaiKhoan, NOT a JPA entity. */
public record Account(
        String id,
        String employeeId,
        String username,
        String displayName,
        String passwordHash,
        Role role,
        AccountStatus status,
        int failedAttempts,
        LocalDateTime firstFailureAt,
        LocalDateTime lockedAt,
        LocalDateTime lockedUntil,
        LockSource lockSource,
        String lockReason,
        boolean forcePasswordChange,
        LocalDateTime passwordUpdatedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime disabledAt,
        String disableReason
) {
    // passwordHash is intentionally never included in a UI or log response.
    // Do not log/serialize this record: record.toString() contains passwordHash.
    @Override
    public String toString() {
        return "Account[id=" + id + ",role=" + role + ",status=" + status + "]";
    }
}
