package com.quanlynhakhoa.features.inventory.dto.request;

/**
 * Điều chỉnh nhanh thông tin theo dõi tồn của một mặt hàng (UC36). Không đổi số tồn.
 *
 * @param viTriLuuTru rỗng = chưa xếp vị trí
 */
public record StockMonitorAdjustRequest(int dinhMucToiThieu, String viTriLuuTru, boolean daDatHang) {
}
