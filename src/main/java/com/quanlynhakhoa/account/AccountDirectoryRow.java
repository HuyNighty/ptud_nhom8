package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.AccountStatus;
import com.quanlynhakhoa.model.Role;
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
        LocalDateTime lastLoginUtc) { }
