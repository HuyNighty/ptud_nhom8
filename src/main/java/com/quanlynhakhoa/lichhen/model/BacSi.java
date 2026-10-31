package com.quanlynhakhoa.lichhen.model;

public class BacSi {
    private String maNhanVien;
    private String hoTen;
    private int soNguoiDangCho = -1;

    public BacSi(String maNhanVien, String hoTen) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public String getHoTen() {
        return hoTen;
    }

    public int getSoNguoiDangCho() {
        return soNguoiDangCho;
    }

    public void setSoNguoiDangCho(int soNguoiDangCho) {
        this.soNguoiDangCho = soNguoiDangCho;
    }

    @Override
    public String toString() {
        if (soNguoiDangCho < 0) {
            return hoTen + " (" + maNhanVien + ")";
        }
        return hoTen + " (" + maNhanVien + ") - " + soNguoiDangCho + " người đang chờ";
    }
}
