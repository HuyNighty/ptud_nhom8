package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongDanhSachTaiKhoan {
    private final String maTaiKhoan;
    private final String tenDangNhap;
    private final String tenHienThi;
    private final String maNhanVien;
    private final String tenNhanVien;
    private final VaiTroHeThong vaiTro;
    private final TrangThaiTaiKhoan trangThai;
    private final boolean canDoiMatKhau;
    private final LocalDateTime thoiGianDangNhapCuoiUtc;
    private final NguonKhoaTaiKhoan nguonKhoa;

    public DongDanhSachTaiKhoan(String maTaiKhoan, String tenDangNhap, String tenHienThi, String maNhanVien, String tenNhanVien, VaiTroHeThong vaiTro, TrangThaiTaiKhoan trangThai, boolean canDoiMatKhau, LocalDateTime thoiGianDangNhapCuoiUtc, NguonKhoaTaiKhoan nguonKhoa) {
        this.maTaiKhoan = maTaiKhoan;
        this.tenDangNhap = tenDangNhap;
        this.tenHienThi = tenHienThi;
        this.maNhanVien = maNhanVien;
        this.tenNhanVien = tenNhanVien;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.canDoiMatKhau = canDoiMatKhau;
        this.thoiGianDangNhapCuoiUtc = thoiGianDangNhapCuoiUtc;
        this.nguonKhoa = nguonKhoa;
    }

    public String accountId() { return maTaiKhoan; }
    public String username() { return tenDangNhap; }
    public String displayName() { return tenHienThi; }
    public String employeeId() { return maNhanVien; }
    public String employeeName() { return tenNhanVien; }
    public VaiTroHeThong role() { return vaiTro; }
    public TrangThaiTaiKhoan status() { return trangThai; }
    public boolean passwordChangeRequired() { return canDoiMatKhau; }
    public LocalDateTime lastLoginUtc() { return thoiGianDangNhapCuoiUtc; }
    public NguonKhoaTaiKhoan lockSource() { return nguonKhoa; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongDanhSachTaiKhoan value)) return false;
        return java.util.Objects.equals(maTaiKhoan, value.maTaiKhoan)
                && java.util.Objects.equals(tenDangNhap, value.tenDangNhap)
                && java.util.Objects.equals(tenHienThi, value.tenHienThi)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(tenNhanVien, value.tenNhanVien)
                && java.util.Objects.equals(vaiTro, value.vaiTro)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && canDoiMatKhau == value.canDoiMatKhau
                && java.util.Objects.equals(thoiGianDangNhapCuoiUtc, value.thoiGianDangNhapCuoiUtc)
                && java.util.Objects.equals(nguonKhoa, value.nguonKhoa);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoan);
        result = 31 * result + java.util.Objects.hashCode(tenDangNhap);
        result = 31 * result + java.util.Objects.hashCode(tenHienThi);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(tenNhanVien);
        result = 31 * result + java.util.Objects.hashCode(vaiTro);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + Boolean.hashCode(canDoiMatKhau);
        result = 31 * result + java.util.Objects.hashCode(thoiGianDangNhapCuoiUtc);
        result = 31 * result + java.util.Objects.hashCode(nguonKhoa);
        return result;
    }

    @Override
    public String toString() {
        return "AccountDirectoryRow[" + "accountId=" + maTaiKhoan + ", " + "username=" + tenDangNhap + ", " + "displayName=" + tenHienThi + ", " + "employeeId=" + maNhanVien + ", " + "employeeName=" + tenNhanVien + ", " + "role=" + vaiTro + ", " + "status=" + trangThai + ", " + "passwordChangeRequired=" + canDoiMatKhau + ", " + "lastLoginUtc=" + thoiGianDangNhapCuoiUtc + ", " + "lockSource=" + nguonKhoa + "]";
    }
}
