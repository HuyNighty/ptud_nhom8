package vn.n08.nhakhoa.model;

/** Phương thức của từng lần thu. Không tích hợp cổng thanh toán trực tuyến. */
public enum PhuongThucThanhToan implements Labeled {
    TIEN_MAT("Tiền mặt", Tone.GREEN),
    CHUYEN_KHOAN("Chuyển khoản ngân hàng", Tone.BLUE),
    THE_POS("Thẻ (POS)", Tone.PURPLE);

    private final String label;
    private final Tone tone;

    PhuongThucThanhToan(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static PhuongThucThanhToan of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
