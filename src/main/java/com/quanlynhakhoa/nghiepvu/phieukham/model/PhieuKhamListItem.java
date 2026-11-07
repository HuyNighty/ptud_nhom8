package com.quanlynhakhoa.nghiepvu.phieukham.model;

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
