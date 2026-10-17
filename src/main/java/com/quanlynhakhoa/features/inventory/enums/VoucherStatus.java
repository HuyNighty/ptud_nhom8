package com.quanlynhakhoa.features.inventory.enums;

/**
 * Trạng thái phiếu kho (nhập, xuất): CHO_XU_LY → DA_HOAN_THANH (đã cập nhật tồn) hoặc DA_HUY (có lý do).
 */
public enum VoucherStatus {
    CHO_XU_LY("Chờ xử lý"),
    DA_HOAN_THANH("Đã hoàn thành"),
    DA_HUY("Đã hủy");

    private final String label;

    VoucherStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
