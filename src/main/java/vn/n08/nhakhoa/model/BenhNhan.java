package vn.n08.nhakhoa.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bảng BenhNhan — thông tin hành chính. */
public record BenhNhan(
        String maBenhNhan,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String soDienThoai,
        String diaChi,
        String nguoiLienHe,
        LocalDateTime ngayTao,
        String maTaiKhoanTao) {
    public int tuoi(LocalDate homNay) { return ngaySinh == null ? 0 : java.time.Period.between(ngaySinh, homNay).getYears(); }
    @Override public String toString() { return hoTen + " (" + maBenhNhan + ")"; }
}
