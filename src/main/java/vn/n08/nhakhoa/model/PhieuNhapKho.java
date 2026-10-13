package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng PhieuNhapKho (nhập từ NCC hoặc hoàn trả). */
public record PhieuNhapKho(
        String maPhieuNhap,
        LocalDateTime ngayNhap,
        String trangThai,
        LoaiNhap loaiNhap,
        String maNhaCungCap,
        String maPhieuXuatGoc,
        String maNhanVien,
        String ghiChu) {}
