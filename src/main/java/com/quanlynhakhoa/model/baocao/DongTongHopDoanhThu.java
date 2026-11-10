package com.quanlynhakhoa.model.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng tổng hợp SQL; Service quyết định kỳ hiển thị và cộng số liệu. */
public final class DongTongHopDoanhThu {
    private final String loai;
    private final LocalDate ngay;
    private final int soHoaDon;
    private final BigDecimal soTien;

    public DongTongHopDoanhThu(String loai, LocalDate ngay, int soHoaDon, BigDecimal soTien) {
        this.loai = loai;
        this.ngay = ngay;
        this.soHoaDon = soHoaDon;
        this.soTien = soTien;
    }

    public String loai() { return loai; }
    public LocalDate ngay() { return ngay; }
    public int soHoaDon() { return soHoaDon; }
    public BigDecimal soTien() { return soTien; }
}
