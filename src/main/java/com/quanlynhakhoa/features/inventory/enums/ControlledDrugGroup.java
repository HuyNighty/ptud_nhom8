package com.quanlynhakhoa.features.inventory.enums;

/** Nhóm thuốc kiểm soát đặc biệt (Figma: Gây nghiện / Hướng thần / Tiền chất). */
public enum ControlledDrugGroup {
    GAY_NGHIEN("Gây nghiện", "Thuốc gây nghiện"),
    HUONG_THAN("Hướng thần", "Thuốc hướng thần"),
    TIEN_CHAT("Tiền chất", "Tiền chất");

    private final String label;
    private final String tabLabel;

    ControlledDrugGroup(String label, String tabLabel) {
        this.label = label;
        this.tabLabel = tabLabel;
    }

    public String label() {
        return label;
    }

    public String tabLabel() {
        return tabLabel;
    }

    @Override
    public String toString() {
        return label;
    }
}
