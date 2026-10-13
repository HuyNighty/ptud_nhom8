package vn.n08.nhakhoa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bảng DanhGiaNhanSu. diem = trung bình các tiêu chí (ChiTietDanhGia). */
public record DanhGiaNhanSu(
        String maDanhGia,
        String maNhanVien,
        String maNguoiDanhGia,
        String kyDanhGia,
        LocalDate ngayDanhGia,
        BigDecimal diem,
        String nhanXet,
        TrangThaiDanhGia trangThai) {}
