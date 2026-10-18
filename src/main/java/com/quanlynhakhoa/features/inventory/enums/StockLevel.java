package com.quanlynhakhoa.features.inventory.enums;

/** Mức tồn khả dụng so với định mức tối thiểu (UC36). */
public enum StockLevel {
    /** Tồn khả dụng bằng 0. */
    HET_HANG("Hết hàng"),
    /** Tồn thấp hơn định mức — cần đặt hàng. */
    THIEU("Thiếu"),
    /** Tồn còn trên định mức nhưng nằm trong vùng đệm cảnh báo. */
    SAP_HET("Sắp hết"),
    /** Tồn đủ. */
    DU("Còn hàng");

    private final String label;

    StockLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

}
