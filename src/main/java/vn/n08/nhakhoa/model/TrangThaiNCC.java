package vn.n08.nhakhoa.model;

/** NhaCungCap.trangThai. */
public enum TrangThaiNCC implements Labeled {
    DANG_HOP_TAC("Đang hợp tác", Tone.GREEN),
    NGUNG_HOP_TAC("Ngừng hợp tác", Tone.GRAY);

    private final String label;
    private final Tone tone;

    TrangThaiNCC(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiNCC of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
