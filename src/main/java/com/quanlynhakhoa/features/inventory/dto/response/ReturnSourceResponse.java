package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import java.time.LocalDate;

/** Phiếu xuất đã hoàn thành còn có thể hoàn trả. */
public record ReturnSourceResponse(
        String maPhieuXuat,
        LocalDate ngayXuat,
        StockOutReason lyDoXuat,
        String nguoiNhan,
        int conDuocTra) {
}
