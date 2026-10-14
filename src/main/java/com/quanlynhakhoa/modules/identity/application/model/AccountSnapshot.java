
package com.quanlynhakhoa.modules.identity.application.model;

import com.quanlynhakhoa.modules.identity.enums.AccountStatus;
import com.quanlynhakhoa.modules.identity.enums.LockSource;
import com.quanlynhakhoa.modules.identity.enums.Role;

import java.time.LocalDateTime;

public record AccountSnapshot(
        String accountId,
        String employeeId,
        String username,
        String passwordHash,
        Role role,
        AccountStatus status,
        int failedAttempts,
        LocalDateTime firstFailedAt,
        LocalDateTime lockedUntil,
        LockSource lockSource,
        boolean mustChangePassword
) {
    @Override
    public String toString() {return "AccountSnapshot[accountId=" + accountId + ", credentials=REDACTED]";}
}
