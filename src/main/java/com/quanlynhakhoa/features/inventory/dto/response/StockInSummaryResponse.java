package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;

/**
 * Thẻ thống kê màn Nhập kho. Giá trị nhập chỉ tính phiếu đã hoàn thành, theo ngày nhập.
 */
public record StockInSummaryResponse(
        long tongPhieu,
        BigDecimal giaTriThangNay,
        BigDecimal giaTriThangTruoc,
        long soChoXuLy,
        long soHoanThanh,
        long soDaHuy) {
}
