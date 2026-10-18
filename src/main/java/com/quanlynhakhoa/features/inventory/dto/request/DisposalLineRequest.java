package com.quanlynhakhoa.features.inventory.dto.request;

/** Hủy {@code soLuong} của lô {@code maLo}. */
public record DisposalLineRequest(Integer maLo, int soLuong) {
}
