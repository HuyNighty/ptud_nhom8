package com.quanlynhakhoa.features.inventory.dto.request;

/** Lấy {@code soLuong} từ kho cho một mặt hàng của hộp. */
public record KitRestockLineRequest(Integer maChiTiet, int soLuong) {
}
