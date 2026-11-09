package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.quantri.taikhoan.entity.AccountStatus;
import com.quanlynhakhoa.quantri.taikhoan.entity.LockSource;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;





/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TaiKhoanDich {
    private final String accountId;
    private final Role role;
    private final AccountStatus status;
    private final LockSource lockSource;

    public TaiKhoanDich(String accountId, Role role, AccountStatus status, LockSource lockSource) {
        this.accountId = accountId;
        this.role = role;
        this.status = status;
        this.lockSource = lockSource;
    }

    public String accountId() { return accountId; }
    public Role role() { return role; }
    public AccountStatus status() { return status; }
    public LockSource lockSource() { return lockSource; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaiKhoanDich value)) return false;
        return java.util.Objects.equals(accountId, value.accountId)
                && java.util.Objects.equals(role, value.role)
                && java.util.Objects.equals(status, value.status)
                && java.util.Objects.equals(lockSource, value.lockSource);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(accountId);
        result = 31 * result + java.util.Objects.hashCode(role);
        result = 31 * result + java.util.Objects.hashCode(status);
        result = 31 * result + java.util.Objects.hashCode(lockSource);
        return result;
    }

    @Override
    public String toString() {
        return "Target[" + "accountId=" + accountId + ", " + "role=" + role + ", " + "status=" + status + ", " + "lockSource=" + lockSource + "]";
    }
}
