package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng của phiếu hoàn trả (đơn giá = giá vốn trên dòng xuất gốc). */
public record StockReturnLineResponse(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int soLuong,
        BigDecimal donGia,
        BigDecimal thanhTien) {
}
