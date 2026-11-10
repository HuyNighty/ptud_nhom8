package com.quanlynhakhoa.model.nhanvien;

import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class YeuCauTaoNhanVien {
    private final String maNhanVien;
    private final String hoTen;
    private final String soDienThoai;
    private final String chucDanh;
    private final LocalDate ngaySinh;
    private final String gioiTinh;
    private final String email;
    private final LocalDate ngayVaoLam;
    private final String chuyenMon;

    public YeuCauTaoNhanVien(String maNhanVien, String hoTen, String soDienThoai, String chucDanh, LocalDate ngaySinh, String gioiTinh, String email, LocalDate ngayVaoLam, String chuyenMon) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.chucDanh = chucDanh;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.email = email;
        this.ngayVaoLam = ngayVaoLam;
        this.chuyenMon = chuyenMon;
    }

    public String employeeId() { return maNhanVien; }
    public String fullName() { return hoTen; }
    public String phone() { return soDienThoai; }
    public String jobTitle() { return chucDanh; }
    public LocalDate birthDate() { return ngaySinh; }
    public String gender() { return gioiTinh; }
    public String email() { return email; }
    public LocalDate joinedAt() { return ngayVaoLam; }
    public String specialty() { return chuyenMon; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof YeuCauTaoNhanVien value)) return false;
        return java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(hoTen, value.hoTen)
                && java.util.Objects.equals(soDienThoai, value.soDienThoai)
                && java.util.Objects.equals(chucDanh, value.chucDanh)
                && java.util.Objects.equals(ngaySinh, value.ngaySinh)
                && java.util.Objects.equals(gioiTinh, value.gioiTinh)
                && java.util.Objects.equals(email, value.email)
                && java.util.Objects.equals(ngayVaoLam, value.ngayVaoLam)
                && java.util.Objects.equals(chuyenMon, value.chuyenMon);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(hoTen);
        result = 31 * result + java.util.Objects.hashCode(soDienThoai);
        result = 31 * result + java.util.Objects.hashCode(chucDanh);
        result = 31 * result + java.util.Objects.hashCode(ngaySinh);
        result = 31 * result + java.util.Objects.hashCode(gioiTinh);
        result = 31 * result + java.util.Objects.hashCode(email);
        result = 31 * result + java.util.Objects.hashCode(ngayVaoLam);
        result = 31 * result + java.util.Objects.hashCode(chuyenMon);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeCreateRequest[" + "employeeId=" + maNhanVien + ", " + "fullName=" + hoTen + ", " + "phone=" + soDienThoai + ", " + "jobTitle=" + chucDanh + ", " + "birthDate=" + ngaySinh + ", " + "gender=" + gioiTinh + ", " + "email=" + email + ", " + "joinedAt=" + ngayVaoLam + ", " + "specialty=" + chuyenMon + "]";
    }
}
