package vn.n08.nhakhoa.model;

/** PhieuDieuChinhKho.loai. */
public enum LoaiDieuChinh implements Labeled {
    KIEM_KE("Kiểm kê", Tone.BLUE),
    HU_HONG("Hư hỏng", Tone.ORANGE),
    HET_HAN("Hết hạn", Tone.RED);

    private final String label;
    private final Tone tone;

    LoaiDieuChinh(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static LoaiDieuChinh of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
