package com.quanlynhakhoa.features.inventory.dto.request;

/** Số đếm thực tế của một lô. {@code soLuongThucTe} null = chưa đếm. */
public record StocktakeCountRequest(Integer maLo, Integer soLuongThucTe, String ghiChu) {
}
