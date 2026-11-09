package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.quantri.taikhoan.entity.AccountStatus;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.quantri.taikhoan.entity.LockSource;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongDanhSachTaiKhoan {
    private final String accountId;
    private final String username;
    private final String displayName;
    private final String employeeId;
    private final String employeeName;
    private final VaiTroHeThong role;
    private final AccountStatus status;
    private final boolean passwordChangeRequired;
    private final LocalDateTime lastLoginUtc;
    private final LockSource lockSource;

    public DongDanhSachTaiKhoan(String accountId, String username, String displayName, String employeeId, String employeeName, VaiTroHeThong role, AccountStatus status, boolean passwordChangeRequired, LocalDateTime lastLoginUtc, LockSource lockSource) {
        this.accountId = accountId;
        this.username = username;
        this.displayName = displayName;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.role = role;
        this.status = status;
        this.passwordChangeRequired = passwordChangeRequired;
        this.lastLoginUtc = lastLoginUtc;
        this.lockSource = lockSource;
    }

    public String accountId() { return accountId; }
    public String username() { return username; }
    public String displayName() { return displayName; }
    public String employeeId() { return employeeId; }
    public String employeeName() { return employeeName; }
    public VaiTroHeThong role() { return role; }
    public AccountStatus status() { return status; }
    public boolean passwordChangeRequired() { return passwordChangeRequired; }
    public LocalDateTime lastLoginUtc() { return lastLoginUtc; }
    public LockSource lockSource() { return lockSource; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongDanhSachTaiKhoan value)) return false;
        return java.util.Objects.equals(accountId, value.accountId)
                && java.util.Objects.equals(username, value.username)
                && java.util.Objects.equals(displayName, value.displayName)
                && java.util.Objects.equals(employeeId, value.employeeId)
                && java.util.Objects.equals(employeeName, value.employeeName)
                && java.util.Objects.equals(role, value.role)
                && java.util.Objects.equals(status, value.status)
                && passwordChangeRequired == value.passwordChangeRequired
                && java.util.Objects.equals(lastLoginUtc, value.lastLoginUtc)
                && java.util.Objects.equals(lockSource, value.lockSource);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(accountId);
        result = 31 * result + java.util.Objects.hashCode(username);
        result = 31 * result + java.util.Objects.hashCode(displayName);
        result = 31 * result + java.util.Objects.hashCode(employeeId);
        result = 31 * result + java.util.Objects.hashCode(employeeName);
        result = 31 * result + java.util.Objects.hashCode(role);
        result = 31 * result + java.util.Objects.hashCode(status);
        result = 31 * result + Boolean.hashCode(passwordChangeRequired);
        result = 31 * result + java.util.Objects.hashCode(lastLoginUtc);
        result = 31 * result + java.util.Objects.hashCode(lockSource);
        return result;
    }

    @Override
    public String toString() {
        return "AccountDirectoryRow[" + "accountId=" + accountId + ", " + "username=" + username + ", " + "displayName=" + displayName + ", " + "employeeId=" + employeeId + ", " + "employeeName=" + employeeName + ", " + "role=" + role + ", " + "status=" + status + ", " + "passwordChangeRequired=" + passwordChangeRequired + ", " + "lastLoginUtc=" + lastLoginUtc + ", " + "lockSource=" + lockSource + "]";
    }
}
