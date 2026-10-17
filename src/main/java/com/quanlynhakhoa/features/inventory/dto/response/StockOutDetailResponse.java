package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StockOutDetailResponse(
        String maPhieu,
        LocalDate ngayXuat,
        StockOutReason lyDo,
        String nguoiNhan,
        VoucherStatus trangThai,
        String ghiChu,
        String nguoiLap,
        LocalDateTime thoiDiemLap,
        String nguoiXuatKho,
        LocalDateTime thoiDiemXuatKho,
        String nguoiHuy,
        LocalDateTime thoiDiemHuy,
        String lyDoHuy,
        List<StockOutLineResponse> dong,
        BigDecimal tongTien) {

    public StockOutDetailResponse {
        dong = List.copyOf(dong);
    }
}
