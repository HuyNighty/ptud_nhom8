package com.quanlynhakhoa.nghiepvu.phieukham.model;

import java.math.BigDecimal;

public record DichVuCanLamSang(
        String maDichVu,
        String tenDichVu,
        BigDecimal donGia,
        String moTa) {

    @Override
    public String toString() {
        return maDichVu + " - " + tenDichVu;
    }
}
