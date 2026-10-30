package com.quanlynhakhoa.benhnhan.hopdong;

import java.time.LocalDate;

public class BenhNhanTomTat {
    private final String maBenhNhan;
    private final String hoTen;
    private final LocalDate ngaySinh;
    private final String gioiTinh;
    private final String soDienThoai;

    public BenhNhanTomTat(String maBenhNhan, String hoTen, LocalDate ngaySinh,
                          String gioiTinh, String soDienThoai) {
        this.maBenhNhan = maBenhNhan;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
    }

    public String getMaBenhNhan() {
        return maBenhNhan;
    }

    public String getHoTen() {
        return hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public String getGioiTinhHienThi() {
        if ("NAM".equals(gioiTinh)) {
            return "Nam";
        }
        if ("NU".equals(gioiTinh)) {
            return "Nữ";
        }
        return "Khác";
    }

    @Override
    public String toString() {
        return maBenhNhan + " - " + hoTen + " - " + soDienThoai;
    }
}
