package com.quanlynhakhoa.benhnhan.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một dòng của bảng dbo.BenhNhan (V5).
 * maTaiKhoanTao: tài khoản đã tạo hồ sơ (truy vết), có thể null với dữ liệu cũ.
 */
public record BenhNhan(
        String maBenhNhan,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String soDienThoai,
        String diaChi,
        String nguoiLienHe,
        LocalDateTime ngayTao,
        String maTaiKhoanTao) {
}
