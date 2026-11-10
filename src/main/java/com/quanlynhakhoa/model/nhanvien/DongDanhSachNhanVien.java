package com.quanlynhakhoa.model.nhanvien;

import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongDanhSachNhanVien {
    private final String employeeId;
    private final String fullName;
    private final String jobTitle;
    private final TrangThaiNhanVien status;
    private final LocalDate joinedAt;
    private final String accountId;
    private final VaiTroHeThong accountRole;

    public DongDanhSachNhanVien(String employeeId, String fullName, String jobTitle, TrangThaiNhanVien status, LocalDate joinedAt, String accountId, VaiTroHeThong accountRole) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.status = status;
        this.joinedAt = joinedAt;
        this.accountId = accountId;
        this.accountRole = accountRole;
    }

    public String employeeId() { return employeeId; }
    public String fullName() { return fullName; }
    public String jobTitle() { return jobTitle; }
    public TrangThaiNhanVien status() { return status; }
    public LocalDate joinedAt() { return joinedAt; }
    public String accountId() { return accountId; }
    public VaiTroHeThong accountRole() { return accountRole; }

    public boolean hasAccount() {
        return accountId != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongDanhSachNhanVien value)) return false;
        return java.util.Objects.equals(employeeId, value.employeeId)
                && java.util.Objects.equals(fullName, value.fullName)
                && java.util.Objects.equals(jobTitle, value.jobTitle)
                && java.util.Objects.equals(status, value.status)
                && java.util.Objects.equals(joinedAt, value.joinedAt)
                && java.util.Objects.equals(accountId, value.accountId)
                && java.util.Objects.equals(accountRole, value.accountRole);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(employeeId);
        result = 31 * result + java.util.Objects.hashCode(fullName);
        result = 31 * result + java.util.Objects.hashCode(jobTitle);
        result = 31 * result + java.util.Objects.hashCode(status);
        result = 31 * result + java.util.Objects.hashCode(joinedAt);
        result = 31 * result + java.util.Objects.hashCode(accountId);
        result = 31 * result + java.util.Objects.hashCode(accountRole);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeDirectoryRow[" + "employeeId=" + employeeId + ", " + "fullName=" + fullName + ", " + "jobTitle=" + jobTitle + ", " + "status=" + status + ", " + "joinedAt=" + joinedAt + ", " + "accountId=" + accountId + ", " + "accountRole=" + accountRole + "]";
    }
}
