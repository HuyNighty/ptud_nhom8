package com.quanlynhakhoa.features.inventory.enums;

/**
 * Tình trạng hộp cấp cứu, suy ra từ thành phần hộp (không lưu). Thứ tự khai báo = mức ưu tiên hiển thị:
 * hạn dùng trước, rồi số lượng.
 */
public enum EmergencyKitStatus {
    /** Có mặt hàng đã quá hạn dùng. */
    HET_HAN("Hết hạn dùng", NoticeSeverity.CRITICAL),
    /** Có mặt hàng còn hạn không quá ngưỡng của hộp. */
    CAN_HAN("Cận hạn dùng", NoticeSeverity.CRITICAL),
    /** Thiếu so với số tối thiểu, kho còn đủ để bổ sung ngay. */
    THIEU("Thiếu mặt hàng", NoticeSeverity.WARNING),
    /** Thiếu so với số tối thiểu, kho không đủ hàng còn hạn để bổ sung — chờ nhập. */
    CHO_BO_SUNG("Chờ bổ sung", NoticeSeverity.INFO),
    /** Hộp chưa khai báo mặt hàng nào. */
    CHUA_CAU_HINH("Chưa có mặt hàng", NoticeSeverity.INFO),
    AN_TOAN("An toàn / Đầy đủ", null);

    private final String label;
    private final NoticeSeverity severity;

    EmergencyKitStatus(String label, NoticeSeverity severity) {
        this.label = label;
        this.severity = severity;
    }

    public String label() {
        return label;
    }

    /** null = an toàn. */
    public NoticeSeverity severity() {
        return severity;
    }

    public boolean needsRestock() {
        return this == THIEU || this == CHO_BO_SUNG;
    }

    public boolean expiryProblem() {
        return this == HET_HAN || this == CAN_HAN;
    }

    @Override
    public String toString() {
        return label;
    }
}
