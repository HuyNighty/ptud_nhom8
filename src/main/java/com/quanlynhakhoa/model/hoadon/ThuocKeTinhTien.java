package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThuocKeTinhTien {
    private final String medicineId;
    private final String name;
    private final String unit;
    private final int quantity;
    private final BigDecimal unitPrice;

    public ThuocKeTinhTien(String medicineId, String name, String unit, int quantity, BigDecimal unitPrice) {
        this.medicineId = medicineId;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String medicineId() { return medicineId; }
    public String name() { return name; }
    public String unit() { return unit; }
    public int quantity() { return quantity; }
    public BigDecimal unitPrice() { return unitPrice; }

    public BigDecimal total() {
        return unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThuocKeTinhTien value)) return false;
        return java.util.Objects.equals(medicineId, value.medicineId)
                && java.util.Objects.equals(name, value.name)
                && java.util.Objects.equals(unit, value.unit)
                && quantity == value.quantity
                && java.util.Objects.equals(unitPrice, value.unitPrice);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(medicineId);
        result = 31 * result + java.util.Objects.hashCode(name);
        result = 31 * result + java.util.Objects.hashCode(unit);
        result = 31 * result + Integer.hashCode(quantity);
        result = 31 * result + java.util.Objects.hashCode(unitPrice);
        return result;
    }

    @Override
    public String toString() {
        return "PrescribedMedicine[" + "medicineId=" + medicineId + ", " + "name=" + name + ", " + "unit=" + unit + ", " + "quantity=" + quantity + ", " + "unitPrice=" + unitPrice + "]";
    }
}
