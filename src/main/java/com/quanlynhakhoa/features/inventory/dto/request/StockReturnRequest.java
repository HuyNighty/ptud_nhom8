package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import java.time.LocalDate;
import java.util.List;

/** Dữ liệu lập/sửa phiếu hoàn trả. Phiếu xuất gốc không đổi được sau khi lập. */
public record StockReturnRequest(
        LocalDate ngayHoanTra,
        String maPhieuXuat,
        ReturnReason lyDo,
        String nguonHoanTra,
        String nguoiHoanTra,
        String ghiChu,
        List<StockReturnLineRequest> dong) {
}
