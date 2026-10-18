package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;

/** Một mặt hàng trong thành phần hộp cấp cứu và số lượng tối thiểu phải có. */
public record EmergencyKitItemRequest(InventoryItemType loai, String maMatHang, int soLuongToiThieu) {
}
