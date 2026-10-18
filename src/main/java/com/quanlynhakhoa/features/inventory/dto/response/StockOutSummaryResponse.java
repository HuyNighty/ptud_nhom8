package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;

/** Thẻ thống kê màn Xuất kho. Giá trị xuất tính theo giá vốn của phiếu đã xuất, theo ngày xuất. */
public record StockOutSummaryResponse(
        long tongPhieu,
        BigDecimal giaTriThangNay,
        long soChoXuLy,
        long soHoanThanh,
        long soDaHuy) {
}
