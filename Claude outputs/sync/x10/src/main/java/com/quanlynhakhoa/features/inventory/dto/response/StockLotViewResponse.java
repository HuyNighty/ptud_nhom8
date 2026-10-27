package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một lô còn tồn của mặt hàng; {@code donGiaNhap} null nếu chưa có phiếu nhập gắn lô. */
public record StockLotViewResponse(
        Integer maLo,
        String soLo,
        LocalDate hanSuDung,
        int soLuongTon,
        BigDecimal donGiaNhap,
        String nhaCungCap) {
}
