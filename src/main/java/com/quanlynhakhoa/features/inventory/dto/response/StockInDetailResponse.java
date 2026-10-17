package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StockInDetailResponse(
        String maPhieu,
        LocalDate ngayNhap,
        String maNhaCungCap,
        String tenNhaCungCap,
        StockInStatus trangThai,
        String ghiChu,
        String nguoiLap,
        LocalDateTime thoiDiemLap,
        String nguoiNhapKho,
        LocalDateTime thoiDiemNhapKho,
        String nguoiHuy,
        LocalDateTime thoiDiemHuy,
        String lyDoHuy,
        List<StockInLineResponse> dong,
        BigDecimal tongTien) {

    public StockInDetailResponse {
        dong = List.copyOf(dong);
    }
}
