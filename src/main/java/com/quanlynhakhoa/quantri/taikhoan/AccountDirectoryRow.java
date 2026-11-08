package com.quanlynhakhoa.quantri.taikhoan;

import com.quanlynhakhoa.quantri.taikhoan.entity.AccountStatus;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.quantri.taikhoan.entity.LockSource;
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
