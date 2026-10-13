package vn.n08.nhakhoa.model;

/** HoaDon.trangThai — suy ra từ tổng tiền và các lần thu thành công. */
public enum TrangThaiHoaDon implements Labeled {
    CHUA_THANH_TOAN("Chưa thanh toán", Tone.AMBER),
    THANH_TOAN_MOT_PHAN("Thanh toán một phần", Tone.BLUE),
    DA_THANH_TOAN("Đã thanh toán", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiHoaDon(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiHoaDon of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
