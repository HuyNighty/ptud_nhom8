package com.quanlynhakhoa.features.inventory.dto.request;

import java.time.LocalDate;
import java.util.List;

public record StockInRequest(
        LocalDate ngayNhap,
        String maNhaCungCap,
        String ghiChu,
        List<StockInLineRequest> dong) {
}
