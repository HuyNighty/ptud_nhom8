package com.quanlynhakhoa.features.inventory.enums;

/** Loại hộp thuốc cấp cứu (Figma Hộp thuốc cấp cứu). */
public enum EmergencyKitType {
    CHONG_SOC("Hộp chống sốc", "Hộp chống sốc"),
    CO_BAN("Hộp cấp cứu cơ bản", "Hộp cấp cứu thường");

    private final String label;
    private final String tabLabel;

    EmergencyKitType(String label, String tabLabel) {
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
