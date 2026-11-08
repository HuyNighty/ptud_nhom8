package com.quanlynhakhoa.nghiepvu.phieukham.entity;

public record BacSi(String maNhanVien, String hoTen) {
    public BacSi {
        maNhanVien = maNhanVien == null ? "" : maNhanVien.trim();
        hoTen = hoTen == null ? "" : hoTen.trim();
    }

    @Override
    public String toString() {
        if (maNhanVien.isBlank()) return hoTen;
        return maNhanVien + " - BS. " + hoTen;
    }
}
