package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import java.time.LocalDate;

/** Một mặt hàng trong nhật ký hộp cấp cứu; {@code ketQua} chỉ có ở lần kiểm tra. */
public record EmergencyKitLogLineResponse(
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        int soLuongTruoc,
        int soLuongSau,
        LocalDate hanDungSau,
        KitCheckResult ketQua) {
}
