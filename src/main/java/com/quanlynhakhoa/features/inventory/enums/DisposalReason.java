package com.quanlynhakhoa.features.inventory.enums;

/** Lý do hủy / hao hụt (Figma Hủy / hao hụt). */
public enum DisposalReason {
    QUA_HAN("Quá hạn sử dụng"),
    HONG_HOC("Hỏng hóc / đổ vỡ"),
    HAO_HUT("Hao hụt / mất mát"),
    KHAC("Khác");

    private final String label;

    DisposalReason(String label) {
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
