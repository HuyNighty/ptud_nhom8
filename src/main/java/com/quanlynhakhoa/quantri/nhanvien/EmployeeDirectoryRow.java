package com.quanlynhakhoa.quantri.nhanvien;

import com.quanlynhakhoa.quantri.nhanvien.model.EmployeeStatus;
import com.quanlynhakhoa.quantri.vaitro.model.Role;
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
