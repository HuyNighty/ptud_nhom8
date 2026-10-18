package com.quanlynhakhoa.features.inventory.dto.request;

/** Một dòng phiếu xuất: xuất {@code soLuong} từ lô {@code maLo} (thường lấy từ gợi ý FEFO). */
public record StockOutLineRequest(Integer maLo, int soLuong) {
}
