package com.quanlynhakhoa.features.inventory.enums;

/** Lý do hoàn trả kho (Figma Hoàn trả kho). */
public enum ReturnReason {
    DU_THUA("Dư thừa", true),
    HANG_LOI("Lỗi", false),
    XUAT_NHAM("Xuất nhầm", true),
    KHAC("Khác", true);

    private final String label;
    private final boolean restock;

    ReturnReason(String label, boolean restock) {
        this.label = label;
        this.restock = restock;
    }

    public String label() {
        return label;
    }

    /**
     * true = khi duyệt, cộng lại vào tồn của đúng lô đã xuất.
     * Hàng lỗi chỉ được ghi nhận (đã trừ tồn lúc xuất), không quay lại tồn khả dụng để không bị xuất lần nữa.
     */
    public boolean restock() {
        return restock;
    }

    @Override
    public String toString() {
        return label;
    }
}
