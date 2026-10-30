package com.quanlynhakhoa.lichhen.model;

public class BacSi {
    private String maNhanVien;
    private String hoTen;

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

    @Override
    public String toString() {
        return hoTen + " (" + maNhanVien + ")";
    }
}
