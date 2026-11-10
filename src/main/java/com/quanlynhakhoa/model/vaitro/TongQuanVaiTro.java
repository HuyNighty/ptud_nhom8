package com.quanlynhakhoa.model.vaitro;

import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import java.util.Set;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TongQuanVaiTro {
    private final VaiTroHeThong role;
    private final Set<Permission> permissions;
    private final int accountCount;

    public TongQuanVaiTro(VaiTroHeThong role, Set<Permission> permissions, int accountCount) {
        if (role == null || permissions == null || accountCount < 0) {
        throw new IllegalArgumentException("Invalid role overview");
        }
        permissions = Set.copyOf(permissions);
        this.role = role;
        this.permissions = permissions;
        this.accountCount = accountCount;
    }

    public VaiTroHeThong role() { return role; }
    public Set<Permission> permissions() { return permissions; }
    public int accountCount() { return accountCount; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TongQuanVaiTro value)) return false;
        return java.util.Objects.equals(role, value.role)
                && java.util.Objects.equals(permissions, value.permissions)
                && accountCount == value.accountCount;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(role);
        result = 31 * result + java.util.Objects.hashCode(permissions);
        result = 31 * result + Integer.hashCode(accountCount);
        return result;
    }

    @Override
    public String toString() {
        return "RoleOverview[" + "role=" + role + ", " + "permissions=" + permissions + ", " + "accountCount=" + accountCount + "]";
    }
}
