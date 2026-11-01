package com.quanlynhakhoa.quantri.taikhoan;

import com.quanlynhakhoa.quantri.taikhoan.model.AccountStatus;
import com.quanlynhakhoa.quantri.vaitro.model.Role;
import com.quanlynhakhoa.quantri.taikhoan.model.LockSource;
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
