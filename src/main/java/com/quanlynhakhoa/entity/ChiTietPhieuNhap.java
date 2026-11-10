package com.quanlynhakhoa.entity;

import java.math.BigDecimal;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class ChiTietPhieuNhap {
    private final String maPhieuNhap;
    private final String maLo;
    private final int soLuong;
    private final BigDecimal donGiaNhap;

    public ChiTietPhieuNhap(String maPhieuNhap, String maLo, int soLuong, BigDecimal donGiaNhap) {
        this.maPhieuNhap = maPhieuNhap;
        this.maLo = maLo;
        this.soLuong = soLuong;
        this.donGiaNhap = donGiaNhap;
    }

    public String getMaPhieuNhap() { return maPhieuNhap; }
    public String getMaLo() { return maLo; }
    public int getSoLuong() { return soLuong; }
    public BigDecimal getDonGiaNhap() { return donGiaNhap; }
}
