package com.quanlynhakhoa.workforce;

import com.quanlynhakhoa.model.EmployeeStatus;
import com.quanlynhakhoa.model.Role;
import java.time.LocalDate;

/** Read-only projection from dbo.NhanVien and optional dbo.TaiKhoan. */
public record EmployeeDirectoryRow(
        String employeeId,
        String fullName,
        String jobTitle,
        EmployeeStatus status,
        LocalDate joinedAt,
        String accountId,
        Role accountRole
) {
    public boolean hasAccount() {
        return accountId != null;
    }
}
