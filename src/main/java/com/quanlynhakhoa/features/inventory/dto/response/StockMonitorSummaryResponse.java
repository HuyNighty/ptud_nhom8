package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;

/**
 * Khối thống kê màn Tồn kho.
 *
 * @param duoiDinhMuc mặt hàng thiếu hoặc hết hàng
 */
public record StockMonitorSummaryResponse(
        int tongMatHang,
        long tongSoLuongTon,
        BigDecimal giaTriTonKho,
        int duoiDinhMuc,
        int sapHet,
        int duTon) {
}
