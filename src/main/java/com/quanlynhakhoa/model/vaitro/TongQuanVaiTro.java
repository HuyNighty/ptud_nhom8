package com.quanlynhakhoa.model.vaitro;

import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Quyen;
import java.util.Set;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TongQuanVaiTro {
    private final VaiTroHeThong vaiTro;
    private final Set<Quyen> cacQuyen;
    private final int soTaiKhoan;

    public TongQuanVaiTro(VaiTroHeThong vaiTro, Set<Quyen> cacQuyen, int soTaiKhoan) {
        if (vaiTro == null || cacQuyen == null || soTaiKhoan < 0) {
        throw new IllegalArgumentException("Invalid role overview");
        }
        cacQuyen = Set.copyOf(cacQuyen);
        this.vaiTro = vaiTro;
        this.cacQuyen = cacQuyen;
        this.soTaiKhoan = soTaiKhoan;
    }

    public VaiTroHeThong role() { return vaiTro; }
    public Set<Quyen> permissions() { return cacQuyen; }
    public int accountCount() { return soTaiKhoan; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TongQuanVaiTro value)) return false;
        return java.util.Objects.equals(vaiTro, value.vaiTro)
                && java.util.Objects.equals(cacQuyen, value.cacQuyen)
                && soTaiKhoan == value.soTaiKhoan;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(vaiTro);
        result = 31 * result + java.util.Objects.hashCode(cacQuyen);
        result = 31 * result + Integer.hashCode(soTaiKhoan);
        return result;
    }

    @Override
    public String toString() {
        return "RoleOverview[" + "role=" + vaiTro + ", " + "permissions=" + cacQuyen + ", " + "accountCount=" + soTaiKhoan + "]";
    }
}
