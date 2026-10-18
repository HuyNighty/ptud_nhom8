package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;

/** Thẻ thống kê màn Hoàn trả kho. Giá trị = tổng giá vốn của các phiếu đã duyệt. */
public record StockReturnSummaryResponse(
        long tongPhieu,
        BigDecimal giaTriHoanTra,
        long soChoDuyet,
        long soHoanThanh,
        long soDaHuy) {
}
