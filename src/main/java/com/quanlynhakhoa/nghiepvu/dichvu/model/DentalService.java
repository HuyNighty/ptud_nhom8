package com.quanlynhakhoa.nghiepvu.dichvu.model;

import java.math.BigDecimal;

public record DentalService(String maDichVu, String tenDichVu, BigDecimal donGia,
                     String moTa, boolean laCanLamSang, boolean dangApDung) { }
