package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một dòng xuất theo lô. Dùng cho cả gợi ý FEFO (chưa lưu) và chi tiết phiếu.
 *
 * @param tonHienTai tồn hiện tại của lô (để người dùng đối chiếu)
 * @param donGia     giá vốn đã chốt khi xuất kho; null nếu phiếu chưa xuất
 */
public record StockOutLineResponse(
        Integer maLo,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        String soLo,
        LocalDate hanSuDung,
        int soLuong,
        int tonHienTai,
        boolean kiemSoatDacBiet,
        BigDecimal donGia,
        BigDecimal thanhTien) {
}
