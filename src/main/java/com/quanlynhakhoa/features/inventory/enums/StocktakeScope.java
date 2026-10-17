package com.quanlynhakhoa.features.inventory.enums;

/** Phạm vi một đợt kiểm kê. */
public enum StocktakeScope {
    TAT_CA("Toàn bộ kho"),
    THUOC("Chỉ thuốc"),
    VAT_TU("Chỉ vật tư");

    private final String label;

    StocktakeScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
