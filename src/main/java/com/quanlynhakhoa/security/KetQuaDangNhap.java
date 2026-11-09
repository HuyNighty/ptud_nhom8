package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.VaiTroHeThong;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaDangNhap {
    private final boolean authenticated;
    private final String accountId;
    private final VaiTroHeThong role;
    private final boolean forcePasswordChange;

    public KetQuaDangNhap(boolean authenticated, String accountId, VaiTroHeThong role, boolean forcePasswordChange) {
        this.authenticated = authenticated;
        this.accountId = accountId;
        this.role = role;
        this.forcePasswordChange = forcePasswordChange;
    }

    public boolean authenticated() { return authenticated; }
    public String accountId() { return accountId; }
    public VaiTroHeThong role() { return role; }
    public boolean forcePasswordChange() { return forcePasswordChange; }

    public static KetQuaDangNhap rejected() {
        return new KetQuaDangNhap(false, null, null, false);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaDangNhap value)) return false;
        return authenticated == value.authenticated
                && java.util.Objects.equals(accountId, value.accountId)
                && java.util.Objects.equals(role, value.role)
                && forcePasswordChange == value.forcePasswordChange;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(this.authenticated);
        result = 31 * result + java.util.Objects.hashCode(this.accountId);
        result = 31 * result + java.util.Objects.hashCode(this.role);
        result = 31 * result + Boolean.hashCode(this.forcePasswordChange);
        return result;
    }

    @Override
    public String toString() {
        return "AuthDecision[" + "authenticated=" + authenticated + ", " + "accountId=" + accountId + ", " + "role=" + role + ", " + "forcePasswordChange=" + forcePasswordChange + "]";
    }
}
