package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import java.time.LocalDate;

public record ExpiringLotResponse(
        String soLo,
        String tenSanPham,
        LocalDate hanSuDung,
        int soLuong,
        String donViTinh,
        ExpiryLevel mucCanhBao) {
}
