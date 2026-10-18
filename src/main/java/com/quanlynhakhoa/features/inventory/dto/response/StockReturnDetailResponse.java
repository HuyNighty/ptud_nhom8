package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StockReturnDetailResponse(
        String maPhieu,
        LocalDate ngayHoanTra,
        String maPhieuXuat,
        ReturnReason lyDo,
        String nguonHoanTra,
        String nguoiHoanTra,
        VoucherStatus trangThai,
        String ghiChu,
        String nguoiLap,
        LocalDateTime thoiDiemLap,
        String nguoiDuyet,
        LocalDateTime thoiDiemDuyet,
        String nguoiHuy,
        LocalDateTime thoiDiemHuy,
        String lyDoHuy,
        List<StockReturnLineResponse> dong,
        BigDecimal tongTien) {
}
