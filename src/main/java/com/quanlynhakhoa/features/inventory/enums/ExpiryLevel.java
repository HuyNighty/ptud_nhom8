package com.quanlynhakhoa.features.inventory.enums;

/** Mức cảnh báo hạn sử dụng của lô (UC37). */
public enum ExpiryLevel {
    DA_HET_HAN("Hết hạn"),
    CON_TOI_DA_7_NGAY("Cận hạn"),
    CON_TOI_DA_30_NGAY("Sắp hết hạn"),
    CON_HAN("Còn hạn");

    private final String label;

    ExpiryLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
