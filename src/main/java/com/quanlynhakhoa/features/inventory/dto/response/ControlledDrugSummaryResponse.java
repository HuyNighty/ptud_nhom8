package com.quanlynhakhoa.features.inventory.dto.response;

/**
 * Thẻ thống kê màn Thuốc kiểm soát đặc biệt.
 * {@code choDuyet} = phiếu xuất đang chờ có thuốc KSĐB (cần phê duyệt kép).
 */
public record ControlledDrugSummaryResponse(int tongLoai, int tongTon, int canHanHetHan, int choDuyet) {
}
