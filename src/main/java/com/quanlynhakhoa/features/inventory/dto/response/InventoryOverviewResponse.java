package com.quanlynhakhoa.features.inventory.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu màn hình "Quản lý kho — Tổng quan". Chỉ chứa số liệu thật từ SQL Server.
 *
 * @param coDuLieuTonKho {@code false} khi chưa có bảng lô hàng/phiếu kho (M2): các số
 *                       tồn, hạn dùng, phiếu và hoạt động trong ngày chưa tính được, UI hiện "—".
 */
public record InventoryOverviewResponse(
        LocalDate ngayXem,
        long tongMatHang,
        long soThuoc,
        long soVatTu,
        long soThuocKiemSoat,
        boolean coDuLieuTonKho,
        int soMatHangSapHetTon,
        int soLoCanXuLyGap,
        int soPhieuChoXuLy,
        List<LowStockItemResponse> matHangSapHetTon,
        List<ExpiringLotResponse> loTheoDoiHan,
        InventoryActivityResponse hoatDongHomNay,
        List<AttentionNoticeResponse> canChuY) {

    public InventoryOverviewResponse {
        matHangSapHetTon = List.copyOf(matHangSapHetTon);
        loTheoDoiHan = List.copyOf(loTheoDoiHan);
        canChuY = List.copyOf(canChuY);
    }
}
