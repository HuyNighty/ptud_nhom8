package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng NhanVien — lớp NhanVien trong class diagram. */
public record NhanVien(
        String maNhanVien,
        String hoTen,
        String soDienThoai,
        VaiTro chucDanh,
        TrangThaiNhanVien trangThai,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String email,
        LocalDate ngayVaoLam,
        String chuyenMon,
        LocalDate ngayNghiViec) {
    public boolean dangLamViec() { return trangThai == TrangThaiNhanVien.DANG_LAM_VIEC; }
    @Override public String toString() { return hoTen + " (" + maNhanVien + ")"; }
}
