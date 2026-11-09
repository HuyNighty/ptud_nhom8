package com.quanlynhakhoa.model.nhanvien;

import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongDanhSachNhanVien {
    private final String maNhanVien;
    private final String hoTen;
    private final String chucDanh;
    private final TrangThaiNhanVien trangThai;
    private final LocalDate ngayVaoLam;
    private final String maTaiKhoan;
    private final VaiTroHeThong vaiTroTaiKhoan;

    public DongDanhSachNhanVien(String maNhanVien, String hoTen, String chucDanh, TrangThaiNhanVien trangThai, LocalDate ngayVaoLam, String maTaiKhoan, VaiTroHeThong vaiTroTaiKhoan) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.chucDanh = chucDanh;
        this.trangThai = trangThai;
        this.ngayVaoLam = ngayVaoLam;
        this.maTaiKhoan = maTaiKhoan;
        this.vaiTroTaiKhoan = vaiTroTaiKhoan;
    }

    public String employeeId() { return maNhanVien; }
    public String fullName() { return hoTen; }
    public String jobTitle() { return chucDanh; }
    public TrangThaiNhanVien status() { return trangThai; }
    public LocalDate joinedAt() { return ngayVaoLam; }
    public String accountId() { return maTaiKhoan; }
    public VaiTroHeThong accountRole() { return vaiTroTaiKhoan; }

    public boolean hasAccount() {
        return maTaiKhoan != null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongDanhSachNhanVien value)) return false;
        return java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(hoTen, value.hoTen)
                && java.util.Objects.equals(chucDanh, value.chucDanh)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(ngayVaoLam, value.ngayVaoLam)
                && java.util.Objects.equals(maTaiKhoan, value.maTaiKhoan)
                && java.util.Objects.equals(vaiTroTaiKhoan, value.vaiTroTaiKhoan);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(hoTen);
        result = 31 * result + java.util.Objects.hashCode(chucDanh);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(ngayVaoLam);
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoan);
        result = 31 * result + java.util.Objects.hashCode(vaiTroTaiKhoan);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeDirectoryRow[" + "employeeId=" + maNhanVien + ", " + "fullName=" + hoTen + ", " + "jobTitle=" + chucDanh + ", " + "status=" + trangThai + ", " + "joinedAt=" + ngayVaoLam + ", " + "accountId=" + maTaiKhoan + ", " + "accountRole=" + vaiTroTaiKhoan + "]";
    }
}
