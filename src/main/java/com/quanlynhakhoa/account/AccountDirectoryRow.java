package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.model.LockSource;
import java.time.LocalDateTime;

public record AccountDirectoryRow(
        String accountId,
        String username,
        String displayName,
        String employeeId,
        String employeeName,
        Role role,
        AccountStatus status,
        boolean passwordChangeRequired,
        LocalDateTime lastLoginUtc,
        LockSource lockSource) { }
