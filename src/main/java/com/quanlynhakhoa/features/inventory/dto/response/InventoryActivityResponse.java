package com.quanlynhakhoa.features.inventory.dto.response;

/**
 * Số phiếu kho phát sinh trong ngày. {@code null} = chức năng tương ứng chưa có (UI hiện "—").
 */
public record InventoryActivityResponse(Integer nhapKho, Integer xuatKho, Integer hoanTra, Integer huyHaoHut) {
}
