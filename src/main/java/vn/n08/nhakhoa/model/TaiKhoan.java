package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng TaiKhoan. matKhau là chuỗi băm PBKDF2, không bao giờ hiển thị. */
public record TaiKhoan(
        String maTaiKhoan,
        String tenDangNhap,
        String matKhau,
        TrangThaiTaiKhoan trangThai,
        VaiTro vaiTro,
        String maNhanVien,
        int soLanSai,
        LocalDateTime lanDangNhapCuoi,
        String lyDoKhoa) {}
