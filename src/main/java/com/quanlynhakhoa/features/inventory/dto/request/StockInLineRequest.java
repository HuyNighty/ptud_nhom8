package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng hàng trên phiếu nhập. Hạn sử dụng bắt buộc với thuốc, tùy chọn với vật tư. */
public record StockInLineRequest(
        InventoryItemType loai,
        String maMatHang,
        String soLo,
        LocalDate hanSuDung,
        int soLuong,
        BigDecimal donGia) {
}
