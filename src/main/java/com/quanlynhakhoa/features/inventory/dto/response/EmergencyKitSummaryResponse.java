package com.quanlynhakhoa.features.inventory.dto.response;

import java.util.List;

/**
 * Khối thống kê hộp cấp cứu.
 *
 * @param tongLoaiMatHang số mặt hàng khác nhau được lưu trong các hộp
 * @param canhBao         câu cảnh báo cụ thể (hạn dùng trước, thiếu hàng sau)
 */
public record EmergencyKitSummaryResponse(
        int tongHop,
        int tongLoaiMatHang,
        int hopCanBoSung,
        int hopCanHetHan,
        int denHanKiemTra,
        List<String> canhBao) {
}
