package com.quanlynhakhoa.features.inventory.enums;

/** Loại sự kiện trong nhật ký hộp cấp cứu. */
public enum KitLogType {
    KIEM_TRA("Kiểm tra"),
    BO_SUNG("Bổ sung"),
    THAY_THE("Thay thế");

    private final String label;

    KitLogType(String label) {
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
