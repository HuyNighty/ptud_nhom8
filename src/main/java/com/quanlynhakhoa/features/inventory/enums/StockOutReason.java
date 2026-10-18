package com.quanlynhakhoa.features.inventory.enums;

/** Lý do xuất kho (Figma Xuất kho). */
public enum StockOutReason {
    DIEU_TRI("Điều trị bệnh nhân", "Điều trị"),
    PHUC_VU_PHONG_KHAM("Phục vụ phòng khám", "Phục vụ PK"),
    HUY_HAO_HUT("Hủy / hao hụt", "Hủy hao hụt"),
    TRA_NCC("Trả nhà cung cấp", "Trả NCC"),
    KHAC("Khác", "Khác");

    private final String label;
    private final String shortLabel;

    StockOutReason(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String label() {
        return label;
    }

    public String shortLabel() {
        return shortLabel;
    }

    @Override
    public String toString() {
        return label;
    }
}
