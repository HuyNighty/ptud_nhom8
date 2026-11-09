package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;





/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TaiKhoanDich {
    private final String accountId;
    private final VaiTroHeThong role;
    private final TrangThaiTaiKhoan status;
    private final NguonKhoaTaiKhoan lockSource;

    public TaiKhoanDich(String accountId, VaiTroHeThong role, TrangThaiTaiKhoan status, NguonKhoaTaiKhoan lockSource) {
        this.accountId = accountId;
        this.role = role;
        this.status = status;
        this.lockSource = lockSource;
    }

    public String accountId() { return accountId; }
    public VaiTroHeThong role() { return role; }
    public TrangThaiTaiKhoan status() { return status; }
    public NguonKhoaTaiKhoan lockSource() { return lockSource; }

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
