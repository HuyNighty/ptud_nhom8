package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockLevel;

public record LowStockItemResponse(
        String maMatHang,
        String tenMatHang,
        int soLuongTon,
        int dinhMucToiThieu,
        StockLevel mucTon) {
}
