package com.quanlynhakhoa.benhnhan.model;

/** Giá trị khớp ràng buộc CK_BenhNhan_GioiTinh trong V5: NAM, NU, KHAC. */
public enum GioiTinh {
    NAM("Nam"),
    NU("Nữ"),
    KHAC("Khác");

    private final String tenHienThi;

    GioiTinh(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String tenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }
}
