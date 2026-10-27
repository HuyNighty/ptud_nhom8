package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import java.time.LocalDate;

/**
 * Một mặt hàng trong hộp cấp cứu.
 *
 * @param danhGia   đánh giá hiện tại theo số hiện có / hạn dùng
 * @param tonKhoDung tồn kho còn hạn trên ngưỡng của hộp — lượng tối đa lấy được khi bổ sung / thay thế
 */
public record EmergencyKitItemResponse(
        Integer maChiTiet,
        InventoryItemType loai,
        String maMatHang,
        String tenMatHang,
        String donViTinh,
        boolean kiemSoatDacBiet,
        int soLuongToiThieu,
        int soLuongHienCo,
        LocalDate hanDung,
        KitCheckResult danhGia,
        int tonKhoDung) {

    public int soLuongThieu() {
        return Math.max(0, soLuongToiThieu - soLuongHienCo);
    }
}
