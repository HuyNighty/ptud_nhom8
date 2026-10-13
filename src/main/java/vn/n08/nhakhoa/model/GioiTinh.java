package vn.n08.nhakhoa.model;

/** Giới tính. */
public enum GioiTinh implements Labeled {
    NAM("Nam", Tone.GRAY),
    NU("Nữ", Tone.GRAY),
    KHAC("Khác", Tone.GRAY);

    private final String label;
    private final Tone tone;

    GioiTinh(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static GioiTinh of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
