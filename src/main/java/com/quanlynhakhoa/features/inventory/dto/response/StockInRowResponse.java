package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng trong danh sách phiếu nhập. */
public record StockInRowResponse(
        String maPhieu,
        LocalDate ngayNhap,
        String maNhaCungCap,
        String tenNhaCungCap,
        String nguoiLap,
        BigDecimal tongTien,
        StockInStatus trangThai) {
}
