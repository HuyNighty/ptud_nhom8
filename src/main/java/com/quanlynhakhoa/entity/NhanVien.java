package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class NhanVien {
    private final String maNhanVien;
    private String hoTen;
    private String soDienThoai;
    private final String chucDanh;
    private final TrangThaiNhanVien trangThai;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String email;
    private final LocalDate ngayVaoLam;
    private String chuyenMon;
    private final LocalDate ngayNghiViec;

    public NhanVien(String maNhanVien, String hoTen, String soDienThoai, String chucDanh, TrangThaiNhanVien trangThai, LocalDate ngaySinh, String gioiTinh, String email, LocalDate ngayVaoLam, String chuyenMon, LocalDate ngayNghiViec) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.chucDanh = chucDanh;
        this.trangThai = trangThai;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.email = email;
        this.ngayVaoLam = ngayVaoLam;
        this.chuyenMon = chuyenMon;
        this.ngayNghiViec = ngayNghiViec;
    }

    public String getMaNhanVien() { return maNhanVien; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getChucDanh() { return chucDanh; }
    public TrangThaiNhanVien getTrangThai() { return trangThai; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public String getChuyenMon() { return chuyenMon; }
    public void setChuyenMon(String chuyenMon) { this.chuyenMon = chuyenMon; }
    public LocalDate getNgayNghiViec() { return ngayNghiViec; }
}
