package com.quanlynhakhoa.nghiepvu.phieukham.entity;

import java.time.LocalDateTime;

public record PhieuKhamListItem(
        String maPhieuKham,
        String maBenhNhan,
        String tenBenhNhan,
        String soDienThoai,
        LocalDateTime thoiGianTiepNhan,
        String maNhanVien,
        String tenBacSi,
        String trangThai) {
}
