package com.quanlynhakhoa.features.inventory.dto.response;

import java.util.List;

/** Lịch sử tồn của một mặt hàng: các lô đang còn và mọi biến động (cũ → mới). */
public record StockHistoryResponse(
        StockMonitorRowResponse matHang,
        List<StockLotViewResponse> lo,
        List<StockMovementResponse> bienDong) {
}
