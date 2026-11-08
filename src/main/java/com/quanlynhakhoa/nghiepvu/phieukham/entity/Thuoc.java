package com.quanlynhakhoa.nghiepvu.phieukham.entity;

public record Thuoc(
        String maThuoc,
        String tenThuoc,
        String donViTinh,
        boolean kiemSoatDacBiet,
        String hamLuong,
        String hoatChat) {

    @Override
    public String toString() {
        String strength = hamLuong == null || hamLuong.isBlank()
                ? "" : " (" + hamLuong.trim() + ")";
        return tenThuoc + strength + " - " + donViTinh;
    }
}
