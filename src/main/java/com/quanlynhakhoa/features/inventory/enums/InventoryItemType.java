package com.quanlynhakhoa.features.inventory.enums;

/** Loại mặt hàng trong danh mục kho. Mỗi loại có tiền tố mã riêng. */
public enum InventoryItemType {
    THUOC("Thuốc", "TH"),
    VAT_TU("Vật tư", "VT");

    private final String label;
    private final String codePrefix;

    InventoryItemType(String label, String codePrefix) {
        this.label = label;
        this.codePrefix = codePrefix;
    }

    public String label() {
        return label;
    }

    public String codePrefix() {
        return codePrefix;
    }

    @Override
    public String toString() {
        return label;
    }
}
