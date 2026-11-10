package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.math.BigDecimal;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class DanhGiaNhanSu {
    private final String maDanhGia;
    private final String maNhanVien;
    private final String maNguoiDanhGia;
    private final String kyDanhGia;
    private final LocalDate ngayDanhGia;
    private BigDecimal diem;
    private String nhanXet;
    private final String trangThai;

    public DanhGiaNhanSu(String maDanhGia, String maNhanVien, String maNguoiDanhGia, String kyDanhGia, LocalDate ngayDanhGia, BigDecimal diem, String nhanXet, String trangThai) {
        this.maDanhGia = maDanhGia;
        this.maNhanVien = maNhanVien;
        this.maNguoiDanhGia = maNguoiDanhGia;
        this.kyDanhGia = kyDanhGia;
        this.ngayDanhGia = ngayDanhGia;
        setDiem(diem);
        this.nhanXet = nhanXet;
        this.trangThai = trangThai;
    }

    public String getMaDanhGia() { return maDanhGia; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getMaNguoiDanhGia() { return maNguoiDanhGia; }
    public String getKyDanhGia() { return kyDanhGia; }
    public LocalDate getNgayDanhGia() { return ngayDanhGia; }
    public BigDecimal getDiem() { return diem; }
    public String getNhanXet() { return nhanXet; }
    public void setNhanXet(String nhanXet) { this.nhanXet = nhanXet; }
    public String getTrangThai() { return trangThai; }

    public void setDiem(BigDecimal diem) {
        if (diem != null && (diem.compareTo(BigDecimal.ONE) < 0 || diem.compareTo(BigDecimal.valueOf(5)) > 0))
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        this.diem = diem;
    }
}
