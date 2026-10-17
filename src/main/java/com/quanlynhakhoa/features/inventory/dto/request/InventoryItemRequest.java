package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;

/**
 * Dữ liệu nhập khi thêm/sửa mặt hàng. Kiểm tra hợp lệ nằm ở InventoryItemService
 * (Swing gọi Service trực tiếp nên không dựa vào validation của REST).
 *
 * @param hamLuong        chỉ dùng cho THUOC
 * @param hoatChat        chỉ dùng cho THUOC
 * @param kiemSoatDacBiet chỉ dùng cho THUOC
 * @param moTa            chỉ dùng cho VAT_TU
 */
public record InventoryItemRequest(
        InventoryItemType loai,
        String ma,
        String ten,
        String donViTinh,
        int dinhMucToiThieu,
        String hamLuong,
        String hoatChat,
        boolean kiemSoatDacBiet,
        String moTa) {
}
