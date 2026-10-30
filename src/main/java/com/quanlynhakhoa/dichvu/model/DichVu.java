package com.quanlynhakhoa.dichvu.model;

import java.math.BigDecimal;

public record DichVu(String maDichVu, String tenDichVu, BigDecimal donGia,
                     String moTa, boolean laCanLamSang, boolean dangApDung) { }
