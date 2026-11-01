package com.quanlynhakhoa.nghiepvu.phieukham.model;

import java.time.LocalDateTime;

public record ChiDinhCanLamSang(
        String maChiDinh,
        String maPhieuKham,
        String maDichVu,
        String tenDichVu,
        String yeuCau,
        String ketQua,
        String maNhanVien,
        String tenBacSi,
        LocalDateTime thoiGianChiDinh,
        LocalDateTime thoiGianKetQua,
        String trangThai) {
}
