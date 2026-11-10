package com.quanlynhakhoa.entity;

import java.time.LocalDateTime;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class PhieuNhapKho {
    private final String maPhieuNhap;
    private final LocalDateTime ngayNhap;
    private final String trangThai;
    private final String loaiNhap;
    private final String maNhaCungCap;
    private final String maPhieuXuatGoc;
    private final String maNhanVien;
    private final String ghiChu;

    public PhieuNhapKho(String maPhieuNhap, LocalDateTime ngayNhap, String trangThai, String loaiNhap, String maNhaCungCap, String maPhieuXuatGoc, String maNhanVien, String ghiChu) {
        this.maPhieuNhap = maPhieuNhap;
        this.ngayNhap = ngayNhap;
        this.trangThai = trangThai;
        this.loaiNhap = loaiNhap;
        this.maNhaCungCap = maNhaCungCap;
        this.maPhieuXuatGoc = maPhieuXuatGoc;
        this.maNhanVien = maNhanVien;
        this.ghiChu = ghiChu;
    }

    public String getMaPhieuNhap() { return maPhieuNhap; }
    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public String getTrangThai() { return trangThai; }
    public String getLoaiNhap() { return loaiNhap; }
    public String getMaNhaCungCap() { return maNhaCungCap; }
    public String getMaPhieuXuatGoc() { return maPhieuXuatGoc; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getGhiChu() { return ghiChu; }
}
