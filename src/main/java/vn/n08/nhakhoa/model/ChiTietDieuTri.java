package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng ChiTietDieuTri — hạng mục của kế hoạch (khóa IDENTITY). */
public record ChiTietDieuTri(
        Integer maChiTiet,
        String maKeHoach,
        String maDichVu,
        String viTriRang,
        LocalDate ngayDuKien,
        LocalDate ngayThucHien,
        String ketQua,
        TrangThaiDieuTri trangThai,
        String maNhanVienThucHien,
        String ghiChu) {}
