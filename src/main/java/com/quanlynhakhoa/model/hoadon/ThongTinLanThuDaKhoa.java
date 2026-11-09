package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;

/** Dữ liệu bất biến của lần thu đang được khóa trong transaction. */
public final class ThongTinLanThuDaKhoa {
    private final String maHoaDon;
    private final String maTaiKhoan;
    private final BigDecimal soTien;
    private final String phuongThuc;
    private final String maGiaoDich;
    private final BigDecimal tienKhachDua;
    private final String trangThai;

    public ThongTinLanThuDaKhoa(String maHoaDon, String maTaiKhoan, BigDecimal soTien, String phuongThuc, String maGiaoDich, BigDecimal tienKhachDua, String trangThai) {
        this.maHoaDon = maHoaDon;
        this.maTaiKhoan = maTaiKhoan;
        this.soTien = soTien;
        this.phuongThuc = phuongThuc;
        this.maGiaoDich = maGiaoDich;
        this.tienKhachDua = tienKhachDua;
        this.trangThai = trangThai;
    }

    public String maHoaDon() { return maHoaDon; }
    public String maTaiKhoan() { return maTaiKhoan; }
    public BigDecimal soTien() { return soTien; }
    public String phuongThuc() { return phuongThuc; }
    public String maGiaoDich() { return maGiaoDich; }
    public BigDecimal tienKhachDua() { return tienKhachDua; }
    public String trangThai() { return trangThai; }
}
