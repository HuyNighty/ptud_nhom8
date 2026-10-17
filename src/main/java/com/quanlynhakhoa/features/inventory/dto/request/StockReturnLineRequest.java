package com.quanlynhakhoa.features.inventory.dto.request;

/** Trả {@code soLuong} của lô {@code maLo} (lô phải có trên phiếu xuất gốc). */
public record StockReturnLineRequest(Integer maLo, int soLuong) {
}
