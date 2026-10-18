package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import java.time.LocalDate;
import java.util.List;

public record StockOutRequest(
        LocalDate ngayXuat,
        StockOutReason lyDo,
        String nguoiNhan,
        String ghiChu,
        List<StockOutLineRequest> dong) {
}
