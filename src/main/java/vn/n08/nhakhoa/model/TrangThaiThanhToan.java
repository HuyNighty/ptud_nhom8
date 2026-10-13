package vn.n08.nhakhoa.model;

/** ThanhToan.trangThai (mỗi lần thu). */
public enum TrangThaiThanhToan implements Labeled {
    THANH_CONG("Thành công", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiThanhToan(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiThanhToan of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
