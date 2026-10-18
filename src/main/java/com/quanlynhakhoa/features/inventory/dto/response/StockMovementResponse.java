package com.quanlynhakhoa.features.inventory.dto.response;

import java.time.LocalDateTime;

/** Một biến động tồn đã ghi sổ của mặt hàng; {@code tonSau} = tồn của mặt hàng sau dòng này. */
public record StockMovementResponse(
        LocalDateTime thoiDiem,
        String maChungTu,
        String loai,
        String soLo,
        int nhap,
        int xuat,
        int tonSau,
        String doiTac,
        String nguoiThucHien) {
}
