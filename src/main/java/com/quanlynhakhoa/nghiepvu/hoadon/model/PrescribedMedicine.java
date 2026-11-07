package com.quanlynhakhoa.nghiepvu.hoadon.model;

import java.math.BigDecimal;

/** Billing information only; a null price means the medicine has not been invoiced. */
public record PrescribedMedicine(String medicineId, String name, String unit,
                                 int quantity, BigDecimal unitPrice) {
    public BigDecimal total() {
        return unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
