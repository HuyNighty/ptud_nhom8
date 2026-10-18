package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import java.time.LocalDate;

/**
 * Một dòng trong danh mục Thuốc & vật tư. Các trường không áp dụng cho loại thì là null/false.
 *
 * @param tonKho            tồn khả dụng = tổng tồn các lô chưa hết hạn
 * @param hanSuDungGanNhat  hạn gần nhất trong các lô còn tồn và chưa hết hạn (null nếu không có)
 * @param mucTon            phân loại tồn so với định mức
 */
public record InventoryItemResponse(
        InventoryItemType loai,
        String ma,
        String ten,
        String donViTinh,
        int dinhMucToiThieu,
        boolean daDatHang,
        String hamLuong,
        String hoatChat,
        boolean kiemSoatDacBiet,
        String moTa,
        int tonKho,
        LocalDate hanSuDungGanNhat,
        StockLevel mucTon) {
}
