package com.quanlynhakhoa.features.inventory.ui;

/** Nút thao tác nhanh ở màn Tổng quan; mỗi nút mở một màn hình trong menu kho. */
public enum InventoryQuickAction {
    NHAP_KHO("Nhập kho", InventoryScreen.STOCK_IN),
    XUAT_KHO("Xuất kho", InventoryScreen.STOCK_OUT),
    HOAN_TRA_KHO("Hoàn trả kho", InventoryScreen.RETURN),
    KIEM_KE_KHO("Kiểm kê kho", InventoryScreen.STOCKTAKE),
    HUY_HAO_HUT("Hủy / hao hụt", InventoryScreen.DISPOSAL),
    XEM_TON_KHO("Xem tồn kho", InventoryScreen.STOCK_LEVEL);

    private final String label;
    private final InventoryScreen target;

    InventoryQuickAction(String label, InventoryScreen target) {
        this.label = label;
        this.target = target;
    }

    public String label() {
        return label;
    }

    public InventoryScreen target() {
        return target;
    }
}
