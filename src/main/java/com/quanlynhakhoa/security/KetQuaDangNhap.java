package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.VaiTroHeThong;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KetQuaDangNhap {
    private final boolean daXacThuc;
    private final String maTaiKhoan;
    private final VaiTroHeThong vaiTro;
    private final boolean batBuocDoiMatKhau;

    public KetQuaDangNhap(boolean daXacThuc, String maTaiKhoan, VaiTroHeThong vaiTro, boolean batBuocDoiMatKhau) {
        this.daXacThuc = daXacThuc;
        this.maTaiKhoan = maTaiKhoan;
        this.vaiTro = vaiTro;
        this.batBuocDoiMatKhau = batBuocDoiMatKhau;
    }

    public boolean authenticated() { return daXacThuc; }
    public String accountId() { return maTaiKhoan; }
    public VaiTroHeThong role() { return vaiTro; }
    public boolean forcePasswordChange() { return batBuocDoiMatKhau; }

    public static KetQuaDangNhap rejected() {
        return new KetQuaDangNhap(false, null, null, false);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KetQuaDangNhap value)) return false;
        return daXacThuc == value.daXacThuc
                && java.util.Objects.equals(maTaiKhoan, value.maTaiKhoan)
                && java.util.Objects.equals(vaiTro, value.vaiTro)
                && batBuocDoiMatKhau == value.batBuocDoiMatKhau;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(this.daXacThuc);
        result = 31 * result + java.util.Objects.hashCode(this.maTaiKhoan);
        result = 31 * result + java.util.Objects.hashCode(this.vaiTro);
        result = 31 * result + Boolean.hashCode(this.batBuocDoiMatKhau);
        return result;
    }

    @Override
    public String toString() {
        return "AuthDecision[" + "authenticated=" + daXacThuc + ", " + "accountId=" + maTaiKhoan + ", " + "role=" + vaiTro + ", " + "forcePasswordChange=" + batBuocDoiMatKhau + "]";
    }
}
