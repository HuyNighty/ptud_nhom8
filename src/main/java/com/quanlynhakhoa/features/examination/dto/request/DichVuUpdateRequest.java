package com.quanlynhakhoa.features.examination.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DichVuUpdateRequest(
        @NotBlank @Size(max = 100) String tenDichVu,
        @Size(max = 500) String moTa,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal donGia,
        @NotNull Boolean laCanLamSang
) {
}
