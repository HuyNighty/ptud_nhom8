package com.quanlynhakhoa.features.inventory.dto.response;

import java.math.BigDecimal;

/** Thẻ thống kê màn Hủy / hao hụt. Giá trị tháng này = phiếu đã duyệt có ngày hủy trong tháng. */
public record DisposalSummaryResponse(
        long tongPhieu,
        BigDecimal giaTriThangNay,
        long soChoDuyet,
        long soHoanThanh,
        long soDaHuy) {
}
