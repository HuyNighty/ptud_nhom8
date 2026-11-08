package com.quanlynhakhoa.nghiepvu.phieukham.entity;

public record ChiTietDonThuoc(
        String maThuoc,
        String tenThuoc,
        String donViTinh,
        int soLuong,
        String lieuDung,
        String cachDung,
        int soNgayDung,
        boolean kiemSoatDacBiet,
        String hoatChat) {
}
