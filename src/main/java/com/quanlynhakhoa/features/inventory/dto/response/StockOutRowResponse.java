package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng trong danh sách phiếu xuất. {@code tongTien} = 0 khi chưa xuất kho. */
public record StockOutRowResponse(
        String maPhieu,
        LocalDate ngayXuat,
        StockOutReason lyDo,
        String nguoiNhan,
        String nguoiLap,
        BigDecimal tongTien,
        VoucherStatus trangThai) {
}
