package com.quanlynhakhoa.features.inventory.enums;

/** Kết quả một mục khi kiểm tra hộp cấp cứu (UC34). */
public enum KitCheckResult {
    DAT("Đạt"),
    THIEU("Thiếu"),
    CAN_HAN("Cận hạn"),
    HET_HAN("Hết hạn");

    private final String label;

    KitCheckResult(String label) {
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
