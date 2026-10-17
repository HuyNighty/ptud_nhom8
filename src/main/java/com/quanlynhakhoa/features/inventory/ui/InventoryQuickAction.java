package com.quanlynhakhoa.features.inventory.ui;

/** Điều hướng từ màn hình Tổng quan kho; MainFrame (shell) quyết định mở Panel nào. */
public enum InventoryQuickAction {
    NHAP_KHO("Nhập kho"),
    XUAT_KHO("Xuất kho"),
    HOAN_TRA_KHO("Hoàn trả kho"),
    KIEM_KE_KHO("Kiểm kê kho"),
    HUY_HAO_HUT("Hủy / hao hụt"),
    XEM_TON_KHO("Xem tồn kho");

    private final String label;

    InventoryQuickAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
