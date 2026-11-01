package com.quanlynhakhoa.nghiepvu.phieukham.model;

import java.time.LocalDateTime;

public record PhieuKham(
        String maPhieuKham,
        String maBenhNhan,
        String tenBenhNhan,
        String soDienThoai,
        String maHoSo,
        String maLichHen,
        LocalDateTime thoiGianTiepNhan,
        String maNhanVien,
        String tenBacSi,
        String trieuChung,
        String chanDoan,
        String ketLuan,
        String trangThai,
        String tienSuBenh,
        String diUng) {
}
