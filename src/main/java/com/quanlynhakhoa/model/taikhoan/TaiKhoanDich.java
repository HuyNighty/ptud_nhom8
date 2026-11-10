package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;





/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TaiKhoanDich {
    private final String maTaiKhoan;
    private final VaiTroHeThong vaiTro;
    private final TrangThaiTaiKhoan trangThai;
    private final NguonKhoaTaiKhoan nguonKhoa;

    public TaiKhoanDich(String maTaiKhoan, VaiTroHeThong vaiTro, TrangThaiTaiKhoan trangThai, NguonKhoaTaiKhoan nguonKhoa) {
        this.maTaiKhoan = maTaiKhoan;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.nguonKhoa = nguonKhoa;
    }

    public String accountId() { return maTaiKhoan; }
    public VaiTroHeThong role() { return vaiTro; }
    public TrangThaiTaiKhoan status() { return trangThai; }
    public NguonKhoaTaiKhoan lockSource() { return nguonKhoa; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaiKhoanDich value)) return false;
        return java.util.Objects.equals(maTaiKhoan, value.maTaiKhoan)
                && java.util.Objects.equals(vaiTro, value.vaiTro)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(nguonKhoa, value.nguonKhoa);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoan);
        result = 31 * result + java.util.Objects.hashCode(vaiTro);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(nguonKhoa);
        return result;
    }

    @Override
    public String toString() {
        return "Target[" + "accountId=" + maTaiKhoan + ", " + "role=" + vaiTro + ", " + "status=" + trangThai + ", " + "lockSource=" + nguonKhoa + "]";
    }
}
