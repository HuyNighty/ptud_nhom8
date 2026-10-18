package com.quanlynhakhoa.features.inventory.dto.response;

import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import java.time.LocalDate;

/** Một lô còn tồn của thuốc kiểm soát đặc biệt. */
public record ControlledLotResponse(Integer maLo, String soLo, LocalDate hanSuDung, int tonKho, ExpiryLevel mucHan) {
}
