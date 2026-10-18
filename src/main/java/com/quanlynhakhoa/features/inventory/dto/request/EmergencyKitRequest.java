package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import java.util.List;

/**
 * Tạo / sửa hộp cấp cứu.
 *
 * @param chuKyKiemTraNgay  số ngày giữa hai lần kiểm tra (mặc định 7)
 * @param nguongHanDungNgay mặt hàng còn hạn không quá số ngày này bị coi là cận hạn (mặc định 30)
 */
public record EmergencyKitRequest(
        String ten,
        EmergencyKitType loai,
        String viTri,
        int chuKyKiemTraNgay,
        int nguongHanDungNgay,
        String ghiChu,
        List<EmergencyKitItemRequest> matHang) {
}
