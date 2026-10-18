package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một lô trên phiếu xuất gốc và số lượng còn được trả.
 * {@code daTra} gồm cả phiếu hoàn trả đang chờ duyệt (không tính phiếu đã hủy và phiếu đang sửa).
 */
public record ReturnableLineResponse(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int daXuat,
        int daTra,
        int conDuocTra,
        BigDecimal donGia) {
}
