package com.quanlynhakhoa.features.inventory.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu màn hình "Quản lý kho — Tổng quan".
 *
 * @param prototypeData {@code true} khi dữ liệu là mẫu, chưa đọc từ SQL Server.
 *                      UI bắt buộc hiển thị nhãn prototype khi cờ này bật.
 */
public record InventoryOverviewResponse(
        LocalDate ngayXem,
        int tongMatHang,
        int soMatHangSapHetTon,
        int soLoCanXuLyGap,
        int soPhieuChoXuLy,
        List<LowStockItemResponse> matHangSapHetTon,
        List<ExpiringLotResponse> loTheoDoiHan,
        InventoryActivityResponse hoatDongHomNay,
        List<AttentionNoticeResponse> canChuY,
        boolean prototypeData) {

    public InventoryOverviewResponse {
        matHangSapHetTon = List.copyOf(matHangSapHetTon);
        loTheoDoiHan = List.copyOf(loTheoDoiHan);
        canChuY = List.copyOf(canChuY);
    }
}
