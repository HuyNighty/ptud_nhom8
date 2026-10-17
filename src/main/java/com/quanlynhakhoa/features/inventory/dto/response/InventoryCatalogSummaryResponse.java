package com.quanlynhakhoa.features.inventory.dto.response;

/**
 * Số liệu đầu màn "Vật tư & thuốc".
 *
 * @param coDuLieuTonKho {@code false} khi chưa có bảng lô hàng (M2): {@code soCanBoSung} chưa tính được.
 */
public record InventoryCatalogSummaryResponse(
        long tongMatHang,
        long soThuoc,
        long soVatTu,
        boolean coDuLieuTonKho,
        int soCanBoSung) {
}
