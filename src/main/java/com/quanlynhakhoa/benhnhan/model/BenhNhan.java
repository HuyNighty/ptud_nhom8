package com.quanlynhakhoa.benhnhan.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Lớp BenhNhan: mỗi đối tượng là 1 dòng trong bảng dbo.BenhNhan
public class BenhNhan {
    private String maBenhNhan;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;      // NAM hoặc NU
    private String soDienThoai;
    private String diaChi;
    private String nguoiLienHe;
    private LocalDateTime ngayTao;

    // Hàm khởi tạo rỗng
    public BenhNhan() {
    }

    // Hàm khởi tạo chỉ có mã
    public BenhNhan(String maBenhNhan) {
        this.maBenhNhan = maBenhNhan;
    }

    // Hàm khởi tạo đầy đủ
    public BenhNhan(String maBenhNhan, String hoTen, LocalDate ngaySinh, String gioiTinh,
                    String soDienThoai, String diaChi, String nguoiLienHe, LocalDateTime ngayTao) {
        this.maBenhNhan = maBenhNhan;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
        this.diaChi = diaChi;
        this.nguoiLienHe = nguoiLienHe;
        this.ngayTao = ngayTao;
    }

    public String getMaBenhNhan() {
        return maBenhNhan;
    }

    public void setMaBenhNhan(String maBenhNhan) {
        this.maBenhNhan = maBenhNhan;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getNguoiLienHe() {
        return nguoiLienHe;
    }

    public void setNguoiLienHe(String nguoiLienHe) {
        this.nguoiLienHe = nguoiLienHe;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    // Đổi mã giới tính trong database sang chữ để hiển thị
    public String getGioiTinhHienThi() {
        if ("NAM".equals(gioiTinh)) return "Nam";
        if ("NU".equals(gioiTinh)) return "Nữ";
        return "Khác";   // chỉ gặp với dữ liệu cũ trong database (ứng dụng không cho chọn nữa)
    }
}
