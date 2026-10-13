package vn.n08.nhakhoa.model;

import java.math.BigDecimal;

/** Bảng ChiTietPhieuNhap. */
public record ChiTietPhieuNhap(
        String maPhieuNhap,
        String maLo,
        int soLuong,
        BigDecimal donGiaNhap) {}
