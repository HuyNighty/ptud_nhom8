package com.quanlynhakhoa.features.inventory.enums;

/** Trạng thái phiếu nhập kho: CHO_XU_LY → DA_HOAN_THANH (đã cộng tồn) hoặc DA_HUY (có lý do). */
public enum StockInStatus {
    CHO_XU_LY("Chờ xử lý"),
    DA_HOAN_THANH("Đã hoàn thành"),
    DA_HUY("Đã hủy");

    private final String label;

    StockInStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
